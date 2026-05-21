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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val title: String,
    val body: String,
    val ctaLabel: String?,
    val onCta: ((OnboardingActions) -> Unit)?
)

interface OnboardingActions {
    fun openAccessibility()
    fun requestBatteryExemption()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    actions: OnboardingActions,
    onFinish: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            title = "Bienvenido a ReelBlocker",
            body = "Esta app vigila cuando entras en Reels de Instagram o Shorts de " +
                "YouTube y te saca antes de que pierdas el tiempo. No envia nada " +
                "fuera del dispositivo.",
            ctaLabel = null,
            onCta = null
        ),
        OnboardingPage(
            title = "Activa el servicio de accesibilidad",
            body = "Es la unica forma en Android de detectar que entras en Reels. " +
                "Solo lee identificadores de pantalla de Instagram y YouTube para " +
                "saber cuando pulsar atras. Nada mas.",
            ctaLabel = "Abrir ajustes de accesibilidad",
            onCta = { it.openAccessibility() }
        ),
        OnboardingPage(
            title = "Excluye de la optimizacion de bateria",
            body = "Sin esto, el sistema puede matar el servicio al cabo de unas " +
                "horas y el bloqueo dejaria de funcionar.",
            ctaLabel = "Excluir de la bateria",
            onCta = { it.requestBatteryExemption() }
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) { Text("Saltar") }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
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
                if (page.ctaLabel != null && page.onCta != null) {
                    Spacer(Modifier.height(32.dp))
                    Button(onClick = { page.onCta.invoke(actions) }) {
                        Text(page.ctaLabel)
                    }
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
