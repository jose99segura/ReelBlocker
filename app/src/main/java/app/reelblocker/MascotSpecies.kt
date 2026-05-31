package app.reelblocker

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** Tier de monetización de una especie. */
enum class Tier { FREE, PRO }

/**
 * Especies coleccionables. Cada una tiene una silueta distinta en HATCHLING
 * y ADULT. EGG y CRACKING son los mismos primitivos con un tinte
 * de acento propio para distinguir a primera vista.
 */
enum class MascotSpecies(
    val id: String,
    @androidx.annotation.StringRes val displayNameRes: Int,
    /** Color de acento aplicado al huevo y a detalles menores. */
    val accentTint: Color,
    val tier: Tier
) {
    CLASICA(
        id = "clasica",
        displayNameRes = R.string.mascot_species_classic,
        accentTint = Color(0xFFFB923C),
        tier = Tier.FREE
    ),
    DRAGON(
        id = "dragon",
        displayNameRes = R.string.mascot_species_dragon,
        accentTint = Color(0xFF7C3AED),
        tier = Tier.FREE
    ),
    TORTUGA(
        id = "tortuga",
        displayNameRes = R.string.mascot_species_turtle,
        accentTint = Color(0xFF059669),
        tier = Tier.PRO
    ),
    LOBO(
        id = "lobo",
        displayNameRes = R.string.mascot_species_wolf,
        accentTint = Color(0xFF475569),
        tier = Tier.PRO
    ),
    BUHO(
        id = "buho",
        displayNameRes = R.string.mascot_species_owl,
        accentTint = Color(0xFFA16207),
        tier = Tier.PRO
    );

    val isPro: Boolean get() = tier == Tier.PRO

    /**
     * Huevo (PNG, render 3D) propio de la especie, mostrado en nivel EGG.
     * Cada especie tiene su huevo, así que al ciclar [Collection.pickNext]
     * por especies no coleccionadas, cada graduación entrega un huevo
     * visualmente distinto hasta completar la colección. (`null` se caería
     * al huevo dibujado por Canvas; ya no hay ninguna especie sin huevo.)
     */
    @get:androidx.annotation.DrawableRes
    val eggRes: Int?
        get() = when (this) {
            CLASICA -> R.drawable.egg_normal_preview
            TORTUGA -> R.drawable.egg_verde_preview
            DRAGON -> R.drawable.egg_lila_preview
            LOBO -> R.drawable.egg_brasa_preview   // huevo de fuego (Brasa)
            BUHO -> R.drawable.egg_chispa_preview  // huevo eléctrico (Chispa)
        }

    companion object {
        fun fromIdOrNull(id: String?): MascotSpecies? =
            entries.firstOrNull { it.id == id }

        val DEFAULT: MascotSpecies = CLASICA

        fun freeSpecies(): List<MascotSpecies> = entries.filter { it.tier == Tier.FREE }
        fun proSpecies(): List<MascotSpecies> = entries.filter { it.tier == Tier.PRO }
    }
}

// ============================================================
//   Dragon — cuerpo redondo con cuernos siempre, alas
//   membranosas desde ADULT, cola corta.
// ============================================================

