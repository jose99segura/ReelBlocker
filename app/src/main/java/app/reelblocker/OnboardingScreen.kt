package app.reelblocker

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch

interface OnboardingActions {
    fun openAccessibility()
    fun requestBatteryExemption()
    fun isAccessibilityEnabled(): Boolean
    fun isBatteryExempt(): Boolean
}

private data class OnboardingPage(
    val title: String,
    val body: String,
    val statusContent: (@Composable (OnboardingActions, Boolean) -> Unit)? = null,
    val isGranted: ((OnboardingActions) -> Boolean)? = null
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    actions: OnboardingActions,
    onFinish: () -> Unit
) {
    // Refrescar cuando volvemos del Settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    var refreshKey by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val accessibilityOn = remember(refreshKey) { actions.isAccessibilityEnabled() }
    val batteryOk = remember(refreshKey) { actions.isBatteryExempt() }

    val pages = listOf(
        OnboardingPage(
            title = "Bienvenido a Basta",
            body = "Esta app vigila cuando entras en Reels de Instagram o Shorts de " +
                "YouTube y te saca antes de que pierdas el tiempo. No envia nada " +
                "fuera del dispositivo."
        ),
        OnboardingPage(
            title = "Activa el servicio de accesibilidad",
            body = "Es la unica forma en Android de detectar Reels. Solo lee " +
                "identificadores de pantalla de Instagram y YouTube para saber " +
                "cuando pulsar atras.",
            statusContent = { acts, granted ->
                if (granted) {
                    GrantedBadge("Servicio activado")
                } else {
                    Button(onClick = { acts.openAccessibility() }) {
                        Text("Abrir ajustes de accesibilidad")
                    }
                }
            },
            isGranted = { it.isAccessibilityEnabled() }
        ),
        OnboardingPage(
            title = "Excluye de la optimizacion de bateria",
            body = "Sin esto, el sistema puede matar el servicio al cabo de unas " +
                "horas y el bloqueo dejaria de funcionar.",
            statusContent = { acts, granted ->
                if (granted) {
                    GrantedBadge("Bateria: exenta")
                } else {
                    Button(onClick = { acts.requestBatteryExemption() }) {
                        Text("Excluir de la bateria")
                    }
                }
            },
            isGranted = { it.isBatteryExempt() }
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    // Auto-avanzar al conceder un permiso (excepto en la ultima pagina).
    LaunchedEffect(accessibilityOn, batteryOk) {
        val currentPage = pages.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        val granted = currentPage.isGranted?.invoke(actions) == true
        if (granted && pagerState.currentPage < pages.lastIndex) {
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) { Text("Saltar") }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) { pageIndex ->
            val page = pages[pageIndex]
            val granted = page.isGranted?.invoke(actions) == true

            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = page.title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = page.body,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (page.statusContent != null) {
                    Spacer(Modifier.height(32.dp))
                    // Forzamos recomposicion en refreshKey/granted
                    @Suppress("UNUSED_EXPRESSION") refreshKey
                    page.statusContent.invoke(actions, granted)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            pages.indices.forEach { i ->
                val selected = i == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (selected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                )
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (isLast) {
                    onFinish()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            }
        ) {
            Text(if (isLast) "Comenzar" else "Siguiente")
        }
    }
}

@Composable
private fun GrantedBadge(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color(0xFF2E7D32))
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF2E7D32)
        )
    }
}
