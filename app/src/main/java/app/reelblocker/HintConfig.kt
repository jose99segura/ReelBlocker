package app.reelblocker

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fuente única de los hints de detección (resource-id de IG/YT que delatan
 * la superficie de vídeo corto).
 *
 * El problema que resuelve: cuando Instagram o YouTube cambian un resource-id,
 * el bloqueo deja de funcionar SIN error visible. Antes solo se arreglaba con
 * un release nuevo (días de propagación). Ahora los hints se pueden parchear
 * publicando un JSON remoto — todos los usuarios se actualizan en horas.
 *
 * Garantía de seguridad: los defaults baked-in son el SUELO. El JSON remoto
 * solo puede *añadir* hints (unión), nunca dejar las listas vacías. Un payload
 * corrupto, vacío o inalcanzable nunca desactiva la detección — se cae a los
 * defaults.
 *
 * El servicio ([BlockerService]) corre en proceso aparte y lee el JSON cacheado
 * en SharedPreferences. El fetch de red lo dispara el proceso principal
 * ([MainActivity]) al abrir la app.
 *
 * Esquema del JSON remoto:
 * {
 *   "v": 1,
 *   "instagram_reels":   ["clips_viewer", "clips_swipe_refresh"],
 *   "instagram_stories": ["reel_viewer", "story_viewer"],
 *   "youtube_shorts":    ["reel_watch_player", ...],
 *   "facebook_reels":    ["reels_viewer", ...],
 *   "tiktok_feed":       ["video_player_progress", ...]
 * }
 */
object HintConfig {
    private const val TAG = "ReelBlocker.Service"
    private const val PREFS = "reelblocker_prefs"
    private const val KEY_JSON = "hints_json"
    private const val KEY_FETCHED_MS = "hints_fetched_ms"

    /**
     * URL del JSON remoto. ALOJAR EL ARCHIVO Y SUSTITUIR ESTA URL por la real
     * (p.ej. un raw de GitHub: https://raw.githubusercontent.com/<user>/<repo>/main/hints.json).
     * Mientras contenga "CHANGEME" no se hace ninguna petición de red — la app
     * funciona con los defaults baked-in.
     */
    const val REMOTE_URL = "https://raw.githubusercontent.com/CHANGEME/basta-hints/main/hints.json"

    /** Cada cuánto se intenta refrescar el JSON (en éxito). */
    private const val FETCH_INTERVAL_MS = 12L * 60 * 60 * 1000 // 12 h
    private const val HTTP_TIMEOUT_MS = 8000
    private const val MAX_BODY_BYTES = 64 * 1024

    // ---- Defaults baked-in (el suelo que nunca puede romperse) ----

    val DEFAULT_INSTAGRAM_REELS = listOf(
        "clips_viewer",
        "clips_swipe_refresh"
    )
    val DEFAULT_INSTAGRAM_STORIES = listOf(
        "reel_viewer",
        "story_viewer"
    )
    val DEFAULT_YOUTUBE_SHORTS = listOf(
        "reel_watch_player",
        "reel_watch_fragment_root",
        "reel_player_page_container",
        "reel_recycler"
    )
    // Suelo vacío a propósito: Facebook usa Litho y aún no hay resource-ids
    // confirmados del visor de Reels. Sin hints, FB no bloquea (cero falsos
    // positivos). Bakear aquí los ids capturados en dispositivo, o empujarlos
    // por el JSON remoto bajo la clave "facebook_reels".
    val DEFAULT_FACEBOOK_REELS = emptyList<String>()
    // Firma del reproductor inmersivo vertical de TikTok (feed "Para ti" /
    // "Siguiendo" y el visor de vídeo). Estos ids semánticos aparecen SOLO con
    // un vídeo reproduciéndose a pantalla completa; NO en perfil, DMs, búsqueda
    // ni la rejilla de vídeos (esas usan listas/grids sin barra de progreso).
    // Capturado en dispositivo (com.zhiliaoapp.musically, 2026-05-29).
    val DEFAULT_TIKTOK_FEED = listOf(
        "video_player_progress",
        "feed_multi_tag_layout"
    )

    /** Resultado del parseo: las listas ya fundidas con sus defaults. */
    data class Parsed(
        val instagramReels: List<String>,
        val instagramStories: List<String>,
        val youtubeShorts: List<String>,
        val facebookReels: List<String>,
        val tiktokFeed: List<String>
    )

    private val DEFAULTS = Parsed(
        DEFAULT_INSTAGRAM_REELS,
        DEFAULT_INSTAGRAM_STORIES,
        DEFAULT_YOUTUBE_SHORTS,
        DEFAULT_FACEBOOK_REELS,
        DEFAULT_TIKTOK_FEED
    )

