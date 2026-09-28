package app.reelblocker

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes

/**
 * Nivel de perfil basado en XP acumulado de POR VIDA. A diferencia de la racha
 * ([Streak]) o la colección ([Collection]), este contador NUNCA se resetea: ni
 * al romper la racha, ni al graduarse, ni al desinstalar (se respalda con el
 * resto de prefs). Es la única capa de progresión permanente de la app.
 *
 * Tres fuentes de XP, cada una enganchada donde el evento ya ocurre una sola vez:
 *  - cada bloqueo detectado     → [addBlockXp]      (desde [Stats.increment])
 *  - cada día protegido nuevo    → [addDayXp]        (desde [Streak.tick])
 *  - cada graduación             → [addGraduationXp] (desde [Collection.consumePendingGraduation])
 *
 * Comparte el mismo SharedPreferences que [Stats], [Streak] y [Collection].
 */
object Profile {
    private const val TAG = "ReelBlocker.Profile"
    private const val PREFS = "reelblocker_prefs"

    private const val KEY_XP = "profile_xp"
    private const val KEY_SEEDED = "profile_seeded"
    /** Último nivel que el usuario ya vio celebrado (ver [pendingLevelUp]). */
    private const val KEY_LAST_SEEN_LEVEL = "profile_last_seen_level"

    // XP por evento. Ajustables: un día protegido vale bastante más que un
    // bloqueo suelto, y una graduación es el hito gordo.
    const val XP_PER_BLOCK = 2
    const val XP_PER_DAY = 15
    const val XP_PER_GRADUATION = 200
    /** Extra por cada ★ adicional de la especie graduada (re-graduaciones). */
    const val XP_PER_GRADUATION_STAR = 50

    /**
     * Solo los primeros N bloqueos de cada día dan XP. Sin tope, entrar a
     * Reels a propósito "farmearía" nivel — justo lo contrario de lo que la app
     * quiere premiar. Así el nivel mide sobre todo días protegidos.
     */
    const val MAX_XP_BLOCKS_PER_DAY = 10

    /** Cada cuántos niveles se sube de rango (hito con celebración grande). */
    const val LEVELS_PER_RANK = 5

    /**
     * Rango = título por tramo de [LEVELS_PER_RANK] niveles. Solo estatus: nunca
     * desbloquea especies ni features, para no tocar el reparto free/Pro.
     */
    enum class Rank(@StringRes val titleRes: Int) {
        ROOKIE(R.string.profile_rank_rookie),         // Nv. 1–4
        APPRENTICE(R.string.profile_rank_apprentice), // Nv. 5–9
        GUARDIAN(R.string.profile_rank_guardian),     // Nv. 10–14
        VETERAN(R.string.profile_rank_veteran),       // Nv. 15–19
        MASTER(R.string.profile_rank_master),         // Nv. 20–24
        SAGE(R.string.profile_rank_sage),             // Nv. 25–29
        LEGEND(R.string.profile_rank_legend);         // Nv. 30+

        /** Nivel en el que se alcanza este rango. */
        val minLevel: Int get() = if (ordinal == 0) 1 else ordinal * LEVELS_PER_RANK

        val next: Rank? get() = entries.getOrNull(ordinal + 1)
    }

    data class State(
        val level: Int,
        val xp: Int,
        /** XP acumulado dentro del nivel actual (0..xpForNextLevel). */
        val xpIntoLevel: Int,
        /** XP que cuesta el tramo del nivel actual al siguiente. */
        val xpForNextLevel: Int,
        /** Progreso 0..1 dentro del nivel actual. */
        val progress: Float
    )

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- Curva (funciones puras, sin Context, testeables) -------------------

    /**
     * XP acumulado total necesario para ALCANZAR el nivel [n] (1-indexado).
     * Curva cuadrática suave: L1=0, L2=100, L3=300, L4=600, L5=1000, L6=1500…
     */
    fun xpToReachLevel(n: Int): Int = 50 * (n - 1) * n

    /** Nivel (≥1) correspondiente a un total de [xp]. Sin tope. */
    fun levelForXp(xp: Int): Int {
        var level = 1
        while (xpToReachLevel(level + 1) <= xp) level++
        return level
    }

    fun rankForLevel(level: Int): Rank =
        Rank.entries[(level / LEVELS_PER_RANK).coerceIn(0, Rank.entries.size - 1)]

    /** XP de una graduación: más cuanto más veces se ha graduado esa especie. */
    fun graduationXp(starsAfter: Int): Int =
        XP_PER_GRADUATION + XP_PER_GRADUATION_STAR * (starsAfter.coerceIn(1, Collection.MAX_STARS) - 1)

    /** ¿El bloqueo nº [blocksToday] del día (1-indexado) todavía da XP? */
    fun blockEarnsXp(blocksToday: Int): Boolean = blocksToday in 1..MAX_XP_BLOCKS_PER_DAY

