package app.reelblocker

import android.content.Context
import android.content.SharedPreferences
import java.time.LocalDate

/**
 * Contador persistente de bloqueos del DIA DE HOY. Si la fecha guardada cambia,
 * los contadores se ponen a cero automaticamente al leer o escribir.
 */
object Stats {
    private const val PREFS = "reelblocker_prefs"
    private const val KEY_DATE = "count_date"
    private const val KEY_TOTAL = "count_total"
    private const val KEY_INSTAGRAM = "count_instagram"
    private const val KEY_YOUTUBE = "count_youtube"

    const val PKG_INSTAGRAM = "com.instagram.android"
    const val PKG_YOUTUBE = "com.google.android.youtube"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun today(): String = LocalDate.now().toString()

    /**
     * Si el dia guardado no es hoy, pone los contadores a cero y actualiza
     * la fecha. Aplica los cambios sincronicamente para que read/increment
     * inmediatos lean valores ya rolados.
     */
    private fun rollIfNewDay(p: SharedPreferences) {
        val today = today()
        if (p.getString(KEY_DATE, null) != today) {
            p.edit()
                .putString(KEY_DATE, today)
                .putInt(KEY_TOTAL, 0)
                .putInt(KEY_INSTAGRAM, 0)
                .putInt(KEY_YOUTUBE, 0)
                .apply()
        }
    }

    fun increment(ctx: Context, pkg: String) {
        val p = prefs(ctx)
        rollIfNewDay(p)
        val perAppKey = when (pkg) {
            PKG_INSTAGRAM -> KEY_INSTAGRAM
            PKG_YOUTUBE -> KEY_YOUTUBE
            else -> null
        }
        val edit = p.edit()
        edit.putInt(KEY_TOTAL, p.getInt(KEY_TOTAL, 0) + 1)
        if (perAppKey != null) {
            edit.putInt(perAppKey, p.getInt(perAppKey, 0) + 1)
        }
        edit.apply()
    }

    data class Counts(val total: Int, val instagram: Int, val youtube: Int)

    fun read(ctx: Context): Counts {
        val p = prefs(ctx)
        rollIfNewDay(p)
        return Counts(
            total = p.getInt(KEY_TOTAL, 0),
            instagram = p.getInt(KEY_INSTAGRAM, 0),
            youtube = p.getInt(KEY_YOUTUBE, 0)
        )
    }
}
