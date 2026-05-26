package app.reelblocker

import android.content.Context

/**
 * Frases rotativas que aparecen en la home. La lista vive en
 * `res/values/strings.xml` y `res/values-en/strings.xml` (array
 * `tips_quotes`) para que se traduzcan automáticamente.
 *
 * Mezcla de:
 * - Datos reales sobre consumo de vídeo corto
 * - Alternativas concretas y sencillas
 * - Reflexiones sin moralina
 *
 * Fuentes principales:
 * - Mark, G. (UC Irvine): tiempo de recuperación de atención tras
 *   distracción (~23 min).
 * - Data.ai / Statista 2024: tiempos medios de TikTok / Reels.
 * - Sleep Foundation: efecto de pantalla nocturna en sueño profundo.
 */
object Tips {

    private fun all(ctx: Context): Array<String> =
        ctx.resources.getStringArray(R.array.tips_quotes)

    fun random(ctx: Context): String = all(ctx).random()

    /** Devuelve N frases distintas (sin repetir). */
    fun randomDistinct(ctx: Context, count: Int): List<String> =
        all(ctx).toList().shuffled().take(count)
}
