package app.reelblocker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

/**
 * Hero principal — sin card chrome. Mascota posada sobre una isla flotante
 * (sombra de luz suave). Tipografia como unica estructura.
 *
 * Tap en la mascota: bounce + wobble + haptic. NO navega.
 */
@Composable
fun StreakCard(
    refreshKey: Int,
    serviceEnabled: Boolean,
    breakRemainingMs: Long? = null,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val state = remember(refreshKey) { Streak.current(ctx) }
    val species = remember(refreshKey) { Collection.currentSpecies(ctx) }
    val haptic = LocalHapticFeedback.current
    val isDark = isSystemInDarkTheme()

    var celebrating by remember { mutableStateOf(false) }
    LaunchedEffect(refreshKey) {
        val from = Streak.consumePendingEvolution(ctx)
        if (from != null && from.ordinal < state.level.ordinal) {
            celebrating = true
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(2500)
            celebrating = false
        }
    }

    // Trigger del bounce — un contador que la mascota observa.
    var tapTrigger by remember { mutableIntStateOf(0) }

    // Auto-wiggle de bienvenida: invita al usuario a tocar la mascota la
    // primera vez que la ve. Se dispara una vez por sesión si la racha es ≤ 1.
    var autoHintFired by remember { mutableStateOf(false) }
    LaunchedEffect(state.count) {
        if (state.count <= 1 && !autoHintFired) {
            delay(1800)
            tapTrigger++
            autoHintFired = true
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Nivel — micro-label arriba, accent color, letter-spaced.
            Text(
                text = stringResource(state.level.displayNameRes).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = state.level.accentColor,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(4.dp))

            // Mascota dentro del anillo, posada sobre la isla flotante.
            // El tap aqui dispara solo la animacion de bounce.
            MascotWithRing(
                level = state.level,
                species = species,
                progress = state.progressInLevel,
                animate = state.count > 0 && serviceEnabled,
                isDark = isDark,
                tapTrigger = tapTrigger,
                onTap = {
                    tapTrigger++
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            )

            Spacer(Modifier.height(8.dp))

            // Numero gigante (rolodex animation).
            AnimatedContent(
                targetState = state.count,
                transitionSpec = {
                    (slideInVertically { -it } + fadeIn())
                        .togetherWith(slideOutVertically { it } + fadeOut())
                },
                label = "streak-count"
            ) { count ->
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = count.toString(),
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-3).sp,
                        lineHeight = 80.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = pluralStringResource(R.plurals.plural_days, count),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            val graduationGoal = MascotLevel.ADULT.minDays
            val subtext: String
            val subtextStyle: androidx.compose.ui.text.TextStyle
            val subtextColor: androidx.compose.ui.graphics.Color
            when {
                breakRemainingMs != null -> {
                    // Estamos en pausa Pro — sustituye el subtexto por countdown.
                    val totalSecs = breakRemainingMs / 1000
                    val mm = totalSecs / 60
                    val ss = totalSecs % 60
                    val mmss = String.format("%d:%02d", mm, ss)
                    subtext = stringResource(R.string.break_home_label, mmss)
                    subtextStyle = MaterialTheme.typography.bodyMedium
                    subtextColor = MaterialTheme.colorScheme.primary
                }
                state.count == 0 && !serviceEnabled -> {
                    subtext = stringResource(R.string.streak_state_service_off)
                    subtextStyle = MaterialTheme.typography.bodyMedium
                    subtextColor = MaterialTheme.colorScheme.onSurfaceVariant
                }
                state.count == 0 -> {
                    subtext = stringResource(R.string.streak_state_new)
                    subtextStyle = MaterialTheme.typography.bodyMedium
                    subtextColor = MaterialTheme.colorScheme.onSurfaceVariant
                }
                state.count >= graduationGoal -> {
                    subtext = stringResource(R.string.streak_state_ready_to_graduate)
                    subtextStyle = MaterialTheme.typography.bodyMedium
                    subtextColor = species.accentTint
                }
                else -> {
                    // Copy discreto y enigmático — no menciona la especie, ni
                    // "guardar", ni el inventario. Solo deja una pista de que
                    // algo está pasando, pequeño y bajo.
                    val remaining = graduationGoal - state.count
                    subtext = if (remaining == 1)
                        stringResource(R.string.streak_state_mystery_one)
                    else
                        stringResource(R.string.streak_state_mystery_many, remaining)
                    subtextStyle = MaterialTheme.typography.labelSmall
                    subtextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                }
            }
            Text(
                text = subtext,
                style = subtextStyle,
                color = subtextColor,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp
            )
        }

        // Confetti overlay durante la evolucion.
        if (celebrating) {
            KonfettiView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                parties = listOf(
                    Party(
                        speed = 0f,
                        maxSpeed = 30f,
                        damping = 0.9f,
                        spread = 360,
                        colors = listOf(
                            state.level.gradientStart.toArgb(),
                            state.level.gradientEnd.toArgb(),
                            state.level.accentColor.toArgb(),
                            Color(0xFFFCD34D).toArgb()
                        ),
                        emitter = Emitter(duration = 200, TimeUnit.MILLISECONDS).max(80),
                        position = Position.Relative(0.5, 0.3)
                    )
                )
            )
        }
    }
}

