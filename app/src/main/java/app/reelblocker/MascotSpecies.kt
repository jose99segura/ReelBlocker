package app.reelblocker

import androidx.compose.ui.graphics.Color

/** Tier de monetización de una especie. */
enum class Tier { FREE, PRO }

/**
 * Especies coleccionables. Cada una se renderiza con sprites webp (render 3D)
 * por fase de evolución vía [spriteRes]; ya no se dibuja con Canvas.
 */
enum class MascotSpecies(
    val id: String,
    @androidx.annotation.StringRes val displayNameRes: Int,
    /** Color de acento usado en UI (inventario, racha, share, widget…). */
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
     * Sprite webp (render 3D) para esta especie en una fase de evolución.
     * Única fuente de verdad del arte; la consumen [MascotCanvas] y
     * [renderMascotBitmap].
     */
    @androidx.annotation.DrawableRes
    fun spriteRes(level: MascotLevel): Int = when (this) {
        CLASICA -> when (level) {
            MascotLevel.EGG -> R.drawable.mascot_clasica_egg
            MascotLevel.CRACKING -> R.drawable.mascot_clasica_cracking
            MascotLevel.HATCHLING -> R.drawable.mascot_clasica_hatchling
            MascotLevel.ADULT -> R.drawable.mascot_clasica_adult
        }
        DRAGON -> when (level) {
            MascotLevel.EGG -> R.drawable.mascot_dragon_egg
            MascotLevel.CRACKING -> R.drawable.mascot_dragon_cracking
            MascotLevel.HATCHLING -> R.drawable.mascot_dragon_hatchling
            MascotLevel.ADULT -> R.drawable.mascot_dragon_adult
        }
        TORTUGA -> when (level) {
            MascotLevel.EGG -> R.drawable.mascot_tortuga_egg
            MascotLevel.CRACKING -> R.drawable.mascot_tortuga_cracking
            MascotLevel.HATCHLING -> R.drawable.mascot_tortuga_hatchling
            MascotLevel.ADULT -> R.drawable.mascot_tortuga_adult
        }
        LOBO -> when (level) {
            MascotLevel.EGG -> R.drawable.mascot_lobo_egg
            MascotLevel.CRACKING -> R.drawable.mascot_lobo_cracking
            MascotLevel.HATCHLING -> R.drawable.mascot_lobo_hatchling
            MascotLevel.ADULT -> R.drawable.mascot_lobo_adult
        }
        BUHO -> when (level) {
            MascotLevel.EGG -> R.drawable.mascot_buho_egg
            MascotLevel.CRACKING -> R.drawable.mascot_buho_cracking
            MascotLevel.HATCHLING -> R.drawable.mascot_buho_hatchling
            MascotLevel.ADULT -> R.drawable.mascot_buho_adult
        }
    }

    /** Huevo (nivel EGG) de la especie. Atajo de [spriteRes]. */
    @get:androidx.annotation.DrawableRes
    val eggRes: Int get() = spriteRes(MascotLevel.EGG)

    companion object {
        fun fromIdOrNull(id: String?): MascotSpecies? =
            entries.firstOrNull { it.id == id }

        val DEFAULT: MascotSpecies = CLASICA

        fun freeSpecies(): List<MascotSpecies> = entries.filter { it.tier == Tier.FREE }
        fun proSpecies(): List<MascotSpecies> = entries.filter { it.tier == Tier.PRO }
    }
}
