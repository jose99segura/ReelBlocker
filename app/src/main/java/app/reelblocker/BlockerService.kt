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

        // Fuente única de verdad para IG/YT en [Stats]; aquí solo se referencian.
        private const val PKG_INSTAGRAM = Stats.PKG_INSTAGRAM
        private const val PKG_YOUTUBE = Stats.PKG_YOUTUBE
        private const val PKG_FACEBOOK = Stats.PKG_FACEBOOK
        private const val PKG_TIKTOK = "com.zhiliaoapp.musically"

        // Etiquetas (content-description) del item "Perfil" de la barra inferior
        // de TikTok, para redirigir alli en vez de hacer BACK (que no saca del
        // feed). Cubre es/en/pt; si no hay match se cae a GLOBAL_ACTION_HOME.
        private val TIKTOK_PROFILE_LABELS = listOf("Perfil", "Profile")

        // Los hints de Reels/Stories/Shorts viven ahora en [HintConfig]
        // (defaults baked-in + override remoto por JSON). El servicio solo lee
        // la cache. Las listas de DM/exit de abajo siguen aqui: son estables y
        // no se gestionan en remoto.

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

        // Cada cuanto, como maximo, corremos el chequeo de "deteccion rota"
        // ante actividad de IG/YT. Barato pero no en cada evento.
        private const val HEALTH_CHECK_INTERVAL_MS = 30L * 60 * 1000
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
    // Rate-limit del chequeo de salud (deteccion rota) ante actividad IG/YT.
    private var lastHealthCheck = 0L

    /**
     * Log verbose solo en builds debug. El lambda evita construir el string
     * en release; [onAccessibilityEvent] se dispara en cada evento del sistema.
     */
    private inline fun logv(msg: () -> String) {
        if (BuildConfig.DEBUG) Log.v(TAG, msg())
    }

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

        logv { "Evento pkg=$pkg tipo=${event.eventType}" }

        if (pkg != PKG_INSTAGRAM && pkg != PKG_YOUTUBE && pkg != PKG_FACEBOOK && pkg != PKG_TIKTOK) return

        // Descanso Pro activo: el servicio sigue conectado (la racha cuenta el
        // día) pero no disparamos back durante la pausa.
        if (Breaks.isOnBreak(this)) return

        // Gate por preferencias del usuario.
        if (!Stats.isAppEnabled(this, pkg)) {
            logv { "Bloqueo desactivado por el usuario en $pkg" }
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

        // Facebook EN PAUSA: no hay señal de deteccion fiable (resource-ids
        // ofuscados como "(name removed)", una sola Activity FbMainTabActivity
        // para todo, y el arbol del visor inmersivo viene contaminado con el
        // chrome del feed de inicio). En debug seguimos volcando para investigar;
        // mientras facebookReels este vacia NO recorremos el arbol (ahorro en
        // release). Si algun dia se publica una señal, FB se reactiva solo.
        if (pkg == PKG_FACEBOOK) {
            dumpFacebookTree(event, root)  // no-op en release (guard interno)
            if (HintConfig.facebookReels(this).isEmpty()) return
        }

        // TikTok: la app entera es feed, pero el reproductor inmersivo vertical
        // (Para ti/Siguiendo/visor) se delata por ids semanticos como
        // "video_player_progress" — ausentes en perfil/DMs/busqueda/rejilla.
        // No exigimos fullscreen (la barra de progreso es fina): basta con que
        // un nodo VISIBLE tenga la firma. Asi se bloquea el doomscroll y se
        // dejan usables las demas pestañas.
        if (pkg == PKG_TIKTOK) {
            dumpDiscoveryTree(event, root, "TT")  // no-op en release
            val ttHints = HintConfig.tiktokFeed(this)
            val ttMatch = if (ttHints.isEmpty()) null else findHintId(root, ttHints)
            if (ttMatch != null) handleReelsDetected(pkg, ttMatch) else handleReelsAbsent(pkg)
            return
        }

        // Actualizar marca de "vimos DM" si el arbol tiene hints DM AHORA.
        // Esto se evalua en cada evento, asi capturamos la ultima vez que
        // el usuario estuvo en una pantalla con thread de DM en el arbol.
        if (pkg == PKG_INSTAGRAM && containsAnyHint(root, INSTAGRAM_DM_HINTS)) {
            lastSeenDmTimestamp = SystemClock.elapsedRealtime()
        }

        // Salud: el usuario esta usando IG/YT ahora mismo. Si antes
        // bloqueabamos pero llevamos demasiado sin un solo bloqueo, los hints
        // probablemente se rompieron → avisar. Throttled.
        val nowWall = System.currentTimeMillis()
        if (nowWall - lastHealthCheck >= HEALTH_CHECK_INTERVAL_MS) {
            lastHealthCheck = nowWall
            HealthCheck.maybeWarnBreakage(this)
        }

        val hints = when (pkg) {
            PKG_INSTAGRAM -> {
                val list = ArrayList<String>()
                list.addAll(HintConfig.instagramReels(this))
                if (Stats.effectiveStoriesBlocked(this)) list.addAll(HintConfig.instagramStories(this))
                list
            }
            PKG_YOUTUBE -> HintConfig.youtubeShorts(this)
            PKG_FACEBOOK -> HintConfig.facebookReels(this)
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
                logv { "Continuando reel DM autorizado, ignorado" }
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
            logv { "Ventana de gracia post-salida, ignoro match en $pkg ($matchedId)" }
            return
        }

        if (lastReelsPackage == pkg) {
            logv { "Ya marcado dentro de Reels en $pkg, no repito back" }
            return
        }

        Log.d(TAG, "DETECTADO Reels/Shorts en $pkg por id: $matchedId -> back")
        lastReelsPackage = pkg
        triggerBack(pkg)
    }

    private fun handleReelsAbsent(pkg: String) {
        if (lastReelsPackage == pkg) {
            logv { "Salimos de Reels en $pkg, abro ventana de gracia" }
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
     * Diagnostico Facebook. Los resource-id de FB vienen ofuscados como
     * "(name removed)", asi que la deteccion por id NO sirve. Este volcado
     * captura las señales alternativas viables:
     *   - la Activity host (event.className en TYPE_WINDOW_STATE_CHANGED),
     *   - content-description y text del overlay (rail de like/comentar/etc.),
     * para decidir por que firma reconocer el visor de Reels de Facebook.
     */
    private fun dumpFacebookTree(event: AccessibilityEvent, root: AccessibilityNodeInfo) {
        // Diagnóstico solo en debug: Facebook está en modo discovery (no bloquea),
        // así que en release no recorremos el árbol ni logueamos nada.
        if (!BuildConfig.DEBUG) return

        // Siempre (sin throttle): en cambios de ventana, la Activity host.
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "FB STATE className=${event.className}")
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastFbDumpTime < 1500L) return
        lastFbDumpTime = now

        Log.d(TAG, "FB === evento tipo=${event.eventType} className=${event.className}")
        val labels = LinkedHashSet<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 1500 && labels.size < 80) {
            val node = queue.removeFirst()
            visited++
            node.contentDescription?.toString()?.let { if (it.isNotBlank()) labels.add("cd: " + it.take(48)) }
            node.text?.toString()?.let { if (it.isNotBlank()) labels.add("tx: " + it.take(48)) }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        if (labels.isEmpty()) {
            Log.d(TAG, "FB   (sin labels — $visited nodos)")
        } else {
            labels.forEach { Log.d(TAG, "FB   $it") }
        }
    }

    /**
     * Diagnostico generico de discovery para una app nueva (p.ej. TikTok).
     * Captura TODO lo potencialmente direccionable: resource-ids reales (los
     * obfuscados "(name removed)" o "0_resource_name_obfuscated" se descartan),
     * content-description, text, y la Activity host en cambios de ventana.
     * Solo en debug y rate-limited. [prefix] etiqueta las lineas en logcat.
     */
    private fun dumpDiscoveryTree(event: AccessibilityEvent, root: AccessibilityNodeInfo, prefix: String) {
        if (!BuildConfig.DEBUG) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(TAG, "$prefix STATE className=${event.className}")
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastFbDumpTime < 1500L) return
        lastFbDumpTime = now

        Log.d(TAG, "$prefix === evento tipo=${event.eventType} className=${event.className}")
        val ids = LinkedHashSet<String>()
        val labels = LinkedHashSet<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited < 1500 && ids.size + labels.size < 120) {
            val node = queue.removeFirst()
            visited++
            node.viewIdResourceName?.let {
                if (it.isNotBlank() && !it.contains("(name removed)") && !it.contains("obfuscated")) {
                    ids.add(it.substringAfter("id/", it))
                }
            }
            node.contentDescription?.toString()?.let { if (it.isNotBlank()) labels.add("cd: " + it.take(48)) }
            node.text?.toString()?.let { if (it.isNotBlank()) labels.add("tx: " + it.take(48)) }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        if (ids.isEmpty()) Log.d(TAG, "$prefix   (sin resource-ids usables — $visited nodos)")
        else ids.forEach { Log.d(TAG, "$prefix   id: $it") }
        labels.forEach { Log.d(TAG, "$prefix   $it") }
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
                        logv { "Match $id descartado por tamano ${bounds.width()}x${bounds.height()}" }
                    }
                } else {
                    logv { "Match $id descartado por no visible" }
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    /**
     * Variante para TikTok: devuelve el id del primer nodo cuyo
     * viewIdResourceName contiene una pista. NO exige fullscreen (la barra
     * "video_player_progress" es pequeña) ni visibilidad — la firma solo existe
     * en el arbol mientras el reproductor inmersivo esta montado; al cambiar a
     * Perfil/DMs TikTok lo destruye y desaparece, asi que la presencia es una
     * señal limpia de on/off. El arbol del feed es grande (videos precargados),
     * por eso el tope de nodos es mas alto que en IG/YT.
     */
    private fun findHintId(
        root: AccessibilityNodeInfo,
        hints: List<String>
    ): String? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        val maxNodes = 2500
        while (queue.isNotEmpty() && visited < maxNodes) {
            val node = queue.removeFirst()
            visited++
            val id = node.viewIdResourceName
            if (id != null && hints.any { id.contains(it, ignoreCase = true) }) {
                return id
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
            logv { "Anti-rebote: ignorando" }
            return
        }
        lastActionTime = now
        // En TikTok el BACK no saca del feed (lo interpreta como "video
        // anterior"), asi que redirigimos a una pestaña segura (Perfil) y, si
        // no la encontramos, salimos al launcher. En IG/YT el BACK si funciona.
        val ok = if (pkg == PKG_TIKTOK) escapeTikTokFeed() else performGlobalAction(GLOBAL_ACTION_BACK)
        Log.d(TAG, "accion salida pkg=$pkg ok=$ok")
        if (ok) {
            Stats.increment(this, pkg)
            HealthCheck.recordBlock(this)
            Toast.makeText(this, R.string.toast_blocked, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Saca al usuario del feed de TikTok sin cerrar la app: busca el item
     * "Perfil" de la barra inferior y lo pulsa (deja DMs/perfil/busqueda
     * usables). Si no se encuentra (otro idioma, cambio de UI), cae a
     * GLOBAL_ACTION_HOME para sacarlo de la app igualmente.
     */
    private fun escapeTikTokFeed(): Boolean {
        val root = rootInActiveWindow ?: return performGlobalAction(GLOBAL_ACTION_HOME)
        val profile = findClickableByContentDesc(root, TIKTOK_PROFILE_LABELS)
        return if (profile != null) {
            val clicked = profile.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            Log.d(TAG, "TikTok: pulsado Perfil = $clicked")
            if (clicked) true else performGlobalAction(GLOBAL_ACTION_HOME)
        } else {
            Log.d(TAG, "TikTok: no se encontro Perfil, salgo al launcher")
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    /**
     * BFS: primer nodo cuyo content-description coincide exactamente (ignorando
     * mayusculas) con alguna etiqueta; sube hasta el primer ancestro clickable
     * y lo devuelve (el item de nav suele tener el texto en un hijo no
     * clickable). Null si no hay match.
     */
    private fun findClickableByContentDesc(
        root: AccessibilityNodeInfo,
        labels: List<String>
    ): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0
        val maxNodes = 800
        while (queue.isNotEmpty() && visited < maxNodes) {
            val node = queue.removeFirst()
            visited++
            val cd = node.contentDescription?.toString()
            if (cd != null && labels.any { it.equals(cd, ignoreCase = true) }) {
                var target: AccessibilityNodeInfo? = node
                while (target != null && !target.isClickable) target = target.parent
                if (target != null) return target
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        // La accesibilidad se desvincula: el sistema nos mato o el usuario
        // apago el servicio. Avisar (salvo desactivacion deliberada reciente),
        // porque a partir de ahora los Reels dejan de bloquearse.
        Log.d(TAG, "Servicio desvinculado")
        HealthCheck.notifyProtectionOff(this)
        return super.onUnbind(intent)
    }

    override fun onInterrupt() {
        Log.d(TAG, "Servicio interrumpido")
    }
}
