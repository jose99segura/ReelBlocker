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
    private const val KEY_LAST_SEEN_PROTECTING = "streak_last_seen_protecting"
    private const val KEY_PENDING_EVOLUTION_FROM = "streak_pending_evolution_from"
    private const val KEY_MIGRATION_V21_DONE = "migration_v21_done"

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

        // XP de perfil: hemos pasado el guard de no-op, así que se registra un
        // día protegido nuevo (como mucho una vez por día natural). Suma aunque
        // la racha se haya reiniciado a 1 — cada día activo cuenta de por vida.
        Profile.addDayXp(ctx)

        // Graduación: si acaba de alcanzar ADULT, archivar la mascota.
        // El editor anterior ya está apply'd; markPendingGraduation usa otro write.
        if (newLevel == MascotLevel.ADULT && oldLevel != MascotLevel.ADULT) {
            Collection.markPendingGraduation(ctx, Collection.currentSpecies(ctx))
        }
        Log.d(TAG, "tick: $currentCount -> $newCount (${newLevel.name})")
        return newCount
    }

    /**
     * Dev-only: fija la racha a [days] sin esperar el paso real del tiempo.
     * Actualiza last_valid_date a hoy y marca evolucion/graduacion pendientes
     * igual que hace [tick]. NO llamar desde flujo normal.
     */
    fun devSetDays(ctx: Context, days: Int) {
        val p = prefs(ctx)
        val today = today()
        val currentCount = p.getInt(KEY_COUNT, 0)
        val oldLevel = MascotLevel.forDays(currentCount)
        val newLevel = MascotLevel.forDays(days)
        val editor = p.edit()
            .putInt(KEY_COUNT, days)
            .putString(KEY_LAST_DATE, today.toString())
        val record = p.getInt(KEY_RECORD, 0)
        if (days > record) {
            editor.putInt(KEY_RECORD, days)
                .putString(KEY_RECORD_DATE, today.toString())
        }
        if (newLevel.ordinal > oldLevel.ordinal) {
            editor.putString(KEY_PENDING_EVOLUTION_FROM, oldLevel.name)
        }
        editor.apply()
        if (newLevel == MascotLevel.ADULT && oldLevel != MascotLevel.ADULT) {
            Collection.markPendingGraduation(ctx, Collection.currentSpecies(ctx))
        }
        Log.d(TAG, "devSetDays: $currentCount -> $days (${newLevel.name})")
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

    /**
     * Devuelve true si la app realmente está protegiendo al usuario:
     * servicio de accesibilidad activo Y TODAS las apps instaladas de
     * BLOCKABLE_APPS están habilitadas en los toggles.
     *
     * Modelo estricto per-app: apagar cualquier toggle (IG o YT) ya cuenta
     * como dejar de proteger → la racha se rompe.
     */
    fun shouldBeProtecting(ctx: Context): Boolean {
        if (!isAccessibilityEnabled(ctx)) return false
        val installed = Stats.BLOCKABLE_APPS.filter { (pkg, _) -> isAppInstalled(ctx, pkg) }
        if (installed.isEmpty()) return false
        return installed.all { (pkg, _) -> Stats.isAppEnabled(ctx, pkg) }
    }

    /** Último estado de protección observado, para detectar transiciones. */
    fun wasProtecting(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_LAST_SEEN_PROTECTING, false)

    fun setProtectingSeen(ctx: Context, protecting: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_LAST_SEEN_PROTECTING, protecting).apply()
    }

    /**
     * One-shot al actualizar a la versión que baja la graduación a día 21.
     * Si el usuario ya estaba en una racha ≥ 21 con la regla antigua (30),
     * marca graduación pendiente inmediata para que vea la celebración al
     * abrir la app. Idempotente: se marca un flag en prefs y no vuelve a
     * ejecutarse.
     */
    fun migrateToV21IfNeeded(ctx: Context) {
        val p = prefs(ctx)
        if (p.getBoolean(KEY_MIGRATION_V21_DONE, false)) return
        val count = p.getInt(KEY_COUNT, 0)
        if (count >= MascotLevel.ADULT.minDays && Collection.pendingGraduation(ctx) == null) {
            Collection.markPendingGraduation(ctx, Collection.currentSpecies(ctx))
            Log.d(TAG, "migrateToV21IfNeeded: count=$count → graduación inmediata")
        }
        p.edit().putBoolean(KEY_MIGRATION_V21_DONE, true).apply()
    }
}
