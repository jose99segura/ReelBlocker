package app.reelblocker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Nivel de estrellas de una especie coleccionada (ver [Collection.stars]).
 * ★1 = primera graduación; cada repetición suma una hasta [Collection.MAX_STARS],
 * que se pinta en dorado como "nivel máximo".
 */
internal val MaxStarsGold = Color(0xFFF59E0B)

/** Color de las estrellas/aura: acento de la especie, dorado al máximo. */
internal fun starColor(stars: Int, accent: Color): Color =
    if (stars >= Collection.MAX_STARS) MaxStarsGold else accent

/**
 * Fila de estrellas: rellenas hasta [stars], apagadas hasta el tope. No pinta
 * nada si [stars] < 1 (especie no coleccionada).
 */
@Composable
fun StarRow(
    stars: Int,
    accent: Color,
    modifier: Modifier = Modifier,
    starSize: Dp = 12.dp
) {
    if (stars < 1) return
    val shown = stars.coerceAtMost(Collection.MAX_STARS)
    val color = starColor(stars, accent)
    val cd = stringResource(R.string.cd_mascot_stars, shown, Collection.MAX_STARS)
    Row(
        modifier = modifier.semantics { contentDescription = cd },
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        repeat(Collection.MAX_STARS) { i ->
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (i < shown) color else color.copy(alpha = 0.18f),
                modifier = Modifier.size(starSize)
            )
        }
    }
}

/**
 * Halo radial detrás de la mascota que crece con las estrellas: nada a ★1,
 * cada vez más intenso de ★2 a ★4 y dorado a ★5. Es la recompensa visual de
 * volver a graduar una especie sin necesitar sprites nuevos.
 */
fun Modifier.starAura(stars: Int, accent: Color): Modifier {
    if (stars < 2) return this
    val shown = stars.coerceAtMost(Collection.MAX_STARS)
    val color = starColor(stars, accent)
    val alpha = 0.14f + 0.09f * (shown - 2)
    return drawBehind {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                center = center,
                radius = size.minDimension * 0.62f
            ),
            radius = size.minDimension * 0.62f
        )
    }
}
