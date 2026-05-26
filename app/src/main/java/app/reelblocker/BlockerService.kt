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
 * Excepcion: si el reel viene de un DM (mensaje directo) de Instagram, no
 * bloqueamos — la heuristica es mirar si la pantalla anterior contenia hints
 * de DM.
 *
 * Logs: adb logcat -s ReelBlocker
 *
 * Las listas de pistas dependen de los resource-id internos de cada app y
 * pueden romperse con actualizaciones de Instagram o YouTube.
 */
class BlockerService : AccessibilityService() {

    companion object {
        private const val TAG = "ReelBlocker"

        private const val PKG_INSTAGRAM = "com.instagram.android"
        private const val PKG_YOUTUBE = "com.google.android.youtube"
        private const val PKG_FACEBOOK = "com.facebook.katana"

        // Reels publicos (TikTok-like). Bloquear siempre si IG esta activo.
        private val INSTAGRAM_REELS_HINTS = listOf(
            "clips_viewer",
            "clips_swipe_refresh"
        )

        // HISTORIAS. Instagram las llama "reel" internamente (legacy 2016).
        // Bloquear solo si el usuario activa el sub-switch.
        private val INSTAGRAM_STORIES_HINTS = listOf(
            "reel_viewer",
            "story_viewer"
        )

        // Hints en el ARBOL: solo se usan para elevar el flag a true en
        // eventos de contenido. Tienen que ser muy especificos para no
        // confundir el icono "DM" del menu (presente en todas las pantallas)
        // o el fragmento de comentarios de un reel.
        private val INSTAGRAM_DM_HINTS = listOf(
            "direct_thread",
            "row_thread",
            "thread_composer",
            "direct_composer"
        )

        // Hints en event.className (solo TYPE_WINDOW_STATE_CHANGED). Mantener
        // por si una version futura de IG usa estas clases — hoy no las usa
        // (ver INSTAGRAM_EXIT_CLASSNAMES). Las pantallas DM hoy son
        // ModalActivity, lo que no nos da informacion direccionable.
        private val INSTAGRAM_DM_CLASSNAME_HINTS = listOf(
            "DirectThread",
            "DirectInbox",
            "DirectMessage"
        )

        // Clases que indican SALIDA del contexto DM: solo cuando vemos una de
        // estas reseteamos el estado de CONSUMED/ARMED a READY. ModalActivity
        // y TransparentModalActivity NO cuentan: la primera hostea los DMs,
        // la segunda hostea el visor de reels recibido por DM.
        private val INSTAGRAM_EXIT_CLASSNAMES = listOf(
            "InstagramMainActivity",
            "MainTabActivity"
        )

        private val YOUTUBE_SHORTS_HINTS = listOf(
            "reel_watch_player",
            "reel_watch_fragment_root",
            "reel_player_page_container",
            "reel_recycler"
        )

        private const val MIN_INTERVAL_MS = 600L
        private const val POST_EXIT_GRACE_MS = 1500L
        private const val FULLSCREEN_FRACTION = 0.6
        // Tras consumir un bypass DM, ignorar matches durante este intervalo
        // para no contar dos veces el mismo reel por eventos de contenido
        // que llegan en rafaga al abrir el visor.
        private const val DM_CONSUMED_GRACE_MS = 2000L
        // Red de seguridad: si por lo que sea no detectamos el swipe, tras
        // este tiempo cualquier nuevo match dispara BACK. 5 min es de
        // sobra para ver un reel pero corta una sesion olvidada.
        private const val DM_VIEW_BUDGET_MS = 300_000L
        // Si vimos hints DM hace menos de esta ventana, asumimos que el
        // reel que se acaba de detectar viene de un DM. Cubre la transicion
        // DM thread -> reel viewer (< 500 ms tipicamente).
        private const val DM_RECENCY_MS = 2500L
    }

    private var lastActionTime = 0L
    private var lastReelsPackage: String? = null
    private var lastReelsExitTime = 0L
    private var displayWidth = 0
    private var displayHeight = 0

    // Estado mucho mas simple: mientras estamos "viendo un reel autorizado
    // por DM", este flag esta activo. Se activa cuando se detecta un reel
    // fullscreen Y en ese instante el arbol tiene hints DM. Se desactiva al
    // hacer scroll, al salir del visor, o al expirar el watchdog.
    private var watchingDmReel = false
    private var watchingDmReelStart = 0L
    // Timestamp del ultimo evento donde vimos hints DM en el arbol. Sirve
    // para decidir si un reel recien detectado viene de un DM.
    private var lastSeenDmTimestamp = 0L
    // Rate-limit del volcado diagnostico de Facebook.
    private var lastFbDumpTime = 0L

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

        if (pkg != PKG_INSTAGRAM && pkg != PKG_YOUTUBE && pkg != PKG_FACEBOOK) return

        // Descanso Pro activo: el servicio sigue conectado (la racha cuenta el
        // día) pero no disparamos back durante la pausa.
        if (Breaks.isOnBreak(this)) return

        // Gate por preferencias del usuario.
        if (!Stats.isAppEnabled(this, pkg)) {
            Log.v(TAG, "Bloqueo desactivado por el usuario en $pkg")
            return
        }