internal fun DrawScope.drawDragonBody(level: MascotLevel, sad: Boolean, eyeOpenness: Float = 1f) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.55f
    val big = level.ordinal >= MascotLevel.ADULT.ordinal
    val bodySize = if (big) w * 0.60f else w * 0.50f
    val accent = MascotSpecies.DRAGON.accentTint
    val body = level.bodyColor

    drawShadow(cx, cy, bodySize, h)

    // Cola hacia abajo-derecha.
    if (level.ordinal >= MascotLevel.HATCHLING.ordinal) {
        val tail = Path().apply {
            moveTo(cx + bodySize * 0.35f, cy + bodySize * 0.20f)
            quadraticBezierTo(
                cx + bodySize * 0.70f, cy + bodySize * 0.35f,
                cx + bodySize * 0.55f, cy + bodySize * 0.55f
            )
            quadraticBezierTo(
                cx + bodySize * 0.45f, cy + bodySize * 0.40f,
                cx + bodySize * 0.30f, cy + bodySize * 0.35f
            )
            close()
        }
        drawPath(tail, color = body.darken(0.05f))
        // Punta de la cola en forma de flecha.
        val tailTip = Path().apply {
            moveTo(cx + bodySize * 0.55f, cy + bodySize * 0.55f)
            lineTo(cx + bodySize * 0.72f, cy + bodySize * 0.62f)
            lineTo(cx + bodySize * 0.62f, cy + bodySize * 0.42f)
            close()
        }
        drawPath(tailTip, color = accent)
    }

    // Cuernos siempre presentes.
    drawTwoHorns(cx, cy, bodySize, color = accent.darken(0.10f))

    // Alas membranosas desde ADULT.
    if (level.ordinal >= MascotLevel.ADULT.ordinal) {
        drawMembraneWings(cx, cy, bodySize, accent = accent)
    }

    drawSpheroidBody(cx, cy, bodySize, body)

    // Cresta dorsal — pequeñas espinas en la cabeza.
    val spikeColor = accent.darken(0.10f)
    for (i in 0..2) {
        val sx = cx - bodySize * 0.10f + i * bodySize * 0.10f
        val sy = cy - bodySize * 0.42f
        val tri = Path().apply {
            moveTo(sx, sy)
            lineTo(sx + bodySize * 0.04f, sy + bodySize * 0.10f)
            lineTo(sx - bodySize * 0.04f, sy + bodySize * 0.10f)
            close()
        }
        drawPath(tri, color = spikeColor)
    }

    drawFace(cx, cy, bodySize, sad = sad, eyeOpenness = eyeOpenness)
}

// ============================================================
//   Tortuga — cuerpo bajo y ancho con caparazón segmentado.
// ============================================================

internal fun DrawScope.drawTortugaBody(level: MascotLevel, sad: Boolean, eyeOpenness: Float = 1f) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.58f
    val big = level.ordinal >= MascotLevel.ADULT.ordinal
    val bodyW = if (big) w * 0.66f else w * 0.56f
    val bodyH = bodyW * 0.62f
    val body = level.bodyColor
    val shellAccent = MascotSpecies.TORTUGA.accentTint

    drawShadow(cx, cy, bodyW, h)

    // Patitas (4 ovalos abajo).
    val footColor = body.darken(0.18f)
    listOf(
        Offset(cx - bodyW * 0.30f, cy + bodyH * 0.30f),
        Offset(cx - bodyW * 0.05f, cy + bodyH * 0.35f),
        Offset(cx + bodyW * 0.05f, cy + bodyH * 0.35f),
        Offset(cx + bodyW * 0.30f, cy + bodyH * 0.30f)
    ).forEach { p ->
        drawOval(
            color = footColor,
            topLeft = Offset(p.x - bodyW * 0.08f, p.y),
            size = Size(bodyW * 0.16f, bodyH * 0.20f)
        )
    }

    // Cabeza sobresaliendo a la izquierda.
    drawOval(
        color = body,
        topLeft = Offset(cx - bodyW * 0.62f, cy - bodyH * 0.10f),
        size = Size(bodyW * 0.32f, bodyH * 0.45f)
    )

    // Caparazón — cúpula sobre el cuerpo.
    val shellBrush = Brush.radialGradient(
        colors = listOf(
            shellAccent.lighten(0.15f),
            shellAccent,
            shellAccent.darken(0.20f)
        ),
        center = Offset(cx - bodyW * 0.10f, cy - bodyH * 0.10f),
        radius = bodyW * 0.50f
    )
    drawOval(
        brush = shellBrush,
        topLeft = Offset(cx - bodyW * 0.40f, cy - bodyH * 0.40f),
        size = Size(bodyW * 0.80f, bodyH * 0.85f)
    )

    // Segmentos del caparazón (líneas curvas).
    val segColor = shellAccent.darken(0.30f).copy(alpha = 0.5f)
    val stroke = Stroke(width = bodyW * 0.015f, cap = StrokeCap.Round)
    // Línea horizontal central.
    val midPath = Path().apply {
        moveTo(cx - bodyW * 0.35f, cy)
        quadraticBezierTo(cx, cy - bodyH * 0.05f, cx + bodyW * 0.35f, cy)
    }
    drawPath(midPath, color = segColor, style = stroke)
    // Tres líneas verticales separando hexágonos.
    for (i in -1..1) {
        val px = cx + i * bodyW * 0.18f
        val seg = Path().apply {
            moveTo(px, cy - bodyH * 0.32f)
            quadraticBezierTo(px + bodyW * 0.02f, cy, px, cy + bodyH * 0.32f)
        }
        drawPath(seg, color = segColor, style = stroke)
    }

    // Cara en la cabeza saliente.
    drawFace(
        cx = cx - bodyW * 0.45f,
        cy = cy + bodyH * 0.05f,
        bodySize = bodyW * 0.30f,
        sad = sad,
        eyeOpenness = eyeOpenness
    )
}