@Composable
private fun MascotWithRing(
    level: MascotLevel,
    species: MascotSpecies,
    progress: Float,
    animate: Boolean,
    isDark: Boolean,
    tapTrigger: Int,
    onTap: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "ring-progress"
    )

    // Animacion de bounce / wobble — solo afecta a la mascota interna.
    val bounceScale = remember { Animatable(1f) }
    val bounceRotation = remember { Animatable(0f) }
    LaunchedEffect(tapTrigger) {
        if (tapTrigger == 0) return@LaunchedEffect
        launch {
            bounceScale.animateTo(0.88f, tween(80, easing = FastOutSlowInEasing))
            bounceScale.animateTo(1.12f, tween(110, easing = FastOutSlowInEasing))
            bounceScale.animateTo(
                1f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        launch {
            bounceRotation.animateTo(6f, tween(100, easing = FastOutSlowInEasing))
            bounceRotation.animateTo(-6f, tween(140, easing = FastOutSlowInEasing))
            bounceRotation.animateTo(
                0f,
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val mascotTapDescription = stringResource(R.string.cd_mascot_tap)
    Box(
        modifier = Modifier.size(width = 260.dp, height = 280.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Isla flotante — elipse difusa pegada a la base del anillo.
        Canvas(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(width = 200.dp, height = 36.dp)
        ) {
            val intensity = if (isDark) 0.38f else 0.28f
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        level.accentColor.copy(alpha = intensity),
                        level.gradientEnd.copy(alpha = intensity * 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.width / 2f
                )
            )
        }

        // Anillo + mascota apilados sobre la isla.
        Box(
            modifier = Modifier.size(260.dp),
            contentAlignment = Alignment.Center
        ) {
            // Anillo delicado — sin track de fondo prominente, solo el arco.
            // El anillo NO se anima con el bounce (queda como marco estable).
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 8.dp.toPx()
                val inset = strokeWidth / 2f + 4.dp.toPx()

                drawArc(
                    color = Color.Black.copy(alpha = 0.06f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - 2 * inset, size.height - 2 * inset),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            level.gradientStart,
                            level.accentColor,
                            level.gradientEnd,
                            level.gradientStart
                        )
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - 2 * inset, size.height - 2 * inset),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // La mascota — clickeable, anima con bounce/wobble.
            AnimatedContent(
                targetState = level,
                transitionSpec = {
                    (scaleIn(initialScale = 0.3f, animationSpec = tween(500)) + fadeIn())
                        .togetherWith(fadeOut(animationSpec = tween(150)))
                },
                label = "mascot-evolution"
            ) { currentLevel ->
                MascotCanvas(
                    level = currentLevel,
                    species = species,
                    animate = animate,
                    modifier = Modifier
                        .size(220.dp)
                        .graphicsLayer {
                            scaleX = bounceScale.value
                            scaleY = bounceScale.value
                            rotationZ = bounceRotation.value
                        }
                        .semantics {
                            role = Role.Button
                            contentDescription = mascotTapDescription
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onTap
                        )
                )
            }
        }
    }
}

internal fun Color.toArgb(): Int {
    val a = (alpha * 255).toInt() and 0xFF
    val r = (red * 255).toInt() and 0xFF
    val g = (green * 255).toInt() and 0xFF
    val b = (blue * 255).toInt() and 0xFF
    return (a shl 24) or (r shl 16) or (g shl 8) or b
}
