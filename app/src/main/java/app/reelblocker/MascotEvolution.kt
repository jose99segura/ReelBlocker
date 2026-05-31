package app.reelblocker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Niveles de la mascota de la racha. Cada nivel se dibuja en Canvas con
 * primitivas (ovalos, arcos, paths) para que no necesitemos assets externos.
 *
 * El orden importa: cada nivel se desbloquea al alcanzar [minDays].
 */
enum class MascotLevel(
    val minDays: Int,
    @androidx.annotation.StringRes val displayNameRes: Int,
    val gradientStart: Color,
    val gradientEnd: Color,
    val bodyColor: Color,
    val accentColor: Color
) {
    EGG(
        minDays = 0,
        displayNameRes = R.string.mascot_level_egg,
        gradientStart = Color(0xFFE0F2FE),
        gradientEnd = Color(0xFFDDD6FE),
        bodyColor = Color(0xFFFEF3C7),
        accentColor = Color(0xFFFBBF24)
    ),
    CRACKING(
        minDays = 3,
        displayNameRes = R.string.mascot_level_cracking,
        gradientStart = Color(0xFFFEF3C7),
        gradientEnd = Color(0xFFFCE7F3),
        bodyColor = Color(0xFFFEF3C7),
        accentColor = Color(0xFFF59E0B)
    ),
    HATCHLING(
        minDays = 8,
        displayNameRes = R.string.mascot_level_hatchling,
        gradientStart = Color(0xFFFCE7F3),
        gradientEnd = Color(0xFFFFE4E6),
        bodyColor = Color(0xFFFDE68A),
        accentColor = Color(0xFFF59E0B)
    ),
    ADULT(
        minDays = 21,
        displayNameRes = R.string.mascot_level_adult,
        gradientStart = Color(0xFFFED7AA),
        gradientEnd = Color(0xFFFCA5A5),
        bodyColor = Color(0xFFF97316),
        accentColor = Color(0xFFC2410C)
    );

    companion object {
        fun forDays(days: Int): MascotLevel =
            entries.last { days >= it.minDays }

        /** Devuelve los días restantes hasta el siguiente nivel, o null si ya está al máximo. */
        fun daysToNext(days: Int): Int? {
            val current = forDays(days)
            val next = entries.getOrNull(current.ordinal + 1) ?: return null
            return next.minDays - days
        }

        /** Progreso 0..1 dentro del nivel actual. Si ya está al máximo, devuelve 1. */
        fun progressInLevel(days: Int): Float {
            val current = forDays(days)
            val next = entries.getOrNull(current.ordinal + 1) ?: return 1f
            val span = (next.minDays - current.minDays).toFloat()
            if (span <= 0f) return 1f
            return ((days - current.minDays) / span).coerceIn(0f, 1f)
        }
    }
}

/**
 * Mascota dibujada en Canvas. Se anima sutilmente (respiracion) si [animate] = true.
 * Si [sad] = true, dibuja una expresion triste y se balancea (para el modal de
 * confirmacion de desactivacion).
 */
@Composable
fun MascotCanvas(
    level: MascotLevel,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
    sad: Boolean = false,
    species: MascotSpecies = MascotSpecies.CLASICA
) {
    val transition = rememberInfiniteTransition(label = "mascot")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animate) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )
    val sway by transition.animateFloat(
        initialValue = -1f,
        targetValue = if (sad) 1f else -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )

    // Parpadeo: el ojo se cierra 80ms, se abre 90ms, cada 3.5-7s aleatoriamente.
    // Solo cuando animate=true (en static contexts como paywall hero el ojo
    // se queda abierto para no distraer).
    val blink = remember { Animatable(1f) }
    LaunchedEffect(animate, sad) {
        if (!animate || sad) {
            blink.snapTo(1f)
            return@LaunchedEffect
        }
        while (true) {
            delay(Random.nextLong(3500L, 7000L))
            blink.animateTo(0f, tween(80, easing = LinearEasing))
            blink.animateTo(1f, tween(90, easing = LinearEasing))
        }
    }

    Canvas(modifier = modifier) {
        val scaleFactor = 1f + 0.028f * sin(breath * PI).toFloat()
        scale(scaleFactor) {
            rotate(if (sad) sway * 4f else 0f) {
                drawMascot(species, level, sad, eyeOpenness = blink.value)
            }
        }
    }
}

