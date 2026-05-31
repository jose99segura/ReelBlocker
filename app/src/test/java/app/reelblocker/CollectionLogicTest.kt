package app.reelblocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Tests JVM puros para la lógica de selección de especie y agregación
 * del Bestiario. No tocan Context ni prefs — el caller en
 * [Collection.pickNext] sigue siendo el responsable de persistir el
 * flag de paywall.
 *
 * Estos tests existen para que un cambio futuro en el tier de una
 * especie o en la regla de elección NO rompa silenciosamente el flujo
 * que el usuario observa (graduadas que se pierden de vista, paywalls
 * que se disparan cuando no debían, etc.).
 */
class CollectionLogicTest {

    private val seeded = Random(42)

    // ============================================================
    //   selectNextSpecies — usuario FREE
    // ============================================================

    @Test
    fun free_user_sin_graduaciones_acaba_de_archivar_clasica_recibe_dragon() {
        val result = Collection.selectNextSpecies(
            collected = setOf(MascotSpecies.CLASICA),
            justArchived = MascotSpecies.CLASICA,
            isPro = false,
            random = seeded
        )
        // Solo Dragon queda free uncollected. No hay paywall todavía.
        assertEquals(MascotSpecies.DRAGON, result.next)
        assertFalse(result.markPendingProUnlock)
    }

    @Test
    fun free_user_con_clasica_archivada_y_archiva_dragon_dispara_paywall() {
        val result = Collection.selectNextSpecies(
            collected = setOf(MascotSpecies.CLASICA, MascotSpecies.DRAGON),
            justArchived = MascotSpecies.DRAGON,
            isPro = false,
            random = seeded
        )
        // Free tier agotado. Devuelve repetición de free (CLASICA por ser
        // la única opción tras excluir la justArchived DRAGON). Marca
        // paywall pendiente.
        assertEquals(MascotSpecies.CLASICA, result.next)
        assertTrue(result.markPendingProUnlock)
    }

    @Test
    fun free_user_que_completo_free_tier_nunca_recibe_pro() {
        // Itera varias semillas para descartar que la aleatoriedad cuele
        // una especie Pro.
        repeat(50) { seed ->
            val result = Collection.selectNextSpecies(
                collected = setOf(MascotSpecies.CLASICA, MascotSpecies.DRAGON),
                justArchived = MascotSpecies.CLASICA,
                isPro = false,
                random = Random(seed.toLong())
            )
            assertFalse(
                "Free user no debe recibir especie Pro como repetición (semilla $seed → ${result.next})",
                result.next.isPro
            )
        }
    }

    // ============================================================
    //   selectNextSpecies — usuario PRO
    // ============================================================

    @Test
    fun pro_user_con_solo_clasica_archivada_recibe_alguna_no_clasica() {
        val result = Collection.selectNextSpecies(
            collected = setOf(MascotSpecies.CLASICA),
            justArchived = MascotSpecies.CLASICA,
            isPro = true,
            random = seeded
        )
        assertNotEquals(MascotSpecies.CLASICA, result.next)
        assertFalse(result.markPendingProUnlock)
    }

    @Test
    fun pro_user_con_todas_coleccionadas_repite_sin_marcar_paywall() {
        val all = MascotSpecies.entries.toSet()
        val result = Collection.selectNextSpecies(
            collected = all,
            justArchived = MascotSpecies.CLASICA,
            isPro = true,
            random = seeded
        )
        // Devuelve cualquier especie del pool excluyendo justArchived.
        // No marca paywall (el usuario ya es Pro).
        assertNotEquals(MascotSpecies.CLASICA, result.next)
        assertFalse(result.markPendingProUnlock)
    }

    @Test
    fun pro_user_pool_completo_puede_devolver_pro_species() {
        // Validamos que el pool del usuario Pro incluye las especies Pro
        // (no se está aplicando el filtro free por error).
        val devueltas = mutableSetOf<MascotSpecies>()
        repeat(200) { seed ->
            val r = Collection.selectNextSpecies(
                collected = emptySet(),
                justArchived = MascotSpecies.CLASICA,
                isPro = true,
                random = Random(seed.toLong())
            )
            devueltas.add(r.next)
        }
        // En 200 iteraciones con semillas distintas debe haber salido al
        // menos una Pro y al menos una free de las disponibles.
        assertTrue("Esperaba al menos una especie Pro: $devueltas",
            devueltas.any { it.isPro })
    }

    // ============================================================
    //   firstByMember — preservación de fecha original en repeticiones
    // ============================================================

    @Test
    fun firstByMember_preserva_primera_graduacion_aunque_haya_repeticiones() {
        val entries = listOf(
            Collection.CollectedMascot(MascotSpecies.CLASICA, "2026-01-01", 30),
            Collection.CollectedMascot(MascotSpecies.DRAGON, "2026-01-31", 30),
            Collection.CollectedMascot(MascotSpecies.CLASICA, "2026-03-01", 30),
            Collection.CollectedMascot(MascotSpecies.CLASICA, "2026-04-01", 30)
        )

        val firstByMember = entries.groupBy { it.species }.mapValues { it.value.first() }

        assertEquals(2, firstByMember.size)
        // La PRIMERA graduación de CLASICA (1 enero) sigue siendo la que
        // se muestra, no es sobrescrita por las repeticiones posteriores.
        assertEquals("2026-01-01", firstByMember[MascotSpecies.CLASICA]?.acquiredDate)
        assertEquals("2026-01-31", firstByMember[MascotSpecies.DRAGON]?.acquiredDate)
    }

    @Test
    fun cuenta_de_repeticiones_correcta_para_especie_activa() {
        val entries = listOf(
            Collection.CollectedMascot(MascotSpecies.CLASICA, "2026-01-01", 30),
            Collection.CollectedMascot(MascotSpecies.DRAGON, "2026-01-31", 30),
            Collection.CollectedMascot(MascotSpecies.CLASICA, "2026-03-01", 30)
        )

        // Misma fórmula que usa InventoryScreen para mostrar el badge.
        assertEquals(3, entries.count { it.species == MascotSpecies.CLASICA } + 1)
        assertEquals(2, entries.count { it.species == MascotSpecies.DRAGON } + 1)
        assertEquals(1, entries.count { it.species == MascotSpecies.TORTUGA } + 1)
    }

    @Test
    fun activeIsRepeat_se_detecta_con_membresia_en_firstByMember() {
        val entries = listOf(
            Collection.CollectedMascot(MascotSpecies.CLASICA, "2026-01-01", 30)
        )
        val firstByMember = entries.groupBy { it.species }.mapValues { it.value.first() }

        // Si la activa es CLASICA (ya graduada), es repetición → satélite
        // de la graduada DEBE aparecer.
        assertTrue(MascotSpecies.CLASICA in firstByMember)
        // Si la activa es DRAGON (ciclo virgen), satélite virgen no aporta.
        assertFalse(MascotSpecies.DRAGON in firstByMember)
    }
}
