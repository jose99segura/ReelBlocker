package app.reelblocker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Generación de la tarjeta-imagen premium para compartir una graduación
 * (día 21) en redes (Instagram Stories, LinkedIn, etc.).
 *
 * Formato vertical 9:16 (1080×1920). Reutiliza [renderMascotBitmap] para la
 * mascota y dibuja el resto con [Canvas] + [Paint] (texto, paneles, gradiente).
 * El texto NO se auto-localiza al pintarse con Paint, así que TODOS los
 * literales se leen con [Context.getString] en tiempo de render (respeta el
 * locale actual del sistema).
 *
 * Disponible para todos los usuarios (gratis) — cada imagen lleva la marca a
 * redes (marketing viral).
 */
private const val TAG = "ReelBlocker.ShareCard"

private const val CARD_W = 1080
private const val CARD_H = 1920

/** Blanco con alpha [a] (0..255), sin importar android.graphics.Color. */
private fun whiteAlpha(a: Int): Int = (a shl 24) or 0x00FFFFFF

/**
 * Renderiza el icono de la app (adaptive icon) a un Bitmap cuadrado con
 * esquinas redondeadas, para el CTA "Disponible en Google Play". Devuelve null
 * si el drawable no se puede cargar (el caller omite el logo).
 */
private fun appIconRounded(ctx: Context, sizePx: Int, radius: Float): Bitmap? {
    val d = ResourcesCompat.getDrawable(ctx.resources, R.mipmap.ic_launcher, ctx.theme)
        ?: return null
    val src = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    d.setBounds(0, 0, sizePx, sizePx)
    d.draw(Canvas(src))
    val out = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    Canvas(out).drawRoundRect(
        RectF(0f, 0f, sizePx.toFloat(), sizePx.toFloat()), radius, radius, paint
    )
    src.recycle()
    return out
}

/**
 * Construye la tarjeta 1080×1920. Función pura y pesada (CPU): llamar fuera
 * del hilo principal. El caller debe reciclar el Bitmap devuelto tras usarlo.
 */
fun buildGraduationShareBitmap(
    ctx: Context,
    species: MascotSpecies,
    daysReached: Int,
    secondsSaved: Long,
    streakRecord: Int
): Bitmap {
    val bmp = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val accent = species.accentTint
    drawCardBase(ctx, canvas, accent, species, MascotLevel.ADULT, ctx.getString(R.string.share_card_eyebrow))
    drawHeadlineCaption(
        canvas,
        ctx.getString(R.string.share_card_days, daysReached),
        ctx.getString(R.string.share_card_days_caption)
    )
    drawTwoChips(
        canvas, accent,
        formatTimeSaved(secondsSaved), ctx.getString(R.string.share_card_stat_time_label),
        ctx.getString(R.string.share_card_streak_value, streakRecord), ctx.getString(R.string.share_card_stat_streak_label)
    )
    drawBrandFooter(ctx, canvas)
    return bmp
}

/**
 * Tarjeta de estadísticas (export desde la pantalla de Stats). Mismo lenguaje
 * visual que la de graduación: titular = tiempo recuperado de por vida.
 */
