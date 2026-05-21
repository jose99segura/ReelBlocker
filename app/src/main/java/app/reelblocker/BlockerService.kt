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

        private val INSTAGRAM_REEL_HINTS = listOf(
            "clips_viewer",
            "reel_viewer",
            "clips_swipe_refresh"
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
        // para no contar dos veces el mismo reel por eventos de contenido.
        private const val DM_CONSUMED_GRACE_MS = 2000L
        // Presupuesto de visualizacion del reel desde DM. Pasado este tiempo
        // dentro del visor, cualquier nuevo match dispara BACK (para que el
        // usuario no pueda hacer swipe a reels infinitos). 10 s cubre un
        // reel corto tipico; despues lo cerramos.
        private const val DM_VIEW_BUDGET_MS = 10_000L
    }

    private var lastActionTime = 0L
    private var lastReelsPackage: String? = null
    private var lastReelsExitTime = 0L
    private var displayWidth = 0
    private var displayHeight = 0

    // Maquina de estados del bypass DM:
    //  READY    - sin DM detectado; al ver pantalla DM pasa a ARMED.
    //  ARMED    - DM detectado; el proximo reel se permite y pasa a CONSUMED.
    //  CONSUMED - bypass usado; para volver a ARMED hace falta primero salir
    //             del contexto DM (transicion a className NO-DM) y volver a
    //             entrar. Asi el visor de reels que cuelga del DirectThread-
    //             Activity no rearma el bypass por si solo.
    private enum class DmState { READY, ARMED, CONSUMED }
    private var dmState = DmState.READY
    private var dmConsumedUntil = 0L
    // Cuando se permite un reel desde DM, fijamos este timestamp. Si el
    // usuario sigue en el visor pasados DM_VIEW_BUDGET_MS, dejamos de ignorar
    // matches y disparamos BACK.
    private var dmAllowanceStarted = 0L

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

        // Gate por preferencias del usuario.
        if (!Stats.isAppEnabled(this, pkg)) {
            Log.v(TAG, "Bloqueo desactivado por el usuario en $pkg")
            return
        }

        val root = rootInActiveWindow
        if (root == null) {
            Log.w(TAG, "rootInActiveWindow NULL para $pkg")
            return
        }

        // Actualizar el estado del bypass DM para Instagram.
        if (pkg == PKG_INSTAGRAM) {
            updateDmState(event, root)
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

    /**
     * Actualiza la maquina de estados del bypass DM en funcion del evento.
     * Solo se hacen transiciones validas; el bypass solo puede rearmarse
     * tras una transicion explicita READY (que requiere haber salido del DM).
     */
    private fun updateDmState(event: AccessibilityEvent, root: AccessibilityNodeInfo) {
        val isStateChange = event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        val cls = event.className?.toString().orEmpty()
        val classDm = INSTAGRAM_DM_CLASSNAME_HINTS.any { cls.contains(it, ignoreCase = true) }
        // SOLO las clases listadas como exit cuentan como "salida del DM".
        // ModalActivity (DM thread) y TransparentModalActivity (visor del
        // reel desde DM) NO son exit: el usuario sigue en contexto DM.
        val classExit = isStateChange && INSTAGRAM_EXIT_CLASSNAMES.any {
            cls.contains(it, ignoreCase = true)
        }
        val treeDm = dmState == DmState.READY && containsAnyHint(root, INSTAGRAM_DM_HINTS)

        val previous = dmState
        dmState = when (dmState) {
            DmState.READY -> when {
                isStateChange && classDm -> DmState.ARMED
                treeDm -> DmState.ARMED
                else -> DmState.READY
            }
            DmState.ARMED -> when {
                classExit -> DmState.READY  // Usuario salio del DM sin tocar reel.
                else -> DmState.ARMED
            }
            DmState.CONSUMED -> when {
                classExit -> DmState.READY  // Usuario salio del contexto DM. Listo para rearmar.
                else -> DmState.CONSUMED
            }
        }
        if (dmState != previous) {
            Log.d(TAG, "DM state: $previous -> $dmState  (cls=$cls classDm=$classDm classExit=$classExit treeDm=$treeDm)")
            if (dmState == DmState.ARMED) dumpDmCandidates(root)
            // Al volver a READY tras una sesion DM, limpiar cualquier residuo
            // que pudiera mantener el bypass activo en el siguiente match.
            if (dmState == DmState.READY && previous == DmState.CONSUMED) {
                lastReelsPackage = null
                dmAllowanceStarted = 0L
                dmConsumedUntil = 0L
                Log.d(TAG, "  reset post-DM: limpiando lastReelsPackage y budget")
            }
        }
    }

    private fun handleReelsDetected(pkg: String, matchedId: String) {
        val now = SystemClock.elapsedRealtime()

        val dmAllowed = pkg == PKG_INSTAGRAM && Stats.isDmReelsAllowed(this)
        if (dmAllowed && now < dmConsumedUntil) {
            // Eventos de contenido del MISMO reel recien permitido.
            Log.v(TAG, "Match dentro de la ventana post-consumo DM, ignorado")
            lastReelsPackage = pkg
            return
        }
        if (dmAllowed && dmState == DmState.ARMED) {
            Log.d(TAG, "Reel desde DM, permitido (id=$matchedId). Bypass consumido.")
            dmState = DmState.CONSUMED
            dmConsumedUntil = now + DM_CONSUMED_GRACE_MS
            dmAllowanceStarted = now
            lastReelsPackage = pkg
            return
        }
        // Si veniamos de un bypass DM y se agoto el presupuesto de
        // visualizacion, dejar de ignorar este match y caer en fire BACK.
        if (dmAllowanceStarted > 0 && now - dmAllowanceStarted >= DM_VIEW_BUDGET_MS) {
            Log.d(TAG, "Budget DM agotado (${(now - dmAllowanceStarted) / 1000}s). Reanudo bloqueo.")
            dmAllowanceStarted = 0L
            lastReelsPackage = null
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
            dmAllowanceStarted = 0L
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
