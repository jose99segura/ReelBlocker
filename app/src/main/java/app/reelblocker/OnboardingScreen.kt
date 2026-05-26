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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch

interface OnboardingActions {
    fun openAccessibility()
    fun requestBatteryExemption()
    fun isAccessibilityEnabled(): Boolean
    fun isBatteryExempt(): Boolean
}

/**
 * Una página del onboarding. Tres tipos via campos opcionales:
 *  - StatPage: si bigNumber != null → muestra dato grande + frase debajo.
 *  - WordmarkPage: si isWordmark == true → muestra "Basta!" gigante.
 *  - ActionPage: si statusContent != null → CTA + estado del permiso.
 *  - Plain: solo title + body (no usado actualmente).
 */
private data class OnboardingPage(
    val title: String,
    val body: String,
    val bigNumber: String? = null,
    val bigNumberLabel: String? = null,
    val source: String? = null,
    val isWordmark: Boolean = false,
    val statusContent: (@Composable (OnboardingActions, Boolean) -> Unit)? = null,
    val isGranted: ((OnboardingActions) -> Boolean)? = null,
    /** Contenido visual custom que se renderiza ENTRE el título y el body. */
    val customContent: (@Composable () -> Unit)? = null
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    actions: OnboardingActions,
    onFinish: () -> Unit
) {
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

    val p3Granted = stringResource(R.string.onboarding_p3_granted)
    val p3Cta = stringResource(R.string.onboarding_p3_cta)
    val p4Granted = stringResource(R.string.onboarding_p4_granted)
    val p4Cta = stringResource(R.string.onboarding_p4_cta)
    val pages = listOf(
        OnboardingPage(
            title = stringResource(R.string.onboarding_p1_title),
            body = stringResource(R.string.onboarding_p1_body),
            isWordmark = true
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_p2_title),
            body = stringResource(R.string.onboarding_p2_body),
            customContent = { MascotIntroVisual() }
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_p3_title),
            body = stringResource(R.string.onboarding_p3_body),
            statusContent = { acts, granted ->
                if (granted) GrantedBadge(p3Granted)
                else Button(onClick = { acts.openAccessibility() }) {
                    Text(p3Cta)
                }
            },
            isGranted = { it.isAccessibilityEnabled() }
        ),
        OnboardingPage(
            title = stringResource(R.string.onboarding_p4_title),
            body = stringResource(R.string.onboarding_p4_body),
            statusContent = { acts, granted ->
                if (granted) GrantedBadge(p4Granted)
                else Button(onClick = { acts.requestBatteryExemption() }) {
                    Text(p4Cta)
                }
            },
            isGranted = { it.isBatteryExempt() }
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLast = pagerState.currentPage == pages.lastIndex

    LaunchedEffect(accessibilityOn, batteryOk) {
        val current = pages.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        val granted = current.isGranted?.invoke(actions) == true
        if (granted && pagerState.currentPage < pages.lastIndex) {
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onFinish) { Text(stringResource(R.string.onboarding_skip)) }
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
                if (page.isWordmark) {
                    // Icono de la marca, grande y centrado.
                    BastaIconBadge(size = 96.dp)
                    Spacer(Modifier.height(24.dp))
                }

                Text(
                    text = page.title,
                    fontSize = if (page.isWordmark) 80.sp else 22.sp,
                    fontWeight = if (page.isWordmark) FontWeight.Black else FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = if (page.isWordmark) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                    lineHeight = if (page.isWordmark) 80.sp else androidx.compose.ui.unit.TextUnit.Unspecified
                )

                if (page.isWordmark) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.app_tagline).uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.3.em,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(28.dp))
                } else {
                    Spacer(Modifier.height(16.dp))
                }

                if (page.customContent != null) {
                    page.customContent.invoke()
                    Spacer(Modifier.height(20.dp))
                }

                Text(
                    text = if (page.isWordmark) highlightStats(page.body) else buildAnnotatedString { append(page.body) },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (page.source != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.onboarding_source_prefix, page.source),
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                if (page.statusContent != null) {
                    Spacer(Modifier.height(32.dp))
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
                if (isLast) onFinish()
                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
            }
        ) {
            Text(if (isLast) stringResource(R.string.onboarding_start) else stringResource(R.string.onboarding_next))
        }
    }
}

/**
 * Visual de presentación de la mascota: huevo grande arriba + fila pequeña
 * de evoluciones con flechas, para que el usuario vea de un vistazo que
 * hay progresión.
 */
@Composable
private fun MascotIntroVisual() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Huevo principal — animado (respiración).
        MascotCanvas(
            level = MascotLevel.EGG,
            animate = true,
            modifier = Modifier.size(140.dp)
        )
        Spacer(Modifier.height(16.dp))

        // Previsualización de evolución: huevo → se agrieta → cría → juvenil → adulto.
        // Termina en ADULT que es el punto de graduación (día 30).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            EvolutionPreview(level = MascotLevel.EGG)
            EvolutionArrow()
            EvolutionPreview(level = MascotLevel.CRACKING)
            EvolutionArrow()
            EvolutionPreview(level = MascotLevel.HATCHLING)
            EvolutionArrow()
            EvolutionPreview(level = MascotLevel.JUVENILE)
            EvolutionArrow()
            EvolutionPreview(level = MascotLevel.ADULT)
        }
    }
}

@Composable
private fun EvolutionPreview(level: MascotLevel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MascotCanvas(
            level = level,
            animate = false,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = when (level) {
                MascotLevel.EGG -> stringResource(R.string.onboarding_day_label, 0)
                MascotLevel.CRACKING -> stringResource(R.string.onboarding_day_label, 3)
                MascotLevel.HATCHLING -> stringResource(R.string.onboarding_day_label, 7)
                MascotLevel.JUVENILE -> stringResource(R.string.onboarding_day_label, 14)
                MascotLevel.ADULT -> stringResource(R.string.onboarding_day_label, 30)
                else -> ""
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EvolutionArrow() {
    Text(
        text = "→",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    )
}

@Composable
private fun BastaIconBadge(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.22f))
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        // Mismo diseno que ic_launcher_foreground.xml: "!" blanco con
        // punto rojo. Viewport 108x108 escalado al tamano del badge.
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = this.size.width // asumimos cuadrado
            fun u(v: Float) = v / 108f * s

            // Barra vertical del "!"
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(u(47f), u(24f)),
                size = Size(u(14f), u(38f)),
                cornerRadius = CornerRadius(u(2.5f), u(2.5f))
            )
            // Punto del "!": circulo rojo limpio
            drawCircle(
                color = Color(0xFFDC2626),
                radius = u(7f),
                center = Offset(u(54f), u(73f))
            )
        }
    }
}

/** Resalta digitos + "min" en rojo para impacto visual. */
private fun highlightStats(text: String): AnnotatedString {
    val regex = Regex("""\d+\s*min(?:/dia)?""")
    return androidx.compose.ui.text.buildAnnotatedString {
        var lastEnd = 0
        regex.findAll(text).forEach { match ->
            append(text.substring(lastEnd, match.range.first))
            withStyle(
                SpanStyle(
                    color = Color(0xFFDC2626),
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(match.value)
            }
            lastEnd = match.range.last + 1
        }
        append(text.substring(lastEnd))
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
