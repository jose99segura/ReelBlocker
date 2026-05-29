package app.reelblocker

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
                .padding(top = 12.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Nivel — micro-label arriba, accent color, letter-spaced.
            Text(
                text = stringResource(state.level.displayNameRes).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = state.level.accentColor,
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(6.dp))

            // Mascota dentro del anillo, posada sobre la isla flotante.
            // El tap aqui dispara solo la animacion de bounce.
            MascotWithRing(
                level = state.level,
                species = species,
                days = state.count,
                goal = MascotLevel.ADULT.minDays,
                animate = state.count > 0 && serviceEnabled,
                tapTrigger = tapTrigger,
                onTap = {
                    tapTrigger++
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            )

            Spacer(Modifier.height(4.dp))

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
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-3).sp,
                        lineHeight = 64.sp
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

            Spacer(Modifier.height(6.dp))

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
    days: Int,
    goal: Int,
    animate: Boolean,
    tapTrigger: Int,
    onTap: () -> Unit
) {

    // Levitación mística — la mascota flota por encima del nido,
    // oscilando suavemente, con un halo que palpita detrás.
    val mysticTransition = rememberInfiniteTransition(label = "mystic")
    val floatY by mysticTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float-y"
    )
    val haloPulse by mysticTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo-pulse"
    )

    // Sin lift: la mascota se posa dentro del nido (la oscilación sigue dando vida).
    val lift = 0.dp

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
        modifier = Modifier.size(width = 308.dp, height = 308.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // 1) Anillo segmentado — un puntito por día hasta la graduación (goal = MascotLevel.ADULT.minDays).
        //    Los días completados se encienden con el accent color de la especie;
        //    los pendientes quedan como track tenue. Feedback claro desde día 1.
        val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
        val litColor = level.accentColor
        val segments = goal
        val litCount = days.coerceIn(0, segments)
        val gapDeg = 3f
        val segDeg = (360f / segments) - gapDeg
        // El offset Y compensa que el dibujo de la mascota deja huecos en la
        // parte superior del Canvas; sin esto el ring queda visualmente alto.
        Canvas(
            modifier = Modifier
                .size(308.dp)
                .align(Alignment.Center)
                .graphicsLayer { translationY = 18.dp.toPx() }
        ) {
            val strokeWidth = 7.dp.toPx()
            val inset = strokeWidth / 2f + 4.dp.toPx()
            val arcSize = Size(size.width - 2 * inset, size.height - 2 * inset)
            val topLeft = Offset(inset, inset)
            for (i in 0 until segments) {
                val startAngle = -90f + i * (360f / segments) + gapDeg / 2f
                drawArc(
                    color = if (i < litCount) litColor else trackColor,
                    startAngle = startAngle,
                    sweepAngle = segDeg,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // 2) Isla flotante — pedestal achatado, delante del anillo.
        //    Padding inferior para que el ring tenga margen por debajo.
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(R.drawable.pedestal_island),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .size(width = 180.dp, height = 120.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.FillBounds
        )

        // 3) Halo + mascota delante de la isla (el huevo se posa sobre el nido).
        //    Padding superior para bajar la mascota dentro del anillo.
        Box(
            modifier = Modifier
                .padding(top = 28.dp)
                .size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            // Halo místico detrás de la mascota — palpita y flota con ella.
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        translationY = floatY.dp.toPx() - lift.toPx()
                    }
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            level.accentColor.copy(alpha = 0.42f * haloPulse),
                            level.gradientEnd.copy(alpha = 0.18f * haloPulse),
                            Color.Transparent
                        ),
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = size.minDimension / 2f
                    ),
                    radius = size.minDimension / 2f,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }

            // La mascota — clickeable, anima con bounce/wobble + flotación mística.
            AnimatedContent(
                targetState = level,
                transitionSpec = {
                    (scaleIn(initialScale = 0.3f, animationSpec = tween(500)) + fadeIn())
                        .togetherWith(fadeOut(animationSpec = tween(150)))
                },
                label = "mascot-evolution"
            ) { currentLevel ->
                val ctxLocal = androidx.compose.ui.platform.LocalContext.current
                val eggPreviewRes = if (currentLevel == MascotLevel.EGG) {
                    // Override de dev (selector en Settings); si está en "none",
                    // se usa el huevo propio de la especie actual.
                    val devOverride = when (Stats.devEggPreview(ctxLocal)) {
                        Stats.EGG_PREVIEW_NORMAL -> R.drawable.egg_normal_preview
                        Stats.EGG_PREVIEW_VERDE -> R.drawable.egg_verde_preview
                        Stats.EGG_PREVIEW_LILA -> R.drawable.egg_lila_preview
                        Stats.EGG_PREVIEW_BRASA -> R.drawable.egg_brasa_preview
                        Stats.EGG_PREVIEW_CHISPA -> R.drawable.egg_chispa_preview
                        else -> null
                    }
                    devOverride ?: species.eggRes
                } else null
                val mascotModifier = Modifier
                    .size(180.dp)
                    .graphicsLayer {
                        scaleX = bounceScale.value
                        scaleY = bounceScale.value
                        rotationZ = bounceRotation.value
                        translationY = floatY.dp.toPx() - lift.toPx()
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
                if (eggPreviewRes != null) {
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(eggPreviewRes),
                        contentDescription = null,
                        modifier = mascotModifier,
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                } else {
                    MascotCanvas(
                        level = currentLevel,
                        species = species,
                        animate = animate,
                        modifier = mascotModifier
                    )
                }
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