private fun DrawScope.drawMascot(
    species: MascotSpecies,
    level: MascotLevel,
    sad: Boolean,
    eyeOpenness: Float
) {
    when (level) {
        MascotLevel.EGG -> drawEgg(species = species, cracked = false)
        MascotLevel.CRACKING -> drawEgg(species = species, cracked = true)
        else -> when (species) {
            MascotSpecies.CLASICA -> drawClasicaCreature(level, sad, eyeOpenness)
            MascotSpecies.DRAGON -> drawDragonBody(level, sad, eyeOpenness)
            MascotSpecies.TORTUGA -> drawTortugaBody(level, sad, eyeOpenness)
            MascotSpecies.LOBO -> drawLoboBody(level, sad, eyeOpenness)
            MascotSpecies.BUHO -> drawBuhoBody(level, sad, eyeOpenness)
        }
    }
}

private fun DrawScope.drawClasicaCreature(level: MascotLevel, sad: Boolean, eyeOpenness: Float) {
    when (level) {
        MascotLevel.HATCHLING -> drawCreature(level, hasWings = false, hasCrest = false, sad = sad, eyeOpenness = eyeOpenness)
        MascotLevel.ADULT -> drawCreature(level, hasWings = true, hasCrest = true, sad = sad, eyeOpenness = eyeOpenness)
        else -> { /* EGG/CRACKING ya manejados arriba */ }
    }
}

/**
 * Dibuja un ojo respetando [openness] (1 = totalmente abierto, 0 = cerrado).
 * Por debajo de un umbral pequeño dibuja una curva "‿" en vez del ojo, para
 * el parpadeo natural.
 *
 * Visible para los drawers de especies en MascotSpecies.kt (mismo paquete).
 */
internal fun DrawScope.drawEye(
    center: Offset,
    eyeR: Float,
    pupilColor: Color,
    pupilOffsetY: Float,
    openness: Float
) {
    val o = openness.coerceIn(0f, 1f)
    if (o < 0.18f) {
        val path = Path().apply {
            moveTo(center.x - eyeR * 0.95f, center.y)
            quadraticBezierTo(
                center.x, center.y + eyeR * 0.45f,
                center.x + eyeR * 0.95f, center.y
            )
        }
        drawPath(
            path = path,
            color = pupilColor,
            style = Stroke(width = eyeR * 0.30f, cap = StrokeCap.Round)
        )
        return
    }
    // Ojo abierto/semi-abierto: escala vertical por openness.
    drawOval(
        color = Color.White,
        topLeft = Offset(center.x - eyeR, center.y - eyeR * o),
        size = Size(eyeR * 2f, eyeR * 2f * o)
    )
    drawCircle(
        color = pupilColor,
        radius = eyeR * 0.55f * o,
        center = Offset(center.x, center.y + pupilOffsetY * o)
    )
    drawCircle(
        color = Color.White,
        radius = eyeR * 0.22f * o,
        center = Offset(
            center.x + eyeR * 0.2f,
            center.y + pupilOffsetY * o - eyeR * 0.2f * o
        )
    )
}

