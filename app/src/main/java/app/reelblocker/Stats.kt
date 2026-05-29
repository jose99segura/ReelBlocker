package app.reelblocker

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.time.LocalDate

/**
 * Contador persistente con historial diario. Se guarda como JSON en
 * SharedPreferences: { "yyyy-MM-dd": { "t": total, "ig": instagram, "yt": youtube, "tt": tiktok }, ... }
 *
 * Los dias mas antiguos que MAX_HISTORY_DAYS se purgan al escribir.
 */
object Stats {
    private const val PREFS = "reelblocker_prefs"
    private const val KEY_HISTORY = "history_json"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val MAX_HISTORY_DAYS = 30L

    fun isOnboardingDone(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_ONBOARDING_DONE, false)

    fun setOnboardingDone(ctx: Context, done: Boolean = true) {
        prefs(ctx).edit().putBoolean(KEY_ONBOARDING_DONE, done).apply()
    }

    const val PKG_INSTAGRAM = "com.instagram.android"
    const val PKG_YOUTUBE = "com.google.android.youtube"
    const val PKG_FACEBOOK = "com.facebook.katana"
    const val PKG_TIKTOK = "com.zhiliaoapp.musically"

    /**
     * Segundos estimados recuperados por cada bloqueo, usado para la métrica
     * "tiempo recuperado". Si cambias este valor, actualiza también la caption
     * `stats_metric_time_recovered_caption` en strings.xml (EN + ES).
     */
    const val SECONDS_PER_BLOCK = 30L

    /** Apps que el usuario puede activar o desactivar desde la UI. */
    // Facebook NO se lista: está pausado (sin señal de detección fiable, ver
    // BlockerService) y un toggle que no hace nada confunde. La infraestructura
    // de FB (discovery, split de stats) se conserva por si se retoma.
    val BLOCKABLE_APPS = listOf(
        PKG_INSTAGRAM to "Instagram",
        PKG_YOUTUBE to "YouTube",
        PKG_TIKTOK to "TikTok"
    )

    private fun appEnabledKey(pkg: String) = "app_enabled_$pkg"

    fun isAppEnabled(ctx: Context, pkg: String): Boolean =
        prefs(ctx).getBoolean(appEnabledKey(pkg), true)

    fun setAppEnabled(ctx: Context, pkg: String, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(appEnabledKey(pkg), enabled).apply()
    }

    private const val KEY_ALLOW_IG_DM = "allow_ig_dm_reels"

    fun isDmReelsAllowed(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_ALLOW_IG_DM, true)

    fun setDmReelsAllowed(ctx: Context, allowed: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_ALLOW_IG_DM, allowed).apply()
    }

    // Dev-only: usar el PNG del huevo lila como preview en vez del Canvas.
    private const val KEY_DEV_USE_LILA_EGG = "dev_use_lila_egg"

    fun isDevLilaEggEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_DEV_USE_LILA_EGG, false)

    fun setDevLilaEggEnabled(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_DEV_USE_LILA_EGG, enabled).apply()
    }

    private const val KEY_BLOCK_IG_STORIES = "block_ig_stories"

    fun isStoriesBlocked(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_BLOCK_IG_STORIES, false)

    fun setStoriesBlocked(ctx: Context, blocked: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_BLOCK_IG_STORIES, blocked).apply()
    }

    /**
     * Valor EFECTIVO del DM bypass: requiere ser Pro Y tener el switch ON.
     * El servicio consulta este, no la pref pelada (que es lo que muestra la UI).
     */
    fun effectiveDmAllowed(ctx: Context): Boolean =
        Premium.isPro(ctx) && isDmReelsAllowed(ctx)

    /**
     * Valor EFECTIVO de bloquear stories: requiere ser Pro Y tener el switch ON.
     */
    fun effectiveStoriesBlocked(ctx: Context): Boolean =
        Premium.isPro(ctx) && isStoriesBlocked(ctx)

    data class Counts(val total: Int, val instagram: Int, val youtube: Int, val tiktok: Int = 0) {
        companion object { val ZERO = Counts(0, 0, 0, 0) }
    }

    data class DayCounts(val date: LocalDate, val counts: Counts)

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun loadHistory(p: SharedPreferences): JSONObject {
        val raw = p.getString(KEY_HISTORY, null) ?: return JSONObject()
        return try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
    }

    private fun purgeOld(history: JSONObject): JSONObject {
        val cutoff = LocalDate.now().minusDays(MAX_HISTORY_DAYS)
        val keysToRemove = mutableListOf<String>()
        val iter = history.keys()
        while (iter.hasNext()) {
            val key = iter.next()
            try {
                if (LocalDate.parse(key).isBefore(cutoff)) keysToRemove += key
            } catch (_: Exception) {
                keysToRemove += key
            }
        }
        keysToRemove.forEach { history.remove(it) }
        return history
    }

    private fun JSONObject.toCounts(): Counts = Counts(
        total = optInt("t"),
        instagram = optInt("ig"),
        youtube = optInt("yt"),
        tiktok = optInt("tt")
    )

    fun increment(ctx: Context, pkg: String) {
        val p = prefs(ctx)
        val history = purgeOld(loadHistory(p))
        val today = LocalDate.now().toString()
        val entry = history.optJSONObject(today) ?: JSONObject()

        entry.put("t", entry.optInt("t") + 1)
        when (pkg) {
            PKG_INSTAGRAM -> entry.put("ig", entry.optInt("ig") + 1)
            PKG_YOUTUBE -> entry.put("yt", entry.optInt("yt") + 1)
            PKG_FACEBOOK -> entry.put("fb", entry.optInt("fb") + 1)
            PKG_TIKTOK -> entry.put("tt", entry.optInt("tt") + 1)
        }
        history.put(today, entry)

        p.edit().putString(KEY_HISTORY, history.toString()).apply()

        // XP de perfil: cada bloqueo suma (capa de progresión permanente).
        Profile.addBlockXp(ctx)
    }

    /** Contadores del dia de hoy. */
    fun read(ctx: Context): Counts {
        val history = loadHistory(prefs(ctx))
        val today = LocalDate.now().toString()
        val entry = history.optJSONObject(today) ?: return Counts.ZERO
        return entry.toCounts()
    }

    /** Suma todos los bloqueos en la historia (ultimos MAX_HISTORY_DAYS). */
    fun totalBlocks(ctx: Context): Int {
        val history = loadHistory(prefs(ctx))
        var sum = 0
        val iter = history.keys()
        while (iter.hasNext()) {
            sum += history.optJSONObject(iter.next())?.optInt("t") ?: 0
        }
        return sum
    }

    /**
     * Devuelve los ultimos N dias incluyendo hoy, en orden cronologico
     * ascendente. Dias sin datos rellenan con ceros.
     */
    fun readLastDays(ctx: Context, days: Int): List<DayCounts> {
        val history = loadHistory(prefs(ctx))
        val today = LocalDate.now()
        return (days - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val counts = history.optJSONObject(date.toString())?.toCounts() ?: Counts.ZERO
            DayCounts(date, counts)
        }
    }
}
