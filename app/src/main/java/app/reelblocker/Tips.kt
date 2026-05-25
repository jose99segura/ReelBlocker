package app.reelblocker

import android.content.Context

/**
 * Frases rotativas que aparecen en la home. Mezcla de:
 * - Datos reales sobre consumo de vídeo corto
 * - Alternativas concretas y sencillas
 * - Reflexiones sin moralina
 *
 * Las frases viven en `res/values/strings.xml` (y por-locale en
 * `res/values-es/strings.xml`) como string-arrays — Android elige la lista
 * correcta segun el locale del sistema sin que nada de aqui cambie.
 *
 * Fuentes principales:
 * - Mark, G. (UC Irvine): tiempo de recuperación de atención tras
 *   distracción (~23 min).
 * - Data.ai / Statista 2024: tiempos medios de TikTok / Reels.
 * - Sleep Foundation: efecto de pantalla nocturna en sueño profundo.
 */
object Tips {

    private fun loadAll(ctx: Context): List<String> {
        val res = ctx.resources
        return res.getStringArray(R.array.tips_stats).toList() +
            res.getStringArray(R.array.tips_alternatives).toList() +
            res.getStringArray(R.array.tips_reflections).toList()
    }

    fun random(ctx: Context): String = loadAll(ctx).random()

    /** Devuelve N frases distintas (sin repetir). */
    fun randomDistinct(ctx: Context, count: Int): List<String> =
        loadAll(ctx).shuffled().take(count)
}
