package app.reelblocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests JVM puros de [HintConfig.parse]. Garantía central: un JSON remoto
 * corrupto, vacío o malicioso NUNCA debe poder desactivar la detección —
 * siempre se cae a los defaults baked-in, y el remoto solo puede AÑADIR hints.
 *
 * Usa el org.json real (testImplementation) en vez del stub de android.jar.
 */
class HintConfigTest {

    @Test
    fun json_nulo_devuelve_defaults() {
        val p = HintConfig.parse(null)
        assertEquals(HintConfig.DEFAULT_INSTAGRAM_REELS, p.instagramReels)
        assertEquals(HintConfig.DEFAULT_INSTAGRAM_STORIES, p.instagramStories)
        assertEquals(HintConfig.DEFAULT_YOUTUBE_SHORTS, p.youtubeShorts)
    }

    @Test
    fun json_vacio_o_blanco_devuelve_defaults() {
        assertEquals(HintConfig.DEFAULT_INSTAGRAM_REELS, HintConfig.parse("").instagramReels)
        assertEquals(HintConfig.DEFAULT_INSTAGRAM_REELS, HintConfig.parse("   ").instagramReels)
    }

    @Test
    fun json_corrupto_devuelve_defaults() {
        val p = HintConfig.parse("{ esto no es json valido ")
        assertEquals(HintConfig.DEFAULT_INSTAGRAM_REELS, p.instagramReels)
        assertEquals(HintConfig.DEFAULT_YOUTUBE_SHORTS, p.youtubeShorts)
    }

    @Test
    fun lista_remota_vacia_conserva_defaults() {
        val p = HintConfig.parse("""{ "v":1, "instagram_reels": [] }""")
        assertEquals(HintConfig.DEFAULT_INSTAGRAM_REELS, p.instagramReels)
    }

    @Test
    fun remoto_solo_anade_no_reemplaza() {
        val p = HintConfig.parse(
            """{ "v":1, "instagram_reels": ["clips_viewer", "nuevo_hint_reels"] }"""
        )
        // Defaults siguen presentes, en orden, + el nuevo al final.
        assertTrue(p.instagramReels.containsAll(HintConfig.DEFAULT_INSTAGRAM_REELS))
        assertTrue(p.instagramReels.contains("nuevo_hint_reels"))
        // clips_viewer (que ya era default) no se duplica.
        assertEquals(1, p.instagramReels.count { it == "clips_viewer" })
    }

    @Test
    fun entradas_vacias_o_blancas_se_descartan() {
        val p = HintConfig.parse(
            """{ "youtube_shorts": ["", "  ", "shorts_extra"] }"""
        )
        assertTrue(p.youtubeShorts.contains("shorts_extra"))
        assertTrue(p.youtubeShorts.none { it.isBlank() })
    }

    @Test
    fun claves_ausentes_usan_su_default() {
        // Solo viene youtube; ig_reels e ig_stories deben quedar en defaults.
        val p = HintConfig.parse("""{ "youtube_shorts": ["x"] }""")
        assertEquals(HintConfig.DEFAULT_INSTAGRAM_REELS, p.instagramReels)
        assertEquals(HintConfig.DEFAULT_INSTAGRAM_STORIES, p.instagramStories)
        assertTrue(p.youtubeShorts.contains("x"))
    }
}
