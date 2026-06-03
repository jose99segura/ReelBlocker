package app.reelblocker

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb

/**
 * Widget de pantalla de inicio (feature Pro). Muestra la mascota de la especie
 * activa + los días de racha. Los no-Pro ven un tile bloqueado que abre el paywall.
 *
 * RemoteViews no soporta Compose, así que la mascota se renderiza a un Bitmap con
 * [renderMascotBitmap] (reutiliza el mismo dibujo DrawScope de la app).
 */
class StreakWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = build(context)
        for (id in ids) manager.updateAppWidget(id, views)
    }

    companion object {
        const val EXTRA_OPEN_PAYWALL = "open_paywall"

        /** Refresca todas las instancias del widget (llamar tras tick/compra). */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, StreakWidget::class.java))
            if (ids.isEmpty()) return
            val views = build(context)
            for (id in ids) manager.updateAppWidget(id, views)
        }

        private fun build(context: Context): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_streak)
            if (Premium.isPro(context)) {
                views.setViewVisibility(R.id.widget_content, View.VISIBLE)
                views.setViewVisibility(R.id.widget_locked, View.GONE)

                val streak = Streak.current(context)
                val species = Collection.currentSpecies(context)
                val density = context.resources.displayMetrics.density
                val px = (132 * density).toInt().coerceIn(132, 360)
                views.setImageViewBitmap(
                    R.id.widget_mascot,
                    widgetMascotBitmap(context, species, streak.level, px)
                )
                views.setTextViewText(
                    R.id.widget_days,
                    context.resources.getQuantityString(
                        R.plurals.plural_days_count, streak.count, streak.count
                    )
                )
                views.setTextViewText(R.id.widget_label, context.getString(R.string.app_name))
                views.setTextColor(R.id.widget_label, species.accentTint.toArgb())
                views.setOnClickPendingIntent(R.id.widget_root, openApp(context, paywall = false))
            } else {
                views.setViewVisibility(R.id.widget_content, View.GONE)
                views.setViewVisibility(R.id.widget_locked, View.VISIBLE)
                views.setOnClickPendingIntent(R.id.widget_root, openApp(context, paywall = true))
            }
            return views
        }

        private fun openApp(context: Context, paywall: Boolean): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (paywall) putExtra(EXTRA_OPEN_PAYWALL, true)
            }
            return PendingIntent.getActivity(
                context,
                if (paywall) 1 else 0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun withAlpha(color: Int, a: Int): Int = (a shl 24) or (color and 0x00FFFFFF)

        /**
         * Imagen de la criatura para el widget: halo radial del color de la
         * especie + el sprite 3D de la mascota ([renderMascotBitmap]).
         */
        private fun widgetMascotBitmap(
            ctx: Context,
            species: MascotSpecies,
            level: MascotLevel,
            sizePx: Int
        ): Bitmap {
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val accent = species.accentTint.toArgb()
            val c = sizePx / 2f
            val r = sizePx * 0.48f
            val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = RadialGradient(
                    c, c, r,
                    intArrayOf(withAlpha(accent, 130), withAlpha(accent, 36), withAlpha(accent, 0)),
                    floatArrayOf(0f, 0.55f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(c, c, r, glow)

            val inner = (sizePx * 0.80f).toInt()
            val off = (sizePx - inner) / 2f
            val art = renderMascotBitmap(ctx, species, level, inner)
            canvas.drawBitmap(art, off, off, null)
            return bmp
        }
    }
}
