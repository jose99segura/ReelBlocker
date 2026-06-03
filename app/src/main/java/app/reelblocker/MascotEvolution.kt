package app.reelblocker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import kotlin.math.PI
import kotlin.math.sin

/**
 * Niveles de la mascota de la racha. Cada nivel mapea a un sprite webp por
 * especie (ver [MascotSpecies.spriteRes]). Conserva además las paletas de
 * color (gradientes/acento) que usa la UI (anillo de progreso, halo, etc.).
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
 * Mascota mostrada como sprite webp (render 3D). Respira sutilmente si
 * [animate] = true. Si [sad] = true, se dessatura y se inclina ligeramente
 * para transmitir tristeza (modal de confirmación de desactivación), sin
 * necesitar arte aparte.
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
    val sadFilter = remember(sad) {
        if (sad) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.45f) }) else null
    }

    Image(
        painter = painterResource(species.spriteRes(level)),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        colorFilter = sadFilter,
        modifier = modifier.graphicsLayer {
            val s = 1f + 0.028f * sin(breath * PI).toFloat()
            scaleX = s
            scaleY = s
            rotationZ = if (sad) 4f else 0f
        }
    )
}

/**
 * Renderiza el sprite de la mascota a un Bitmap de Android fuera de cualquier
 * composición. Usado por el widget de pantalla de inicio (RemoteViews no
 * soporta Compose) y por las share cards.
 */
fun renderMascotBitmap(
    context: Context,
    species: MascotSpecies,
    level: MascotLevel,
    sizePx: Int
): Bitmap {
    val src = BitmapFactory.decodeResource(context.resources, species.spriteRes(level))
    return Bitmap.createScaledBitmap(src, sizePx, sizePx, true)
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
