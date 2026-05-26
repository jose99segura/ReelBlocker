package app.reelblocker

import android.content.Context
import android.util.Log
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Sistema de racha diaria. Cuenta dias consecutivos con el servicio activo y
 * persiste el estado en el mismo SharedPreferences que [Stats].
 *
 * Reglas:
 * - Si tick() se llama el mismo dia que la ultima actualizacion, no-op.
 * - Si se llama al dia siguiente, racha +1.
 * - Si se llama tras 2+ dias, racha se reinicia a 1 (sin dia de gracia en v1).
 * - Si el servicio se desactiva (detectado externamente o via UI), racha = 0.
 *
 * El flag de evolucion pendiente se activa cuando el nuevo [MascotLevel]
 * es diferente del anterior; la UI lo lee y dispara la celebracion.
 */
object Streak {
    private const val TAG = "ReelBlocker.Streak"
    private const val PREFS = "reelblocker_prefs"

    private const val KEY_COUNT = "streak_count"
    private const val KEY_LAST_DATE = "streak_last_valid_date"
    private const val KEY_RECORD = "streak_record"
    private const val KEY_RECORD_DATE = "streak_record_date"
    private const val KEY_LAST_SEEN_ENABLED = "streak_last_seen_enabled"
    private const val KEY_LAST_SEEN_PROTECTING = "streak_last_seen_protecting"
    private const val KEY_PENDING_EVOLUTION_FROM = "streak_pending_evolution_from"

    data class State(
        val count: Int,
        val record: Int,
        val recordDate: String?,
        val level: MascotLevel,
        val daysToNextLevel: Int?,
        val progressInLevel: Float
    )

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())

    /**
     * Suma un dia si corresponde. Devuelve el contador resultante.
     * Marca evolucion pendiente si el nivel cambia.
     */
    fun tick(ctx: Context): Int {
        val p = prefs(ctx)
        val today = today()
        val lastDateStr = p.getString(KEY_LAST_DATE, null)
        val currentCount = p.getInt(KEY_COUNT, 0)

        val newCount = if (lastDateStr == null) {
            1
        } else {
            val lastDate = try { LocalDate.parse(lastDateStr) } catch (_: Exception) { null }
            if (lastDate == null) {
                1
            } else {
                val daysBetween = ChronoUnit.DAYS.between(lastDate, today)
                when {
                    daysBetween == 0L -> currentCount  // ya contado hoy
                    daysBetween == 1L -> currentCount + 1
                    daysBetween < 0L -> currentCount  // reloj atras, no hacer nada raro
                    else -> 1  // se perdio al menos un dia: reiniciar
                }
            }
        }

        if (newCount == currentCount && p.getString(KEY_LAST_DATE, null) == today.toString()) {
            return currentCount  // no-op real
        }

        val oldLevel = MascotLevel.forDays(currentCount)
        val newLevel = MascotLevel.forDays(newCount)

        val editor = p.edit()
            .putInt(KEY_COUNT, newCount)
            .putString(KEY_LAST_DATE, today.toString())

        // Record historico.
        val record = p.getInt(KEY_RECORD, 0)
        if (newCount > record) {
            editor.putInt(KEY_RECORD, newCount)
            editor.putString(KEY_RECORD_DATE, today.toString())
        }

        // Marcar evolucion pendiente si subio de nivel.
        if (newLevel.ordinal > oldLevel.ordinal) {
            editor.putString(KEY_PENDING_EVOLUTION_FROM, oldLevel.name)
            Log.d(TAG, "tick: evolucion ${oldLevel.name} -> ${newLevel.name} (dia $newCount)")
        }

        editor.apply()

        // Graduación: si acaba de alcanzar ADULT, archivar la mascota.
        // El editor anterior ya está apply'd; markPendingGraduation usa otro write.
        if (newLevel == MascotLevel.ADULT && oldLevel != MascotLevel.ADULT) {
            Collection.markPendingGraduation(ctx, Collection.currentSpecies(ctx))
        }
        Log.d(TAG, "tick: $currentCount -> $newCount (${newLevel.name})")
        return newCount
    }

    /** Rompe la racha (a 0). Conserva el record. */
    fun breakStreak(ctx: Context, reason: String) {
        val p = prefs(ctx)
        val previous = p.getInt(KEY_COUNT, 0)
        if (previous == 0) return  // nada que romper
        p.edit()
            .putInt(KEY_COUNT, 0)
            .remove(KEY_LAST_DATE)
            .remove(KEY_PENDING_EVOLUTION_FROM)
            .apply()
        Log.d(TAG, "breakStreak: $previous -> 0 (motivo: $reason)")
    }

    fun current(ctx: Context): State {
        val p = prefs(ctx)
        val count = p.getInt(KEY_COUNT, 0)
        val record = p.getInt(KEY_RECORD, 0)
        val recordDate = p.getString(KEY_RECORD_DATE, null)
        val level = MascotLevel.forDays(count)
        return State(
            count = count,
            record = record,
            recordDate = recordDate,
            level = level,
            daysToNextLevel = MascotLevel.daysToNext(count),
            progressInLevel = MascotLevel.progressInLevel(count)
        )
    }

    /** Lee y limpia el flag de evolucion pendiente. */
    fun consumePendingEvolution(ctx: Context): MascotLevel? {
        val p = prefs(ctx)
        val fromName = p.getString(KEY_PENDING_EVOLUTION_FROM, null) ?: return null
        p.edit().remove(KEY_PENDING_EVOLUTION_FROM).apply()
        return try { MascotLevel.valueOf(fromName) } catch (_: Exception) { null }
    }

    /** Lee el ultimo estado conocido del servicio (para detectar transiciones). */
    fun wasServiceEnabled(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_LAST_SEEN_ENABLED, false)

    fun setServiceEnabledSeen(ctx: Context, enabled: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_LAST_SEEN_ENABLED, enabled).apply()
    }

    /**
     * Devuelve true si la app realmente está protegiendo al usuario:
     * servicio de accesibilidad activo Y al menos una app instalada de
     * BLOCKABLE_APPS está habilitada en los toggles del usuario.
     *
     * Modelo estricto: si el usuario apaga todos los toggles (o el único
     * instalado), no está protegiendo nada → racha se rompe.
     */
    fun shouldBeProtecting(ctx: Context): Boolean {
        if (!isAccessibilityEnabled(ctx)) return false
        return Stats.BLOCKABLE_APPS.any { (pkg, _) ->
            isAppInstalled(ctx, pkg) && Stats.isAppEnabled(ctx, pkg)
        }
    }

    /** Último estado de protección observado, para detectar transiciones. */
    fun wasProtecting(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_LAST_SEEN_PROTECTING, false)

    fun setProtectingSeen(ctx: Context, protecting: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_LAST_SEEN_PROTECTING, protecting).apply()
    }
}