    // ---- Cache en memoria (servicio): se re-parsea solo si cambia el ts ----

    private var cachedAtMs = -1L
    private var cached: Parsed = DEFAULTS

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    private fun current(ctx: Context): Parsed {
        val p = prefs(ctx)
        val fetched = p.getLong(KEY_FETCHED_MS, 0L)
        if (fetched == cachedAtMs) return cached  // sin cambios: memo
        cachedAtMs = fetched
        cached = parse(p.getString(KEY_JSON, null))
        return cached
    }

    fun instagramReels(ctx: Context): List<String> = current(ctx).instagramReels
    fun instagramStories(ctx: Context): List<String> = current(ctx).instagramStories
    fun youtubeShorts(ctx: Context): List<String> = current(ctx).youtubeShorts
    fun facebookReels(ctx: Context): List<String> = current(ctx).facebookReels
    fun tiktokFeed(ctx: Context): List<String> = current(ctx).tiktokFeed

    /**
     * Parseo puro y a prueba de fallos. JSON nulo/vacío/corrupto → defaults.
     * Cada lista se funde con su default por unión (el remoto solo añade).
     * Aislado de Android para poder testearlo en JVM.
     */
    fun parse(raw: String?): Parsed {
        if (raw.isNullOrBlank()) return DEFAULTS
        return try {
            val o = JSONObject(raw)
            Parsed(
                merge(DEFAULT_INSTAGRAM_REELS, o.optJSONArray("instagram_reels")),
                merge(DEFAULT_INSTAGRAM_STORIES, o.optJSONArray("instagram_stories")),
                merge(DEFAULT_YOUTUBE_SHORTS, o.optJSONArray("youtube_shorts")),
                merge(DEFAULT_FACEBOOK_REELS, o.optJSONArray("facebook_reels")),
                merge(DEFAULT_TIKTOK_FEED, o.optJSONArray("tiktok_feed"))
            )
        } catch (_: Exception) {
            DEFAULTS
        }
    }

    private fun merge(defaults: List<String>, arr: JSONArray?): List<String> {
        // LinkedHashSet: defaults primero (orden estable), luego extras remotos
        // sin duplicar. Si el remoto viene vacío/nulo, quedan solo los defaults.
        val out = LinkedHashSet(defaults)
        if (arr != null) {
            for (i in 0 until arr.length()) {
                val s = arr.optString(i, "").trim()
                if (s.isNotEmpty()) out.add(s)
            }
        }
        return out.toList()
    }

    /**
     * Descarga el JSON remoto si toca (throttle FETCH_INTERVAL_MS) y lo cachea
     * en prefs. Silencioso ante cualquier fallo. Llamar desde el proceso
     * principal (tiene red); el servicio solo lee la cache.
     *
     * El timestamp solo se actualiza en éxito → si el servidor está caído se
     * reintenta en la siguiente apertura en vez de esperar 12 h.
     */
    fun maybeFetch(ctx: Context) {
        if (REMOTE_URL.contains("CHANGEME")) return  // URL sin configurar
        val p = prefs(ctx)
        val last = p.getLong(KEY_FETCHED_MS, 0L)
        if (System.currentTimeMillis() - last in 0 until FETCH_INTERVAL_MS) return

        Thread {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(REMOTE_URL).openConnection() as HttpURLConnection).apply {
                    connectTimeout = HTTP_TIMEOUT_MS
                    readTimeout = HTTP_TIMEOUT_MS
                    requestMethod = "GET"
                }
                if (conn.responseCode != 200) {
                    Log.d(TAG, "Fetch hints: HTTP ${conn.responseCode}")
                    return@Thread
                }
                val body = conn.inputStream.bufferedReader()
                    .use { it.readText() }
                    .take(MAX_BODY_BYTES)
                // Validar que es JSON válido antes de cachear. Si no parsea,
                // JSONObject lanza y no guardamos nada (seguimos con la cache
                // o los defaults).
                JSONObject(body)
                p.edit()
                    .putString(KEY_JSON, body)
                    .putLong(KEY_FETCHED_MS, System.currentTimeMillis())
                    .apply()
                Log.d(TAG, "Hints actualizados desde remoto (${body.length} bytes)")
            } catch (e: Exception) {
                Log.d(TAG, "Fetch hints falló: ${e.message}")
            } finally {
                conn?.disconnect()
            }
        }.apply { isDaemon = true }.start()
    }
}
