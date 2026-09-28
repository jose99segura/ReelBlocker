package app.reelblocker

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

/**
 * Aviso discreto al bloquear: una píldora pequeña arriba con la mascota activa
 * y el contador de hoy ("¡Basta! · 14 hoy"). Sustituye al Toast genérico para
 * que el momento del bloqueo se asocie a la mascota, sin estorbar: no es
 * tocable ni enfocable (los toques pasan a la app de debajo) y se desvanece
 * sola en [VISIBLE_MS].
 *
 * Se dibuja como TYPE_ACCESSIBILITY_OVERLAY, que el servicio de accesibilidad
 * puede añadir sin el permiso SYSTEM_ALERT_WINDOW. Si el sistema lo rechaza,
 * cae al Toast de siempre.
 */
class BlockBubble(private val service: AccessibilityService) {

    private companion object {
        const val TAG = "ReelBlocker.Bubble"
        const val VISIBLE_MS = 1800L
        const val FADE_MS = 180L
        const val ICON_DP = 28
        val BACKGROUND = 0xE61F1F23.toInt()
    }

    private val wm = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val hideRunnable = Runnable { dismiss() }

    private var root: LinearLayout? = null
    private var iconView: ImageView? = null
    private var textView: TextView? = null

    // El sprite se decodifica solo cuando cambia la especie o el nivel.
    private var iconKey: Pair<MascotSpecies, MascotLevel>? = null
    private var iconBitmap: Bitmap? = null

    fun show() {
        val count = Stats.read(service).total
        val text = service.getString(
            R.string.block_bubble_text,
            service.getString(R.string.toast_blocked),
            count
        )
        try {
            val view = root ?: build().also { v ->
                wm.addView(v, layoutParams())
                root = v
                v.alpha = 0f
                v.animate().alpha(1f).setDuration(FADE_MS).start()
            }
            iconView?.setImageBitmap(icon())
            textView?.text = text
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // TalkBack anuncia el título del panel al aparecer (el Toast
                // antiguo también se anunciaba).
                view.accessibilityPaneTitle = text
            }
            handler.removeCallbacks(hideRunnable)
            handler.postDelayed(hideRunnable, VISIBLE_MS)
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo mostrar la burbuja, uso Toast", e)
            root = null
            Toast.makeText(service, R.string.toast_blocked, Toast.LENGTH_SHORT).show()
        }
    }

    /** Oculta la burbuja (con fundido). Llamar también al desvincular el servicio. */
    fun dismiss() {
        handler.removeCallbacks(hideRunnable)
        val view = root ?: return
        root = null
        view.animate().alpha(0f).setDuration(FADE_MS).withEndAction {
            runCatching { wm.removeView(view) }
        }.start()
    }

    private fun build(): LinearLayout {
        val icon = ImageView(service).apply {
            layoutParams = LinearLayout.LayoutParams(dp(ICON_DP), dp(ICON_DP))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val label = TextView(service).apply {
            setTextColor(0xFFFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginStart = dp(8) }
        }
        iconView = icon
        textView = label
        return LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), dp(5), dp(14), dp(5))
            background = GradientDrawable().apply {
                cornerRadius = dp(999).toFloat()
                setColor(BACKGROUND)
            }
            elevation = dp(4).toFloat()
            addView(icon)
            addView(label)
        }
    }

    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        y = statusBarHeight() + dp(8)
    }

    private fun icon(): Bitmap {
        val key = Collection.currentSpecies(service) to Streak.current(service).level
        iconBitmap?.takeIf { key == iconKey }?.let { return it }
        return trimmedIcon(key.first, key.second, dp(ICON_DP)).also {
            iconKey = key
            iconBitmap = it
        }
    }

    /**
     * Los sprites traen mucho margen transparente (pensado para la pantalla
     * grande); a tamaño de icono la mascota quedaba diminuta. Recorta al
     * contenido visible y lo encaja centrado en un cuadrado de [sizePx].
     */
    private fun trimmedIcon(species: MascotSpecies, level: MascotLevel, sizePx: Int): Bitmap {
        val src = BitmapFactory.decodeResource(
            service.resources,
            species.spriteRes(level),
            BitmapFactory.Options().apply { inSampleSize = 4 }
        )
        var left = src.width; var top = src.height; var right = -1; var bottom = -1
        val row = IntArray(src.width)
        for (y in 0 until src.height) {
            src.getPixels(row, 0, src.width, 0, y, src.width, 1)
            for (x in row.indices) {
                if ((row[x] ushr 24) > 16) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    bottom = y
                }
            }
        }
        val content = if (right < 0) Rect(0, 0, src.width, src.height)
                      else Rect(left, top, right + 1, bottom + 1)
        val scale = sizePx.toFloat() / maxOf(content.width(), content.height())
        val w = content.width() * scale
        val h = content.height() * scale
        val dst = RectF((sizePx - w) / 2f, (sizePx - h) / 2f, (sizePx + w) / 2f, (sizePx + h) / 2f)
        return Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).also { out ->
            Canvas(out).drawBitmap(src, content, dst, Paint(Paint.FILTER_BITMAP_FLAG))
            src.recycle()
        }
    }

    private fun statusBarHeight(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            wm.currentWindowMetrics.windowInsets
                .getInsets(WindowInsets.Type.statusBars()).top
        } else {
            dp(24)
        }

    private fun dp(value: Int): Int =
        (value * service.resources.displayMetrics.density).toInt()
}
