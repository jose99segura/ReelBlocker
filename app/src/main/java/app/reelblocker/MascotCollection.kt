package app.reelblocker

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId

/**
 * Inventario de mascotas graduadas. Al alcanzar [MascotLevel.ADULT] la mascota
 * actual se archiva aquí y un huevo nuevo emerge con una especie distinta
 * (aleatoria entre las aún no coleccionadas; cuando están las 5, aleatoria del
 * pool completo).
 *
 * Comparte el mismo SharedPreferences que [Stats] y [Streak].
 */
object Collection {
    private const val TAG = "ReelBlocker.Collection"
    private const val PREFS = "reelblocker_prefs"

    private const val KEY_COLLECTION_JSON = "collection_json"
    private const val KEY_CURRENT_SPECIES = "current_species"
    private const val KEY_PENDING_GRADUATION = "pending_graduation_from"
    private const val KEY_PENDING_PRO_UNLOCK = "pending_pro_unlock"

    data class CollectedMascot(
        val species: MascotSpecies,
        val acquiredDate: String,   // ISO LocalDate
        val daysToReach: Int,
        // Stats del periodo, congeladas al graduarse para que el re-compartir
        // sea fiel aunque el historial rodante de 30 días ya no las contenga.
        // 0 en entradas archivadas antes de esta versión (fallback en la UI).
        val secondsSaved: Long = 0,
        val streakRecord: Int = 0
    )

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun today(): String =
        LocalDate.now(ZoneId.systemDefault()).toString()

    /** Especie activa (la del huevo actualmente en juego). */
    fun currentSpecies(ctx: Context): MascotSpecies {
        val id = prefs(ctx).getString(KEY_CURRENT_SPECIES, null)
        return MascotSpecies.fromIdOrNull(id) ?: MascotSpecies.DEFAULT
    }

