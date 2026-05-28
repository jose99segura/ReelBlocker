package app.reelblocker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests JVM puros de la heurística de "detección probablemente rota"
 * ([HealthCheck.shouldWarnBreakage]). Debe ser conservadora: nada de avisos
 * a quien nunca bloqueó (recién instalado) ni a quien simplemente dejó de ver
 * Reels, ni repetir el aviso dentro de la ventana de re-notificación.
 */
class HealthCheckLogicTest {

    private val day = 24L * 60 * 60 * 1000
    private val now = 1_000_000_000_000L

    @Test
    fun nunca_bloqueo_no_avisa() {
        assertFalse(
            HealthCheck.shouldWarnBreakage(
                everBlocked = false, lastBlockMs = 0L, lastNotifiedMs = 0L, nowMs = now
            )
        )
    }

    @Test
    fun bloqueo_reciente_no_avisa() {
        assertFalse(
            HealthCheck.shouldWarnBreakage(
                everBlocked = true,
                lastBlockMs = now - 2 * day, // hace 2 días, dentro de la ventana
                lastNotifiedMs = 0L,
                nowMs = now
            )
        )
    }

    @Test
    fun mucho_sin_bloquear_con_historial_avisa() {
        assertTrue(
            HealthCheck.shouldWarnBreakage(
                everBlocked = true,
                lastBlockMs = now - 10 * day, // > 7 días
                lastNotifiedMs = 0L,
                nowMs = now
            )
        )
    }

    @Test
    fun no_repite_dentro_de_la_ventana_de_renotificacion() {
        assertFalse(
            HealthCheck.shouldWarnBreakage(
                everBlocked = true,
                lastBlockMs = now - 10 * day,
                lastNotifiedMs = now - 2 * day, // ya avisado hace 2 días
                nowMs = now
            )
        )
    }

    @Test
    fun reavisa_pasada_la_ventana_de_renotificacion() {
        assertTrue(
            HealthCheck.shouldWarnBreakage(
                everBlocked = true,
                lastBlockMs = now - 30 * day,
                lastNotifiedMs = now - 10 * day, // último aviso hace > 7 días
                nowMs = now
            )
        )
    }
}
