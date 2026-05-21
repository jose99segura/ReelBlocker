package app.reelblocker

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * Servicio principal. Recibe eventos de Instagram y YouTube, decide si estamos
 * en Reels/Shorts a pantalla completa y, si es asi, nos saca con el boton atras.
 *
 * Logs: adb logcat -s ReelBlocker
 *
 * Las listas de pistas dependen de los resource-id internos de cada app y pueden
 * romperse con actualizaciones de Instagram o YouTube.
 */
class BlockerService : AccessibilityService() {

    companion object {
        private const val TAG = "ReelBlocker"

        private const val PKG_INSTAGRAM = "com.instagram.android"
        private const val PKG_YOUTUBE = "com.google.android.youtube"

        private val INSTAGRAM_REEL_HINTS = listOf(
            "clips_viewer",
            "reel_viewer",
            "clips_swipe_refresh"
        )

        private val YOUTUBE_SHORTS_HINTS = listOf(
            "reel_watch_player",
            "reel_watch_fragment_root",
            "reel_player_page_container",
            "reel_recycler"
        )

        private const val MIN_INTERVAL_MS = 600L
        // Tras salir de Reels, ventana de gracia para no encadenar otro back
        // si Instagram deja un fragmento residual que aun matchea.
        private const val POST_EXIT_GRACE_MS = 1500L
        // Un nodo solo cuenta como "Reels a pantalla completa" si ocupa al
        // menos este porcentaje del ancho y alto de la pantalla.
        private const val FULLSCREEN_FRACTION = 0.6
    }

    private var lastActionTime = 0L
    private var lastReelsPackage: String? = null
    private var lastReelsExitTime = 0L
    private var displayWidth = 0
    private var displayHeight = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        val dm = resources.displayMetrics
        displayWidth = dm.widthPixels
        displayHeight = dm.heightPixels
        Log.d(TAG, "==> Servicio CONECTADO  pantalla=${displayWidth}x${displayHeight}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString() ?: return

        Log.v(TAG, "Evento pkg=$pkg tipo=${event.eventType}")

        if (pkg != PKG_INSTAGRAM && pkg != PKG_YOUTUBE) return

        val root = rootInActiveWindow
        if (root == null) {
            Log.w(TAG, "rootInActiveWindow NULL para $pkg")
            return
        }

        val hints = when (pkg) {
            PKG_INSTAGRAM -> INSTAGRAM_REEL_HINTS
            PKG_YOUTUBE -> YOUTUBE_SHORTS_HINTS
            else -> return
        }

        val matched = findFullscreenMatch(root, hints)
        if (matched != null) {
            handleReelsDetected(pkg, matched)
        } else {
            handleReelsAbsent(pkg)
        }
    }

    private fun handleReelsDetected(pkg: String, matchedId: String) {
        val now = SystemClock.elapsedRealtime()

        // Ventana de gracia justo despues de un back: ignoramos el match para
        // no encadenar otro back que acabe cerrando la app.
        if (now - lastReelsExitTime < POST_EXIT_GRACE_MS) {
            Log.v(TAG, "En ventana de gracia post-salida, ignoro match en $pkg ($matchedId)")
            return
        }

        // Si ya estabamos dentro de Reels de este paquete, no volvemos a pulsar back.
        if (lastReelsPackage == pkg) {
            Log.v(TAG, "Ya marcado dentro de Reels en $pkg, no repito back")
            return
        }

        Log.d(TAG, "DETECTADO Reels/Shorts en $pkg por id: $matchedId -> back")
        lastReelsPackage = pkg
        triggerBack(pkg)
    }

    private fun handleReelsAbsent(pkg: String) {
        if (lastReelsPackage == pkg) {
            Log.v(TAG, "Salimos de Reels en $pkg, abro ventana de gracia")
            lastReelsPackage = null
            lastReelsExitTime = SystemClock.elapsedRealtime()
        }
    }

    /**
     * Busca un nodo cuyo viewIdResourceName contiene alguna pista Y que es
     * visible Y ocupa la mayor parte de la pantalla. Devuelve el id o null.
     */
    private fun findFullscreenMatch(
        root: AccessibilityNodeInfo,
        hints: List<String>
    ): String? {
        val minWidth = (displayWidth * FULLSCREEN_FRACTION).toInt()
        val minHeight = (displayHeight * FULLSCREEN_FRACTION).toInt()
        val bounds = Rect()

        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        val maxNodes = 800

        while (queue.isNotEmpty() && visited < maxNodes) {
            val node = queue.removeFirst()
            visited++

            val id = node.viewIdResourceName
            if (id != null && hints.any { id.contains(it, ignoreCase = true) }) {
                if (node.isVisibleToUser) {
                    node.getBoundsInScreen(bounds)
                    if (bounds.width() >= minWidth && bounds.height() >= minHeight) {
                        return id
                    } else {
                        Log.v(TAG, "Match $id descartado por tamano ${bounds.width()}x${bounds.height()}")
                    }
                } else {
                    Log.v(TAG, "Match $id descartado por no visible")
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun triggerBack(pkg: String) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastActionTime < MIN_INTERVAL_MS) {
            Log.v(TAG, "Anti-rebote: ignorando")
            return
        }
        lastActionTime = now
        val ok = performGlobalAction(GLOBAL_ACTION_BACK)
        Log.d(TAG, "performGlobalAction(BACK) = $ok")
        if (ok) {
            Stats.increment(this, pkg)
            Toast.makeText(this, R.string.toast_blocked, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Servicio interrumpido")
    }
}
