package app.reelblocker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Auto-diagnóstico: avisa al usuario cuando la protección deja de funcionar,
 * que de otro modo sería un fallo silencioso (los Reels vuelven a aparecer y
 * el usuario solo lo nota — si lo nota — semanas después → reseña de 1★).
 *
 * Dos señales:
 *
 *  1. Protección caída: el servicio de accesibilidad se desvincula
 *     ([BlockerService.onUnbind]). Cubre el caso de que el sistema lo mate o
 *     el usuario lo apague. Se suprime si el usuario fue hace poco a Ajustes de
 *     accesibilidad desde la app (desactivación deliberada).
 *
 *  2. Detección probablemente rota (heurística conservadora): el usuario SIGUE
 *     usando IG/YT (el servicio recibe sus eventos) y ANTES bloqueábamos, pero
 *     llevamos >7 días sin un solo bloqueo. Señal fuerte de que los hints se
 *     rompieron. No se dispara para quien simplemente dejó de ver Reels (ese
 *     nunca tuvo `everBlocked` reciente con actividad actual).
 *
 * Toda la decisión vive en funciones puras ([shouldWarnBreakage]) para poder
 * testearla en JVM sin Context.
 */
object HealthCheck {
    private const val TAG = "ReelBlocker.Service"
    private const val PREFS = "reelblocker_prefs"

    const val CHANNEL_ID = "protection_alerts"
    private const val NOTIF_PROTECTION_OFF = 1001
    private const val NOTIF_BREAKAGE = 1002

    private const val KEY_LAST_BLOCK_MS = "health_last_block_ms"
    private const val KEY_EVER_BLOCKED = "health_ever_blocked"
    private const val KEY_BREAKAGE_NOTIFIED_MS = "health_breakage_notified_ms"
    private const val KEY_SUPPRESS_OFF_UNTIL_MS = "health_suppress_off_until_ms"

    /** Sin bloqueos durante esta ventana (con actividad) ⇒ probable rotura. */
    const val BREAKAGE_WINDOW_MS = 7L * 24 * 60 * 60 * 1000
    /** No repetir el aviso de rotura más de una vez por ventana. */
    const val BREAKAGE_RENOTIFY_MS = 7L * 24 * 60 * 60 * 1000
    /** Ventana de gracia tras ir a Ajustes de accesibilidad (desactiv. deliberada). */
    private const val SUPPRESS_OFF_MS = 5L * 60 * 1000

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- Señales que escribe el servicio ----

    /** Llamar al confirmar un bloqueo (back disparado con éxito). */
    fun recordBlock(ctx: Context) {
        prefs(ctx).edit()
            .putLong(KEY_LAST_BLOCK_MS, System.currentTimeMillis())
            .putBoolean(KEY_EVER_BLOCKED, true)
            .apply()
    }

    /**
     * Llamar cuando MainActivity manda al usuario a Ajustes de accesibilidad,
     * para no avisar de "protección desactivada" ante una acción deliberada.
     */
    fun suppressProtectionOffNotice(ctx: Context) {
        prefs(ctx).edit()
            .putLong(KEY_SUPPRESS_OFF_UNTIL_MS, System.currentTimeMillis() + SUPPRESS_OFF_MS)
            .apply()
    }

    // ---- Decisión pura (testeable) ----

    fun shouldWarnBreakage(
        everBlocked: Boolean,
        lastBlockMs: Long,
        lastNotifiedMs: Long,
        nowMs: Long
    ): Boolean =
        everBlocked &&
            lastBlockMs > 0 &&
            nowMs - lastBlockMs >= BREAKAGE_WINDOW_MS &&
            nowMs - lastNotifiedMs >= BREAKAGE_RENOTIFY_MS

    // ---- Disparadores ----

    /**
     * Llamar desde el servicio cuando ve actividad de IG/YT (el caller debe
     * throttlear). Si llevamos demasiado sin bloquear pese a la actividad,
     * avisa de probable rotura.
     */
    fun maybeWarnBreakage(ctx: Context) {
        val p = prefs(ctx)
        val now = System.currentTimeMillis()
        val should = shouldWarnBreakage(
            everBlocked = p.getBoolean(KEY_EVER_BLOCKED, false),
            lastBlockMs = p.getLong(KEY_LAST_BLOCK_MS, 0L),
            lastNotifiedMs = p.getLong(KEY_BREAKAGE_NOTIFIED_MS, 0L),
            nowMs = now
        )
        if (!should) return
        p.edit().putLong(KEY_BREAKAGE_NOTIFIED_MS, now).apply()
        notify(
            ctx,
            NOTIF_BREAKAGE,
            ctx.getString(R.string.health_breakage_title),
            ctx.getString(R.string.health_breakage_body)
        )
    }

    /** Llamar desde [BlockerService.onUnbind]: la protección acaba de caer. */
    fun notifyProtectionOff(ctx: Context) {
        val until = prefs(ctx).getLong(KEY_SUPPRESS_OFF_UNTIL_MS, 0L)
        if (System.currentTimeMillis() < until) {
            Log.d(TAG, "Protección caída: aviso suprimido (desactivación deliberada)")
            return
        }
        notify(
            ctx,
            NOTIF_PROTECTION_OFF,
            ctx.getString(R.string.health_protection_off_title),
            ctx.getString(R.string.health_protection_off_body)
        )
    }

    // ---- Infra de notificaciones ----

    private fun notify(ctx: Context, id: Int, title: String, body: String) {
        if (!canPost(ctx)) {
            Log.d(TAG, "Notificación omitida: sin permiso POST_NOTIFICATIONS")
            return
        }
        ensureChannel(ctx)

        // Tap → abrir la app para que el usuario revise/reactive.
        val launch = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
        val pending = launch?.let {
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            android.app.PendingIntent.getActivity(
                ctx, id, it,
                android.app.PendingIntent.FLAG_IMMUTABLE or
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .apply { pending?.let { setContentIntent(it) } }
            .build()

        try {
            NotificationManagerCompat.from(ctx).notify(id, notif)
        } catch (e: SecurityException) {
            Log.d(TAG, "notify() lanzó SecurityException: ${e.message}")
        }
    }

    private fun ensureChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (mgr.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            ctx.getString(R.string.health_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = ctx.getString(R.string.health_channel_desc)
        }
        mgr.createNotificationChannel(channel)
    }

    private fun canPost(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            ctx, android.Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