// ============================================================
//   Lobo — cuerpo redondo con orejas triangulares + hocico.
// ============================================================

internal fun DrawScope.drawLoboBody(level: MascotLevel, sad: Boolean, eyeOpenness: Float = 1f) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.55f
    val big = level.ordinal >= MascotLevel.ADULT.ordinal
    val bodySize = if (big) w * 0.58f else w * 0.50f
    val body = level.bodyColor
    val accent = MascotSpecies.LOBO.accentTint

    drawShadow(cx, cy, bodySize, h)

    // Orejas triangulares grandes.
    val earOuter = accent
    val earInner = accent.lighten(0.35f)
    val leftEar = Path().apply {
        moveTo(cx - bodySize * 0.38f, cy - bodySize * 0.30f)
        lineTo(cx - bodySize * 0.50f, cy - bodySize * 0.70f)
        lineTo(cx - bodySize * 0.18f, cy - bodySize * 0.45f)
        close()
    }
    val rightEar = Path().apply {
        moveTo(cx + bodySize * 0.38f, cy - bodySize * 0.30f)
        lineTo(cx + bodySize * 0.50f, cy - bodySize * 0.70f)
        lineTo(cx + bodySize * 0.18f, cy - bodySize * 0.45f)
        close()
    }
    drawPath(leftEar, color = earOuter)
    drawPath(rightEar, color = earOuter)
    // Interior rosado.
    val leftInner = Path().apply {
        moveTo(cx - bodySize * 0.36f, cy - bodySize * 0.36f)
        lineTo(cx - bodySize * 0.45f, cy - bodySize * 0.62f)
        lineTo(cx - bodySize * 0.24f, cy - bodySize * 0.45f)
        close()
    }
    val rightInner = Path().apply {
        moveTo(cx + bodySize * 0.36f, cy - bodySize * 0.36f)
        lineTo(cx + bodySize * 0.45f, cy - bodySize * 0.62f)
        lineTo(cx + bodySize * 0.24f, cy - bodySize * 0.45f)
        close()
    }
    drawPath(leftInner, color = earInner)
    drawPath(rightInner, color = earInner)

    drawSpheroidBody(cx, cy, bodySize, body)

    // Hocico — pequeña protuberancia central inferior.
    val muzzleColor = body.lighten(0.15f)
    drawOval(
        color = muzzleColor,
        topLeft = Offset(cx - bodySize * 0.18f, cy + bodySize * 0.02f),
        size = Size(bodySize * 0.36f, bodySize * 0.26f)
    )

    drawFace(cx, cy, bodySize, sad = sad, customNose = true, eyeOpenness = eyeOpenness)
}

// ============================================================
//   Búho — cuerpo muy redondo, ojos enormes, plumas/penacho.
// ============================================================