        // Detectar swipe dentro del visor: si estabamos viendo un reel
        // permitido por DM y el usuario hace scroll para pasar al siguiente,
        // terminamos el bypass y dejamos que el proximo match dispare BACK.
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED &&
            pkg == PKG_INSTAGRAM &&
            watchingDmReel) {
            Log.d(TAG, "Scroll detectado en visor DM, fin de bypass — proximo reel se bloquea")
            watchingDmReel = false
            lastReelsPackage = null
            return
        }

        val root = rootInActiveWindow
        if (root == null) {
            Log.w(TAG, "rootInActiveWindow NULL para $pkg")
            return
        }

        // FASE DESCUBRIMIENTO Facebook: por ahora solo volcamos el arbol
        // para ver que resource-ids / className expone. Aun no bloqueamos.
        if (pkg == PKG_FACEBOOK) {
            dumpFacebookTree(event, root)
            return
        }

        // Actualizar marca de "vimos DM" si el arbol tiene hints DM AHORA.
        // Esto se evalua en cada evento, asi capturamos la ultima vez que
        // el usuario estuvo en una pantalla con thread de DM en el arbol.
        if (pkg == PKG_INSTAGRAM && containsAnyHint(root, INSTAGRAM_DM_HINTS)) {
            lastSeenDmTimestamp = SystemClock.elapsedRealtime()
        }

        val hints = when (pkg) {
            PKG_INSTAGRAM -> {
                val list = ArrayList<String>(4)
                list.addAll(INSTAGRAM_REELS_HINTS)
                if (Stats.effectiveStoriesBlocked(this)) list.addAll(INSTAGRAM_STORIES_HINTS)
                list
            }
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
        val dmAllowed = pkg == PKG_INSTAGRAM && Stats.effectiveDmAllowed(this)

        // Si ya estabamos viendo un reel autorizado por DM y no ha pasado el
        // watchdog, ignoramos. Si pasa el watchdog, dejamos caer al fire BACK.
        if (dmAllowed && watchingDmReel) {
            if (now - watchingDmReelStart < DM_VIEW_BUDGET_MS) {
                Log.v(TAG, "Continuando reel DM autorizado, ignorado")
                lastReelsPackage = pkg
                return
            } else {
                Log.d(TAG, "Watchdog DM expirado, reanudo bloqueo")
                watchingDmReel = false
                lastReelsPackage = null
            }
        }

        // Punto critico: si vimos hints DM hace muy poco (< DM_RECENCY_MS),
        // interpretamos que el reel viene de un DM. La transicion DM thread
        // -> reel viewer es < 500 ms, asi que 2.5 s cubre el caso con
        // margen. Esto funciona aunque el visor del reel sustituya el
        // thread en el arbol (que es lo que pasaba con el approach anterior).
        val msSinceDm = now - lastSeenDmTimestamp
        val cameFromDm = pkg == PKG_INSTAGRAM &&
            lastSeenDmTimestamp > 0 &&
            msSinceDm < DM_RECENCY_MS
        if (dmAllowed && !watchingDmReel && cameFromDm) {
            Log.d(TAG, "Reel autorizado: vimos DM hace ${msSinceDm}ms (id=$matchedId)")
            watchingDmReel = true
            watchingDmReelStart = now
            lastReelsPackage = pkg
            return
        }

        if (now - lastReelsExitTime < POST_EXIT_GRACE_MS) {
            Log.v(TAG, "Ventana de gracia post-salida, ignoro match en $pkg ($matchedId)")
            return
        }

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
            watchingDmReel = false
        }
    }

    /**
     * BFS rapido: devuelve true si encuentra algun nodo cuyo
     * viewIdResourceName contiene alguna pista. No exige visibilidad ni
     * tamano — basta con que el fragmento exista en el arbol.
     */
    /**
     * Diagnostico: log los primeros N ids del arbol que contengan keywords
     * candidatas a DM. Util para descubrir los resource-id reales que usa
     * Instagram cuando deja de coincidir con INSTAGRAM_DM_HINTS.
     */
    private fun dumpDmCandidates(root: AccessibilityNodeInfo) {
        val keywords = listOf("direct", "thread", "message", "msg", "chat", "inbox")
        val seen = mutableSetOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 1500 && seen.size < 25) {
            val node = queue.removeFirst()
            visited++
            node.viewIdResourceName?.let { id ->
                if (keywords.any { id.contains(it, ignoreCase = true) } && seen.add(id)) {
                    Log.d(TAG, "  dm-candidate id: $id")
                }
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        if (seen.isEmpty()) Log.d(TAG, "  no dm-candidate ids encontrados")
    }

    /**
     * Diagnostico Facebook: cada ~1.5 s, volcar el className del evento y
     * todos los resource-id no nulos del arbol. Sirve para descubrir por
     * que se reconoce el visor de Reels de Facebook.
     */
    private fun dumpFacebookTree(event: AccessibilityEvent, root: AccessibilityNodeInfo) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastFbDumpTime < 1500L) return
        lastFbDumpTime = now

        Log.d(TAG, "FB === evento tipo=${event.eventType} className=${event.className}")
        val ids = mutableSetOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 1500 && ids.size < 60) {
            val node = queue.removeFirst()
            visited++
            node.viewIdResourceName?.let { if (it.isNotBlank()) ids.add(it) }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        if (ids.isEmpty()) {
            Log.d(TAG, "FB   (sin resource-ids en el arbol — $visited nodos)")
        } else {
            ids.forEach { Log.d(TAG, "FB   id: $it") }
        }
    }

    private fun containsAnyHint(
        root: AccessibilityNodeInfo,
        hints: List<String>
    ): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        val maxNodes = 800
        while (queue.isNotEmpty() && visited < maxNodes) {
            val node = queue.removeFirst()
            visited++
            val id = node.viewIdResourceName
            if (id != null && hints.any { id.contains(it, ignoreCase = true) }) {
                return true
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return false
    }

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
