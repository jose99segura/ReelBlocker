package app.reelblocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Contrato de [Stats.canonicalPackage]: que todo fork de YouTube caiga en el
 * paquete canonico, y — mas importante — que NO caiga nada que no sea YouTube.
 *
 * El riesgo real esta en el falso positivo: el bloqueo total Pro dispara HOME
 * en cualquier evento del paquete, asi que meter YouTube Music o Kids aqui
 * cerraria de golpe una app que el usuario nunca pidio bloquear.
 */
class PackageCanonicalizationTest {

    @Test
    fun youtube_oficial_es_su_propio_canonico() {
        assertEquals(Stats.PKG_YOUTUBE, Stats.canonicalPackage("com.google.android.youtube"))
    }

    @Test
    fun forks_conocidos_caen_en_youtube() {
        listOf(
            "app.revanced.android.youtube",   // ReVanced non-root
            "app.rvx.android.youtube",        // ReVanced Extended
            "com.vanced.android.youtube"      // Vanced (legacy)
        ).forEach {
            assertEquals("fork no canonicalizado: $it", Stats.PKG_YOUTUBE, Stats.canonicalPackage(it))
        }
    }

    @Test
    fun fork_renombrado_a_mano_cae_por_heuristica() {
        // Builds propias que no estan en la lista explicita: el segmento
        // "youtube" basta.
        assertEquals(Stats.PKG_YOUTUBE, Stats.canonicalPackage("com.miapp.youtube.custom"))
    }

    @Test
    fun apps_hermanas_de_google_NO_son_youtube() {
        listOf(
            "com.google.android.apps.youtube.music",      // YouTube Music
            "com.google.android.apps.youtube.kids",       // YouTube Kids
            "com.google.android.apps.youtube.creator",    // YouTube Studio
            "com.google.android.apps.youtube.unplugged",  // YouTube TV
            "app.revanced.android.apps.youtube.music"     // ReVanced Music
        ).forEach {
            assertNotEquals("falso positivo peligroso: $it", Stats.PKG_YOUTUBE, Stats.canonicalPackage(it))
        }
    }

    @Test
    fun otras_apps_pasan_intactas() {
        listOf(
            Stats.PKG_INSTAGRAM,
            Stats.PKG_TIKTOK,
            Stats.PKG_FACEBOOK,
            "com.whatsapp",
            "com.google.android.gms"   // contiene "go" pero no "youtube"
        ).forEach {
            assertEquals(it, Stats.canonicalPackage(it))
        }
    }

    @Test
    fun familia_de_youtube_incluye_los_forks_y_las_demas_apps_solo_a_si_mismas() {
        val family = Stats.packageFamily(Stats.PKG_YOUTUBE)
        assertEquals(4, family.size)
        assertEquals(Stats.PKG_YOUTUBE, family.first())
        assertEquals(listOf(Stats.PKG_INSTAGRAM), Stats.packageFamily(Stats.PKG_INSTAGRAM))
    }
}
