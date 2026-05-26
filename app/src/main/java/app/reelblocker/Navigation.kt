package app.reelblocker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Cuatro destinos top-level navegados vía bottom navigation bar. La
 * transición entre tabs es un cross-fade simple — no hay dirección
 * espacial porque las pestañas no son una jerarquía lineal.
 */
sealed class Screen {
    data object Home : Screen()
    data object Stats : Screen()
    data object Inventory : Screen()
    data object Settings : Screen()

    companion object {
        /**
         * Orden en la barra inferior. Construido on-demand para evitar el
         * bug de inicialización estática de Kotlin con `data object`s dentro
         * de sealed class — si se evalúa eagerly desde el companion init,
         * los singletons pueden no estar todavía cargados y la lista acaba
         * con nulls, lo que rompe el `when` exhaustivo del BottomNavBar.
         */
        val bottomTabs: List<Screen>
            get() = listOf(Home, Stats, Inventory, Settings)
    }
}

@Composable
fun ScreenContainer(
    current: Screen,
    modifier: Modifier = Modifier,
    content: @Composable (Screen) -> Unit
) {
    AnimatedContent(
        targetState = current,
        modifier = modifier,
        transitionSpec = {
            fadeIn(tween(180)).togetherWith(fadeOut(tween(140)))
        },
        label = "screen-nav"
    ) { screen ->
        content(screen)
    }
}