internal fun DrawScope.drawBuhoBody(level: MascotLevel, sad: Boolean, eyeOpenness: Float = 1f) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h * 0.56f
    val big = level.ordinal >= MascotLevel.ADULT.ordinal
    val bodySize = if (big) w * 0.62f else w * 0.54f
    val body = level.bodyColor
    val accent = MascotSpecies.BUHO.accentTint

    drawShadow(cx, cy, bodySize, h)

    // Pequeñas alas plegadas a los lados (desde ADULT).
    if (level.ordinal >= MascotLevel.ADULT.ordinal) {
        val wingColor = body.darken(0.12f)
        val leftWing = Path().apply {
            moveTo(cx - bodySize * 0.35f, cy - bodySize * 0.05f)
            quadraticBezierTo(
                cx - bodySize * 0.55f, cy + bodySize * 0.20f,
                cx - bodySize * 0.30f, cy + bodySize * 0.30f
            )
            close()
        }
        val rightWing = Path().apply {
            moveTo(cx + bodySize * 0.35f, cy - bodySize * 0.05f)
            quadraticBezierTo(
                cx + bodySize * 0.55f, cy + bodySize * 0.20f,
                cx + bodySize * 0.30f, cy + bodySize * 0.30f
            )
            close()
        }
        drawPath(leftWing, color = wingColor)
        drawPath(rightWing, color = wingColor)
    }

    drawSpheroidBody(cx, cy, bodySize, body)

    // Penacho / tufts de plumas en la cabeza.
    val tuftColor = accent
    val leftTuft = Path().apply {
        moveTo(cx - bodySize * 0.30f, cy - bodySize * 0.42f)
        lineTo(cx - bodySize * 0.20f, cy - bodySize * 0.65f)
        lineTo(cx - bodySize * 0.15f, cy - bodySize * 0.40f)
        close()
    }
    val rightTuft = Path().apply {
        moveTo(cx + bodySize * 0.30f, cy - bodySize * 0.42f)
        lineTo(cx + bodySize * 0.20f, cy - bodySize * 0.65f)
        lineTo(cx + bodySize * 0.15f, cy - bodySize * 0.40f)
        close()
    }
    drawPath(leftTuft, color = tuftColor)
    drawPath(rightTuft, color = tuftColor)

    // Discos faciales (anillos alrededor de los ojos — sello del búho).
    val discColor = body.lighten(0.20f)
    drawCircle(color = discColor, radius = bodySize * 0.18f, center = Offset(cx - bodySize * 0.15f, cy - bodySize * 0.05f))
    drawCircle(color = discColor, radius = bodySize * 0.18f, center = Offset(cx + bodySize * 0.15f, cy - bodySize * 0.05f))

    // Ojos grandes (override del drawFace estándar para enfatizar).
    val eyeY = cy - bodySize * 0.05f
    val eyeOffset = bodySize * 0.15f
    val eyeR = bodySize * 0.11f
    val pupilColor = Color(0xFF1F2937)
    val pupilOffsetY = if (sad) eyeR * 0.3f else 0f
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

    // Pico pequeño triangular hacia abajo.
    val beakColor = Color(0xFFEA580C)
    val beak = Path().apply {
        moveTo(cx - bodySize * 0.04f, cy + bodySize * 0.05f)
        lineTo(cx + bodySize * 0.04f, cy + bodySize * 0.05f)
        lineTo(cx, cy + bodySize * 0.14f)
        close()
    }
    drawPath(beak, color = beakColor)

    if (sad && eyeOpenness > 0.5f) {
        // Mini ceño caído sobre cada ojo (solo si los ojos están abiertos).
        val brow = Stroke(width = bodySize * 0.022f, cap = StrokeCap.Round)
        val leftBrow = Path().apply {
            moveTo(cx - eyeOffset - eyeR, eyeY - eyeR * 1.4f)
            lineTo(cx - eyeOffset + eyeR, eyeY - eyeR * 0.9f)
        }
        val rightBrow = Path().apply {
            moveTo(cx + eyeOffset + eyeR, eyeY - eyeR * 1.4f)
            lineTo(cx + eyeOffset - eyeR, eyeY - eyeR * 0.9f)
        }
        drawPath(leftBrow, color = pupilColor, style = brow)
        drawPath(rightBrow, color = pupilColor, style = brow)
    }
}

// ============================================================
//   Helpers compartidos (visibles para MascotEvolution.kt
//   via internal).
// ============================================================

internal fun DrawScope.drawShadow(cx: Float, cy: Float, bodySize: Float, canvasH: Float) {
    drawOval(
        color = Color.Black.copy(alpha = 0.12f),
        topLeft = Offset(cx - bodySize * 0.45f, cy + bodySize * 0.35f),
        size = Size(bodySize * 0.9f, canvasH * 0.05f)
    )
}

internal fun DrawScope.drawSpheroidBody(cx: Float, cy: Float, bodySize: Float, body: Color) {
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
}