    /** Lista completa archivada, en orden de adquisición. */
    fun read(ctx: Context): List<CollectedMascot> {
        val raw = prefs(ctx).getString(KEY_COLLECTION_JSON, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val species = MascotSpecies.fromIdOrNull(obj.optString("species")) ?: continue
                    add(
                        CollectedMascot(
                            species = species,
                            acquiredDate = obj.optString("date"),
                            daysToReach = obj.optInt("days", 21),
                            secondsSaved = obj.optLong("secs", 0),
                            streakRecord = obj.optInt("record", 0)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "read: JSON corrupto, devolviendo lista vacía", e)
            emptyList()
        }
    }

    /** ¿Hay una graduación pendiente de consumir por la UI? Devuelve la especie graduada. */
    fun pendingGraduation(ctx: Context): MascotSpecies? {
        val id = prefs(ctx).getString(KEY_PENDING_GRADUATION, null) ?: return null
        return MascotSpecies.fromIdOrNull(id)
    }

    /**
     * Marca una graduación pendiente. Lo llama [Streak.tick] cuando detecta
     * la transición a ADULT. NO archiva todavía — la UI consumirá el flag.
     */
    fun markPendingGraduation(ctx: Context, species: MascotSpecies) {
        prefs(ctx).edit().putString(KEY_PENDING_GRADUATION, species.id).apply()
        Log.d(TAG, "markPendingGraduation: ${species.id}")
    }

    /**
     * Archiva la mascota pendiente, rompe la racha, escoge la siguiente
     * especie y devuelve la especie recién archivada. Es una no-op si no
     * hay graduación pendiente.
     */
    fun consumePendingGraduation(ctx: Context, daysReached: Int): MascotSpecies? {
        val species = pendingGraduation(ctx) ?: return null

        // 1) Releer la colección y añadir la mascota graduada EN MEMORIA (permite
        //    duplicados; orden de adquisición).
        val arr = try {
            val raw = prefs(ctx).getString(KEY_COLLECTION_JSON, null)
            if (raw == null) JSONArray() else JSONArray(raw)
        } catch (_: Exception) {
            JSONArray()
        }
        // Congelar las stats del periodo ANTES de romper la racha (breakStreak
        // preserva el récord, pero lo leemos aquí por claridad). El tiempo se
        // calcula sobre los últimos `daysReached` días del historial.
        val secsSaved = Stats.readLastDays(ctx, daysReached)
            .sumOf { it.counts.total } * Stats.SECONDS_PER_BLOCK
        val record = Streak.current(ctx).record
        arr.put(JSONObject().apply {
            put("species", species.id)
            put("date", today())
            put("days", daysReached)
            put("secs", secsSaved)
            put("record", record)
        })

        // 2) Elegir la siguiente especie con la colección YA actualizada (incluye
        //    la recién archivada). Selección por tier: free solo entre freeSpecies;
        //    al agotar las free uncollected se marca el pro-unlock para el paywall.
        val collected = buildSet {
            for (i in 0 until arr.length()) {
                MascotSpecies.fromIdOrNull(arr.getJSONObject(i).optString("species"))?.let { add(it) }
            }
        }
        val selection = selectNextSpecies(collected, justArchived = species, isPro = Premium.isPro(ctx))

        // Romper la racha antes de la escritura final. Es idempotente (no-op si ya
        // está a 0), así que reintentarlo tras un crash es seguro.
        Streak.breakStreak(ctx, reason = "graduation")

        // 3) Escritura ÚNICA y atómica: colección + especie siguiente + limpiar el
        //    flag pendiente (+ pro-unlock). Si el proceso muere ANTES, el flag sigue
        //    puesto y se reintenta limpio; si muere DESPUÉS, ya está todo hecho. La
        //    mascota nunca se archiva dos veces.
        val editor = prefs(ctx).edit()
            .putString(KEY_COLLECTION_JSON, arr.toString())
            .putString(KEY_CURRENT_SPECIES, selection.next.id)
            .remove(KEY_PENDING_GRADUATION)
        if (selection.markPendingProUnlock) {
            editor.putBoolean(KEY_PENDING_PRO_UNLOCK, true)
            Log.d(TAG, "consumePendingGraduation: free tier agotado → pending_pro_unlock=true")
        }
        editor.apply()

        // XP de perfil: la graduación es el hito gordo de progresión permanente.
        Profile.addGraduationXp(ctx)
        Log.d(TAG, "consumePendingGraduation: archivada=${species.id} próxima=${selection.next.id}")
        return species
    }

    /**
     * Lógica pura de selección, extraída para tests JVM. No toca prefs ni
     * Context.
     */
    internal data class SelectResult(
        val next: MascotSpecies,
        val markPendingProUnlock: Boolean
    )

    internal fun selectNextSpecies(
        collected: Set<MascotSpecies>,
        justArchived: MascotSpecies,
        isPro: Boolean,
        random: kotlin.random.Random = kotlin.random.Random.Default
    ): SelectResult {
        val pool = if (isPro) MascotSpecies.entries.toList() else MascotSpecies.freeSpecies()
        val uncollected = pool.filter { it !in collected && it != justArchived }
        if (uncollected.isNotEmpty()) {
            return SelectResult(uncollected.random(random), markPendingProUnlock = false)
        }
        val mark = !isPro
        val fallback = pool.filter { it != justArchived }
        val next = when {
            fallback.isNotEmpty() -> fallback.random(random)
            pool.isNotEmpty() -> pool.random(random)
            else -> justArchived
        }
        return SelectResult(next, markPendingProUnlock = mark)
    }

    /** ¿Hay un paywall pendiente por agotar las especies free? */
    fun pendingProUnlock(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_PENDING_PRO_UNLOCK, false)

    /** Marca el paywall pendiente como mostrado. Llamar tras abrir la paywall. */
    fun consumeProUnlock(ctx: Context) {
        prefs(ctx).edit().remove(KEY_PENDING_PRO_UNLOCK).apply()
    }

    /** Cuenta de especies únicas coleccionadas. */
    fun uniqueCount(ctx: Context): Int =
        read(ctx).map { it.species }.toSet().size

    /** Dev-only: vacia el Pokedex y reinicia la especie activa a DEFAULT. */
    fun devClear(ctx: Context) {
        prefs(ctx).edit()
            .remove(KEY_COLLECTION_JSON)
            .remove(KEY_CURRENT_SPECIES)
            .remove(KEY_PENDING_GRADUATION)
            .remove(KEY_PENDING_PRO_UNLOCK)
            .apply()
        Log.d(TAG, "devClear: inventario vaciado")
    }
}
