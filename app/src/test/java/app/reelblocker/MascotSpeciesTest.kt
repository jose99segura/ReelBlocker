package app.reelblocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica el contrato de tiers Free/Pro de [MascotSpecies]. Es el
 * fundamento del gating de la colección — si alguien cambia el tier de
 * una especie sin querer, la economía free se rompe.
 */
class MascotSpeciesTest {

    @Test
    fun freeSpecies_contiene_exactamente_clasica_y_dragon() {
        val free = MascotSpecies.freeSpecies()
        assertEquals(2, free.size)
        assertTrue(MascotSpecies.CLASICA in free)
        assertTrue(MascotSpecies.DRAGON in free)
    }

    @Test
    fun proSpecies_contiene_exactamente_tortuga_lobo_buho() {
        val pro = MascotSpecies.proSpecies()
        assertEquals(3, pro.size)
        assertTrue(MascotSpecies.TORTUGA in pro)
        assertTrue(MascotSpecies.LOBO in pro)
        assertTrue(MascotSpecies.BUHO in pro)
    }

    @Test
    fun isPro_coincide_con_la_division_de_tiers() {
        assertFalse(MascotSpecies.CLASICA.isPro)
        assertFalse(MascotSpecies.DRAGON.isPro)
        assertTrue(MascotSpecies.TORTUGA.isPro)
        assertTrue(MascotSpecies.LOBO.isPro)
        assertTrue(MascotSpecies.BUHO.isPro)
    }

    @Test
    fun cubrimos_las_5_especies() {
        val total = MascotSpecies.freeSpecies().size + MascotSpecies.proSpecies().size
        assertEquals(MascotSpecies.entries.size, total)
        assertEquals(5, MascotSpecies.entries.size)
    }
}