internal fun DrawScope.drawTwoHorns(cx: Float, cy: Float, bodySize: Float, color: Color) {
    val leftHorn = Path().apply {
        moveTo(cx - bodySize * 0.30f, cy - bodySize * 0.45f)
        quadraticBezierTo(cx - bodySize * 0.55f, cy - bodySize * 0.75f, cx - bodySize * 0.40f, cy - bodySize * 0.90f)
        lineTo(cx - bodySize * 0.22f, cy - bodySize * 0.50f)
        close()
    }
    val rightHorn = Path().apply {
        moveTo(cx + bodySize * 0.30f, cy - bodySize * 0.45f)
        quadraticBezierTo(cx + bodySize * 0.55f, cy - bodySize * 0.75f, cx + bodySize * 0.40f, cy - bodySize * 0.90f)
        lineTo(cx + bodySize * 0.22f, cy - bodySize * 0.50f)
        close()
    }
    drawPath(leftHorn, color = color)
    drawPath(rightHorn, color = color)
}

internal fun DrawScope.drawMembraneWings(cx: Float, cy: Float, bodySize: Float, accent: Color) {
    val wingColor = accent.copy(alpha = 0.85f)
    val leftWing = Path().apply {
        moveTo(cx - bodySize * 0.35f, cy - bodySize * 0.10f)
        quadraticBezierTo(
            cx - bodySize * 0.95f, cy - bodySize * 0.10f,
            cx - bodySize * 0.85f, cy + bodySize * 0.25f
        )
        lineTo(cx - bodySize * 0.55f, cy + bodySize * 0.05f)
        lineTo(cx - bodySize * 0.35f, cy + bodySize * 0.15f)
        close()
    }
    val rightWing = Path().apply {
        moveTo(cx + bodySize * 0.35f, cy - bodySize * 0.10f)
        quadraticBezierTo(
            cx + bodySize * 0.95f, cy - bodySize * 0.10f,
            cx + bodySize * 0.85f, cy + bodySize * 0.25f
        )
        lineTo(cx + bodySize * 0.55f, cy + bodySize * 0.05f)
        lineTo(cx + bodySize * 0.35f, cy + bodySize * 0.15f)
        close()
    }
    drawPath(leftWing, color = wingColor)
    drawPath(rightWing, color = wingColor)
}

/**
 * Cara genérica: ojos + boca/pico/mejillas. customNose=true omite el pico
 * triangular (para especies que ya tienen hocico u otro elemento).
 */
internal fun DrawScope.drawFace(
    cx: Float,
    cy: Float,
    bodySize: Float,
    sad: Boolean,
    customNose: Boolean = false,
    eyeOpenness: Float = 1f
) {
    val blush = Color(0xFFFB7185).copy(alpha = 0.45f)
    drawCircle(
        color = blush,
        radius = bodySize * 0.07f,
        center = Offset(cx - bodySize * 0.22f, cy + bodySize * 0.05f)
    )
    drawCircle(
        color = blush,
        radius = bodySize * 0.07f,
        center = Offset(cx + bodySize * 0.22f, cy + bodySize * 0.05f)
    )

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

    if (sad) {
        val mouth = Path().apply {
            moveTo(cx - bodySize * 0.08f, cy + bodySize * 0.12f)
            quadraticBezierTo(cx, cy + bodySize * 0.05f, cx + bodySize * 0.08f, cy + bodySize * 0.12f)
        }
        drawPath(
            path = mouth,
            color = pupilColor,
            style = Stroke(width = bodySize * 0.025f, cap = StrokeCap.Round)
        )
    } else if (!customNose) {
        val beak = Path().apply {
            moveTo(cx - bodySize * 0.05f, cy + bodySize * 0.08f)
            lineTo(cx + bodySize * 0.05f, cy + bodySize * 0.08f)
            lineTo(cx, cy + bodySize * 0.16f)
            close()
        }
        drawPath(beak, color = Color(0xFFEA580C))
    } else {
        // Nariz pequeña centrada (para lobo).
        drawCircle(
            color = pupilColor,
            radius = bodySize * 0.035f,
            center = Offset(cx, cy + bodySize * 0.10f)
        )
    }
}