private fun DrawScope.drawEgg(species: MascotSpecies, cracked: Boolean) {
    val w = size.width
    val h = size.height
    val eggW = w * 0.55f
    val eggH = h * 0.72f
    val cx = w / 2f
    val cy = h / 2f + h * 0.02f

    // Sombra inferior suave.
    drawOval(
        color = Color.Black.copy(alpha = 0.10f),
        topLeft = Offset(cx - eggW / 2f, cy + eggH / 2f - h * 0.04f),
        size = Size(eggW, h * 0.08f)
    )

    // Cuerpo del huevo con gradiente vertical.
    val eggBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7), Color(0xFFFCD34D)),
        start = Offset(cx, cy - eggH / 2f),
        end = Offset(cx, cy + eggH / 2f)
    )
    drawOval(
        brush = eggBrush,
        topLeft = Offset(cx - eggW / 2f, cy - eggH / 2f),
        size = Size(eggW, eggH)
    )

    // Highlight superior izquierdo (reflejo).
    drawOval(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0f)),
            center = Offset(cx - eggW * 0.18f, cy - eggH * 0.25f),
            radius = eggW * 0.25f
        ),
        topLeft = Offset(cx - eggW * 0.35f, cy - eggH * 0.45f),
        size = Size(eggW * 0.45f, eggH * 0.45f)
    )

    // Pintitas decorativas — tono basado en la especie para diferenciar huevos.
    val speckle = species.accentTint.darken(0.20f).copy(alpha = 0.45f)
    listOf(
        Offset(cx - eggW * 0.15f, cy + eggH * 0.05f) to (w * 0.018f),
        Offset(cx + eggW * 0.12f, cy - eggH * 0.10f) to (w * 0.013f),
        Offset(cx + eggW * 0.05f, cy + eggH * 0.22f) to (w * 0.016f),
        Offset(cx - eggW * 0.20f, cy + eggH * 0.25f) to (w * 0.011f)
    ).forEach { (pos, r) ->
        drawCircle(color = speckle, radius = r, center = pos)
    }

    if (cracked) {
        // Linea de grieta zigzagueante.
        val crackColor = Color(0xFF78350F)
        val path = Path().apply {
            moveTo(cx - eggW * 0.30f, cy - eggH * 0.05f)
            lineTo(cx - eggW * 0.18f, cy - eggH * 0.12f)
            lineTo(cx - eggW * 0.05f, cy - eggH * 0.02f)
            lineTo(cx + eggW * 0.08f, cy - eggH * 0.10f)
            lineTo(cx + eggW * 0.20f, cy - eggH * 0.01f)
            lineTo(cx + eggW * 0.30f, cy - eggH * 0.08f)
        }
        drawPath(
            path = path,
            color = crackColor,
            style = Stroke(width = w * 0.012f, cap = StrokeCap.Round)
        )
        // Grieta secundaria mas corta.
        val path2 = Path().apply {
            moveTo(cx - eggW * 0.05f, cy - eggH * 0.02f)
            lineTo(cx - eggW * 0.10f, cy + eggH * 0.10f)
            lineTo(cx + eggW * 0.02f, cy + eggH * 0.15f)
        }
        drawPath(
            path = path2,
            color = crackColor,
            style = Stroke(width = w * 0.009f, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawCreature(
    level: MascotLevel,
    hasWings: Boolean,
    hasCrest: Boolean,
    sad: Boolean = false,
    eyeOpenness: Float = 1f
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.55f

    val bodySize = w * 0.52f
    val body = level.bodyColor
    val accent = level.accentColor

    // Sombra base.
    drawOval(
        color = Color.Black.copy(alpha = 0.12f),
        topLeft = Offset(cx - bodySize * 0.45f, cy + bodySize * 0.35f),
        size = Size(bodySize * 0.9f, h * 0.05f)
    )

    // Crest (cresta) detras del cuerpo.
    if (hasCrest) {
        val crestPath = Path().apply {
            moveTo(cx - bodySize * 0.12f, cy - bodySize * 0.55f)
            quadraticBezierTo(cx, cy - bodySize * 0.95f, cx + bodySize * 0.05f, cy - bodySize * 0.50f)
            quadraticBezierTo(cx + bodySize * 0.15f, cy - bodySize * 0.85f, cx + bodySize * 0.20f, cy - bodySize * 0.45f)
            quadraticBezierTo(cx + bodySize * 0.25f, cy - bodySize * 0.75f, cx + bodySize * 0.30f, cy - bodySize * 0.35f)
            close()
        }
        drawPath(crestPath, color = accent)
    }

    // Wings (alas) — detras del cuerpo.
    if (hasWings) {
        val wingColor = accent.copy(alpha = 0.85f)
        val leftWing = Path().apply {
            moveTo(cx - bodySize * 0.35f, cy - bodySize * 0.05f)
            quadraticBezierTo(
                cx - bodySize * 0.85f, cy + bodySize * 0.05f,
                cx - bodySize * 0.55f, cy + bodySize * 0.30f
            )
            quadraticBezierTo(
                cx - bodySize * 0.45f, cy + bodySize * 0.15f,
                cx - bodySize * 0.35f, cy + bodySize * 0.20f
            )
            close()
        }
        val rightWing = Path().apply {
            moveTo(cx + bodySize * 0.35f, cy - bodySize * 0.05f)
            quadraticBezierTo(
                cx + bodySize * 0.85f, cy + bodySize * 0.05f,
                cx + bodySize * 0.55f, cy + bodySize * 0.30f
            )
            quadraticBezierTo(
                cx + bodySize * 0.45f, cy + bodySize * 0.15f,
                cx + bodySize * 0.35f, cy + bodySize * 0.20f
            )
            close()
        }
        drawPath(leftWing, color = wingColor)
        drawPath(rightWing, color = wingColor)
    }

    // Cuerpo principal con gradiente esferico.
    val bodyBrush = Brush.radialGradient(
        colors = listOf(
            body.lighten(0.25f),
            body,
            body.darken(0.18f)
        ),
        center = Offset(cx - bodySize * 0.15f, cy - bodySize * 0.15f),
        radius = bodySize * 0.7f
    )
    drawCircle(brush = bodyBrush, radius = bodySize * 0.5f, center = Offset(cx, cy))

    // Mejillas (rubor).
    val blush = Color(0xFFFB7185).copy(alpha = 0.45f)
    drawCircle(color = blush, radius = bodySize * 0.07f, center = Offset(cx - bodySize * 0.22f, cy + bodySize * 0.05f))
    drawCircle(color = blush, radius = bodySize * 0.07f, center = Offset(cx + bodySize * 0.22f, cy + bodySize * 0.05f))

    // Ojos.
    val eyeY = cy - bodySize * 0.08f
    val eyeOffset = bodySize * 0.15f
    val eyeR = bodySize * 0.07f
    val pupilColor = Color(0xFF1F2937)
    val pupilOffsetY = if (sad) eyeR * 0.3f else -eyeR * 0.1f
    drawEye(
        center = Offset(cx - eyeOffset, eyeY),
        eyeR = eyeR,
        pupilColor = pupilColor,
        pupilOffsetY = pupilOffsetY,
        openness = eyeOpenness
    )
    drawEye(
        center = Offset(cx + eyeOffset, eyeY),
        eyeR = eyeR,
        pupilColor = pupilColor,
        pupilOffsetY = pupilOffsetY,
        openness = eyeOpenness
    )

    // Boca / pico.
    if (sad) {
        // Boca triste — arco hacia abajo.
        val mouthPath = Path().apply {
            moveTo(cx - bodySize * 0.08f, cy + bodySize * 0.12f)
            quadraticBezierTo(cx, cy + bodySize * 0.05f, cx + bodySize * 0.08f, cy + bodySize * 0.12f)
        }
        drawPath(
            path = mouthPath,
            color = pupilColor,
            style = Stroke(width = bodySize * 0.025f, cap = StrokeCap.Round)
        )
    } else {
        // Pico naranja pequeno.
        val beakPath = Path().apply {
            moveTo(cx - bodySize * 0.05f, cy + bodySize * 0.08f)
            lineTo(cx + bodySize * 0.05f, cy + bodySize * 0.08f)
            lineTo(cx, cy + bodySize * 0.16f)
            close()
        }
        drawPath(beakPath, color = Color(0xFFEA580C))
    }
}

internal fun Color.lighten(amount: Float): Color = Color(
    red = (red + (1f - red) * amount).coerceIn(0f, 1f),
    green = (green + (1f - green) * amount).coerceIn(0f, 1f),
    blue = (blue + (1f - blue) * amount).coerceIn(0f, 1f),
    alpha = alpha
)

internal fun Color.darken(amount: Float): Color = Color(
    red = (red * (1f - amount)).coerceIn(0f, 1f),
    green = (green * (1f - amount)).coerceIn(0f, 1f),
    blue = (blue * (1f - amount)).coerceIn(0f, 1f),
    alpha = alpha
)