fun buildStatsShareBitmap(
    ctx: Context,
    species: MascotSpecies,
    level: MascotLevel,
    lifetimeBlocks: Long,
    streakRecord: Int
): Bitmap {
    val bmp = Bitmap.createBitmap(CARD_W, CARD_H, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val accent = species.accentTint
    drawCardBase(ctx, canvas, accent, species, level, ctx.getString(R.string.share_stats_eyebrow))
    drawHeadlineCaption(
        canvas,
        formatTimeSaved(lifetimeBlocks * Stats.SECONDS_PER_BLOCK),
        ctx.getString(R.string.share_stats_time_caption)
    )
    drawTwoChips(
        canvas, accent,
        lifetimeBlocks.toString(), ctx.getString(R.string.share_stats_blocks_label),
        ctx.getString(R.string.share_card_streak_value, streakRecord), ctx.getString(R.string.share_card_stat_streak_label)
    )
    drawBrandFooter(ctx, canvas)
    return bmp
}

// ===== Helpers de dibujo compartidos por las tarjetas =====

private val CARD_CX = CARD_W / 2f
private val GRAY_LIGHT = 0xFFC2C7D0.toInt()
private val GRAY_MUTED = 0xFF8A909C.toInt()

/** Fondo oscuro + glow + wordmark + eyebrow + anillo + mascota + nombre de especie. */
private fun drawCardBase(
    ctx: Context,
    canvas: Canvas,
    accent: androidx.compose.ui.graphics.Color,
    species: MascotSpecies,
    level: MascotLevel,
    eyebrowText: String
) {
    val accentInt = accent.toArgb()
    val glowCy = 700f

    canvas.drawRect(
        0f, 0f, CARD_W.toFloat(), CARD_H.toFloat(),
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, CARD_H.toFloat(),
                accent.darken(0.86f).toArgb(), 0xFF050507.toInt(),
                Shader.TileMode.CLAMP
            )
        }
    )
    canvas.drawCircle(
        CARD_CX, glowCy, 470f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                CARD_CX, glowCy, 470f,
                accent.copy(alpha = 0.34f).toArgb(), accent.copy(alpha = 0f).toArgb(),
                Shader.TileMode.CLAMP
            )
        }
    )
    canvas.drawText(
        ctx.getString(R.string.app_name), CARD_CX, 168f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = whiteAlpha(255); textAlign = Paint.Align.CENTER; textSize = 78f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); letterSpacing = 0.04f
        }
    )
    canvas.drawText(
        eyebrowText.uppercase(), CARD_CX, 248f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentInt; textAlign = Paint.Align.CENTER; textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); letterSpacing = 0.24f
        }
    )
    canvas.drawCircle(
        CARD_CX, glowCy, 340f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 3f; color = accent.copy(alpha = 0.55f).toArgb()
        }
    )
    val mascotPx = 600
    val mascot = renderMascotBitmap(ctx, species, level, mascotPx)
    canvas.drawBitmap(mascot, CARD_CX - mascotPx / 2f, glowCy - mascotPx / 2f, null)
    mascot.recycle()

    canvas.drawText(
        ctx.getString(species.displayNameRes), CARD_CX, 1110f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentInt; textAlign = Paint.Align.CENTER; textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); letterSpacing = 0.04f
        }
    )
}

/** Titular grande (blanco) + subtítulo (gris). */
private fun drawHeadlineCaption(canvas: Canvas, headline: String, caption: String) {
    canvas.drawText(
        headline, CARD_CX, 1270f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = whiteAlpha(255); textAlign = Paint.Align.CENTER; textSize = 132f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    )
    canvas.drawText(
        caption, CARD_CX, 1338f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GRAY_LIGHT; textAlign = Paint.Align.CENTER; textSize = 46f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
    )
}

/** Dos chips oscuros con borde de acento: (valor, etiqueta) izq y der. */
private fun drawTwoChips(
    canvas: Canvas,
    accent: androidx.compose.ui.graphics.Color,
    leftValue: String, leftLabel: String,
    rightValue: String, rightLabel: String
) {
    val chipTop = 1430f
    val chipBottom = 1655f
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = whiteAlpha(16) }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; strokeWidth = 2f; color = accent.copy(alpha = 0.45f).toArgb()
    }
    val leftChip = RectF(140f, chipTop, 520f, chipBottom)
    val rightChip = RectF(560f, chipTop, 940f, chipBottom)
    for (chip in listOf(leftChip, rightChip)) {
        canvas.drawRoundRect(chip, 32f, 32f, fill)
        canvas.drawRoundRect(chip, 32f, 32f, stroke)
    }
    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = whiteAlpha(255); textAlign = Paint.Align.CENTER; textSize = 68f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = GRAY_MUTED; textAlign = Paint.Align.CENTER; textSize = 34f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); letterSpacing = 0.04f
    }
    val valueBaseline = chipTop + 110f
    val labelBaseline = chipTop + 168f
    canvas.drawText(leftValue, leftChip.centerX(), valueBaseline, valuePaint)
    canvas.drawText(leftLabel, leftChip.centerX(), labelBaseline, labelPaint)
    canvas.drawText(rightValue, rightChip.centerX(), valueBaseline, valuePaint)
    canvas.drawText(rightLabel, rightChip.centerX(), labelBaseline, labelPaint)
}

