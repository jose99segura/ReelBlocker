package app.reelblocker

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject
import java.time.LocalDate

/**
 * Contador persistente con historial diario. Se guarda como JSON en
 * SharedPreferences: { "yyyy-MM-dd": { "t": total, "ig": instagram, "yt": youtube }, ... }
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

    fun setOnboardingDone(ctx: Context) {
        prefs(ctx).edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    const val PKG_INSTAGRAM = "com.instagram.android"
    const val PKG_YOUTUBE = "com.google.android.youtube"

    /** Apps que el usuario puede activar o desactivar desde la UI. */
    val BLOCKABLE_APPS = listOf(
        PKG_INSTAGRAM to "Instagram",
        PKG_YOUTUBE to "YouTube"
    )

    private fun appEnabledKey(pkg: String) = "app_enabled_$pkg"

    fun isAppEnabled(ctx: Context, pkg: String): Boolean =
        prefs(ctx).getBoolean(appEnabledKey(pkg), true)

    fun setAppEnabled(ctx: Context, pkg: String, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(appEnabledKey(pkg), enabled).apply()
    }

    data class Counts(val total: Int, val instagram: Int, val youtube: Int) {
        companion object { val ZERO = Counts(0, 0, 0) }
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
        youtube = optInt("yt")
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
        }
        history.put(today, entry)

        p.edit().putString(KEY_HISTORY, history.toString()).apply()
    }

    /** Contadores del dia de hoy. */
    fun read(ctx: Context): Counts {
        val history = loadHistory(prefs(ctx))
        val today = LocalDate.now().toString()
        val entry = history.optJSONObject(today) ?: return Counts.ZERO
        return entry.toCounts()
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
