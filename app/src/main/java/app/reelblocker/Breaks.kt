package app.reelblocker

import android.content.Context
import android.util.Log
import java.time.LocalDate
import java.time.ZoneId

/**
 * Descansos (breaks) — feature Pro. Permite pausar el bloqueo de Reels/Shorts
 * durante 10 minutos sin romper la racha. Uno por día calendario.
 *
 * Persistencia en el mismo SharedPreferences que [Stats] y [Streak].
 */
object Breaks {
    private const val TAG = "ReelBlocker.Breaks"
    private const val PREFS = "reelblocker_prefs"

    private const val KEY_BREAK_END_MS = "break_end_ms"
    private const val KEY_BREAK_CONSUMED_DATE = "break_consumed_date"

    /** Duración del descanso. 10 minutos. */
    const val BREAK_DURATION_MS: Long = 10L * 60_000

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun today(): String =
        LocalDate.now(ZoneId.systemDefault()).toString()

    /** ¿El usuario tiene un descanso disponible hoy? (no consumido). */
    fun isAvailableToday(ctx: Context): Boolean {
        val consumed = prefs(ctx).getString(KEY_BREAK_CONSUMED_DATE, null)
        return consumed != today()
    }

    /** ¿Hay un descanso activo ahora mismo? */
    fun isOnBreak(ctx: Context): Boolean {
        val endMs = prefs(ctx).getLong(KEY_BREAK_END_MS, 0L)
        return System.currentTimeMillis() < endMs
    }

    /** Millis restantes del descanso, o null si no hay descanso en curso. */
    fun millisRemaining(ctx: Context): Long? {
        val endMs = prefs(ctx).getLong(KEY_BREAK_END_MS, 0L)
        val remaining = endMs - System.currentTimeMillis()
        return if (remaining > 0L) remaining else null
    }

    /**
     * Arranca un descanso de 10 min. Marca el día como consumido.
     * Devuelve true si se inició, false si ya estaba consumido hoy.
     */
    fun start(ctx: Context): Boolean {
        if (!isAvailableToday(ctx)) {
            Log.d(TAG, "start: ya consumido hoy, ignorando")
            return false
        }
        val endMs = System.currentTimeMillis() + BREAK_DURATION_MS
        prefs(ctx).edit()
            .putLong(KEY_BREAK_END_MS, endMs)
            .putString(KEY_BREAK_CONSUMED_DATE, today())
            .apply()
        Log.d(TAG, "start: break started, ends at $endMs")
        return true
    }

    /** Termina manualmente el descanso. No devuelve el consumo del día. */
    fun endEarly(ctx: Context) {
        prefs(ctx).edit().putLong(KEY_BREAK_END_MS, 0L).apply()
        Log.d(TAG, "endEarly: break ended manually")
    }
}