/** Lema + tagline + CTA "Disponible en Google Play" con el logo. */
private fun drawBrandFooter(ctx: Context, canvas: Canvas) {
    canvas.drawText(
        ctx.getString(R.string.share_card_motto), CARD_CX, 1715f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = whiteAlpha(245); textAlign = Paint.Align.CENTER; textSize = 52f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
    )
    canvas.drawText(
        ctx.getString(R.string.share_card_tagline), CARD_CX, 1772f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = GRAY_MUTED; textAlign = Paint.Align.CENTER; textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
    )
    val ctaText = ctx.getString(R.string.share_card_get_on_play)
    val ctaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = whiteAlpha(230); textAlign = Paint.Align.LEFT; textSize = 36f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); letterSpacing = 0.02f
    }
    val iconSize = 62
    val icon = appIconRounded(ctx, iconSize, 16f)
    val gap = 18f
    val iconAdvance = if (icon != null) iconSize + gap else 0f
    val startX = CARD_CX - (iconAdvance + ctaPaint.measureText(ctaText)) / 2f
    val ctaCenterY = 1858f
    if (icon != null) {
        canvas.drawBitmap(icon, startX, ctaCenterY - iconSize / 2f, null)
        icon.recycle()
    }
    val fm = ctaPaint.fontMetrics
    canvas.drawText(ctaText, startX + iconAdvance, ctaCenterY - (fm.ascent + fm.descent) / 2f, ctaPaint)
}

/** Segundos → "Xh Ym" / "Ym" / "—". Misma lógica que formatRecoveredFull (StatsScreen). */
internal fun formatTimeSaved(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "—"
    }
}

/** Guarda el PNG en cacheDir/shared_images/ y devuelve una Uri de FileProvider. */
private fun saveShareImage(ctx: Context, bmp: Bitmap, fileName: String): Uri {
    val dir = File(ctx.cacheDir, "shared_images")
    dir.mkdirs()
    val file = File(dir, fileName)
    FileOutputStream(file).use { out ->
        bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
}

/** Dispara el share sheet con [uri] (imagen) + [caption]. En Main thread. */
private fun fireImageShare(ctx: Context, uri: Uri, caption: String, chooser: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, caption)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ctx.startActivity(
        Intent.createChooser(send, chooser).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    )
}

/**
 * Entry point que llaman ambas pantallas (graduación e Bestiario). Renderiza
 * la tarjeta fuera del hilo principal, la guarda como PNG y dispara el share
 * sheet con imagen + caption. Si algo falla, cae al share de texto plano.
 *
 * Debe invocarse desde una corutina con dispatcher Main (p. ej. el scope de un
 * Composable): el trabajo pesado se delega a [Dispatchers.Default] internamente
 * y `startActivity` se ejecuta de vuelta en Main.
 */
suspend fun shareGraduationImage(
    ctx: Context,
    species: MascotSpecies,
    daysReached: Int,
    secondsSaved: Long,
    streakRecord: Int
) {
    val caption = ctx.getString(
        R.string.graduation_share_text,
        daysReached,
        ctx.getString(species.displayNameRes)
    )
    val chooser = ctx.getString(R.string.graduation_share_chooser)
    try {
        val uri = withContext(Dispatchers.Default) {
            val bmp = buildGraduationShareBitmap(ctx, species, daysReached, secondsSaved, streakRecord)
            val u = saveShareImage(ctx, bmp, "basta_graduation.png")
            bmp.recycle()
            u
        }
        fireImageShare(ctx, uri, caption, chooser)
    } catch (e: Exception) {
        Log.w(TAG, "shareGraduationImage: fallo generando imagen, fallback a texto", e)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, caption)
        }
        ctx.startActivity(Intent.createChooser(send, chooser))
    }
}

/**
 * Export de estadísticas como imagen tipo tarjeta (pantalla de Stats, Pro).
 * Renderiza fuera del hilo principal y comparte por el share sheet. Fallback a
 * texto si algo falla.
 */
suspend fun shareStatsImage(
    ctx: Context,
    species: MascotSpecies,
    level: MascotLevel,
    lifetimeBlocks: Long,
    streakRecord: Int
) {
    val hours = (lifetimeBlocks * Stats.SECONDS_PER_BLOCK) / 3600
    val caption = ctx.getString(R.string.share_stats_caption, lifetimeBlocks, hours)
    val chooser = ctx.getString(R.string.share_stats_chooser)
    try {
        val uri = withContext(Dispatchers.Default) {
            val bmp = buildStatsShareBitmap(ctx, species, level, lifetimeBlocks, streakRecord)
            val u = saveShareImage(ctx, bmp, "basta_stats.png")
            bmp.recycle()
            u
        }
        fireImageShare(ctx, uri, caption, chooser)
    } catch (e: Exception) {
        Log.w(TAG, "shareStatsImage: fallo generando imagen, fallback a texto", e)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, caption)
        }
        ctx.startActivity(Intent.createChooser(send, chooser))
    }
}