    /** Deriva el [State] completo a partir de un total de XP. Pura. */
    fun stateForXp(xp: Int): State {
        val level = levelForXp(xp)
        val floor = xpToReachLevel(level)
        val ceil = xpToReachLevel(level + 1)
        val span = (ceil - floor).coerceAtLeast(1)
        val into = (xp - floor).coerceIn(0, span)
        return State(
            level = level,
            xp = xp,
            xpIntoLevel = into,
            xpForNextLevel = span,
            progress = into.toFloat() / span.toFloat()
        )
    }

    // ---- Lectura ------------------------------------------------------------

    fun current(ctx: Context): State {
        seedIfNeeded(ctx)
        return stateForXp(prefs(ctx).getInt(KEY_XP, 0))
    }

    // ---- Escritura ----------------------------------------------------------

    private fun addXp(ctx: Context, amount: Int) {
        if (amount <= 0) return
        seedIfNeeded(ctx)
        val p = prefs(ctx)
        val current = p.getInt(KEY_XP, 0)
        p.edit().putInt(KEY_XP, current + amount).apply()
    }

    /** [blocksToday] = bloqueos de hoy YA incluyendo este. */
    fun addBlockXp(ctx: Context, blocksToday: Int) {
        if (blockEarnsXp(blocksToday)) addXp(ctx, XP_PER_BLOCK)
    }
    fun addDayXp(ctx: Context) = addXp(ctx, XP_PER_DAY)
    /** [starsAfter] = veces que la especie se ha graduado, incluida esta. */
    fun addGraduationXp(ctx: Context, starsAfter: Int) = addXp(ctx, graduationXp(starsAfter))

    // ---- Subidas de nivel ---------------------------------------------------

    /** Una subida pendiente de celebrar: de [fromLevel] a [toLevel]. */
    data class LevelUp(val fromLevel: Int, val toLevel: Int) {
        /** Se cruzó un hito de rango (múltiplo de [LEVELS_PER_RANK]). */
        val isRankUp: Boolean get() = rankForLevel(toLevel) != rankForLevel(fromLevel)
        val rank: Rank get() = rankForLevel(toLevel)
    }

    /**
     * Subida de nivel aún no celebrada, o null. El XP se suma desde varios
     * sitios (service, worker, tick), así que en vez de disparar eventos se
     * compara el nivel actual con el último visto al volver a la app. Varios
     * niveles de golpe se celebran como uno solo.
     */
    fun pendingLevelUp(ctx: Context): LevelUp? {
        val p = prefs(ctx)
        val level = current(ctx).level
        if (!p.contains(KEY_LAST_SEEN_LEVEL)) {
            // Primera vez con esta feature: el nivel existente no es "nuevo".
            p.edit().putInt(KEY_LAST_SEEN_LEVEL, level).apply()
            return null
        }
        val seen = p.getInt(KEY_LAST_SEEN_LEVEL, level)
        return if (level > seen) LevelUp(seen, level) else null
    }

    fun markLevelSeen(ctx: Context, level: Int) {
        prefs(ctx).edit().putInt(KEY_LAST_SEEN_LEVEL, level).apply()
    }

    /**
     * One-shot para usuarios que ya tenían progreso antes de existir el sistema
     * de niveles: siembra un XP aproximado a partir de lo que aún se puede leer
     * (récord de racha, bloqueos de los últimos 30 días, graduaciones). El
     * historial real >30 días ya se purgó, así que es deliberadamente aproximado:
     * solo evita que un usuario fiel arranque en Nv.1.
     *
     * Se ejecuta en background para no bloquear el hilo principal la primera vez.
     */
    fun seedIfNeeded(ctx: Context) {
        val p = prefs(ctx)
        if (p.getBoolean(KEY_SEEDED, false)) return
        // Marcar sembrado YA para que llamadas concurrentes (current(), addXp…)
        // no lancen hilos duplicados mientras este todavía se ejecuta.
        p.edit().putBoolean(KEY_SEEDED, true).apply()
        val appCtx = ctx.applicationContext
        Thread {
            val seed = Streak.current(appCtx).record * XP_PER_DAY +
                Stats.totalBlocks(appCtx) * XP_PER_BLOCK +
                Collection.read(appCtx).size * XP_PER_GRADUATION
            // El nivel sembrado no es una subida: marcarlo ya como visto para
            // que un usuario fiel no reciba 8 niveles de celebración de golpe.
            prefs(appCtx).edit()
                .putInt(KEY_XP, seed)
                .putInt(KEY_LAST_SEEN_LEVEL, levelForXp(seed))
                .apply()
            Log.d(TAG, "seedIfNeeded: XP inicial sembrado = $seed (nivel ${levelForXp(seed)})")
        }.apply { isDaemon = true }.start()
    }
}
