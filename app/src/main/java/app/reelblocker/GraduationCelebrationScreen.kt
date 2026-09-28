package app.reelblocker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Spread
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

/**
 * Pantalla ceremonial del día 21 (graduación). Sustituye al [GraduationDialog] modal
 * por una experiencia full-screen con konfetti procedural, mascota grande,
 * y botón compartir-texto (marketing viral gratis).
 *
 * El botón "Compartir" lanza un Intent.ACTION_SEND con texto plano que
 * incluye días + especie + nombre de la app. Cualquier app de mensajería,
 * red social o email lo puede recibir.
 */
@Composable
fun GraduationCelebrationScreen(
    graduatedSpecies: MascotSpecies,
    daysReached: Int,
    /** Estrellas de [graduatedSpecies] contando esta graduación (1 = primera vez). */
    newStars: Int,
    /** Especies elegibles como siguiente huevo, o null si manda la ruleta. */
    levelUpChoices: List<MascotSpecies>?,
    /** Estrellas por especie contando esta graduación, para el selector. */
    starsAfter: Map<MascotSpecies, Int>,
    onContinue: (chosenNext: MascotSpecies?) -> Unit
) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val accent = graduatedSpecies.accentTint
    val scope = rememberCoroutineScope()
    var sharing by remember { mutableStateOf(false) }
    var chosen by remember(levelUpChoices) {
        mutableStateOf(
            levelUpChoices?.let { Collection.defaultLevelUpPick(it, starsAfter, graduatedSpecies) }
        )
    }
    val mascotSize = if (levelUpChoices != null) 150.dp else 220.dp

    // Back físico = mismo efecto que tap "Continuar" — confirma la celebración.
    BackHandler { onContinue(chosen) }

    val parties = remember(accent) {
        listOf(
            Party(
                speed = 0f,
                maxSpeed = 30f,
                damping = 0.9f,
                angle = Angle.BOTTOM,
                spread = Spread.WIDE,
                colors = listOf(
                    accent.toArgb(),
                    accent.lighten(0.3f).toArgb(),
                    accent.darken(0.15f).toArgb(),
                    0xFFFEF3C7.toInt(),
                    0xFFFCD34D.toInt()
                ),
                position = Position.Relative(0.5, 0.0),
                emitter = Emitter(duration = 1500, TimeUnit.MILLISECONDS).perSecond(120)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Base opaca: cubre el Home y el bottom nav por completo (pantalla
            // ceremonial, no un modal semitransparente).
            .background(MaterialTheme.colorScheme.surface)
            // Tinte de acento sutil arriba que se desvanece hacia surface.
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.12f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top label
            Text(
                text = stringResource(R.string.graduation_celebration_label).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

            // Mascota + textos centrales
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(mascotSize),
                    contentAlignment = Alignment.Center
                ) {
                    MascotCanvas(
                        level = MascotLevel.ADULT,
                        species = graduatedSpecies,
                        animate = true,
                        modifier = Modifier
                            .size(mascotSize)
                            .starAura(newStars, accent)
                    )
                }
                Spacer(Modifier.height(12.dp))
                StarRow(stars = newStars, accent = accent, starSize = 22.dp)
                Spacer(Modifier.height(16.dp))
                val speciesName = stringResource(graduatedSpecies.displayNameRes)
                Text(
                    text = when {
                        newStars == Collection.MAX_STARS ->
                            stringResource(R.string.graduation_title_max, speciesName)
                        newStars in 2 until Collection.MAX_STARS ->
                            stringResource(R.string.graduation_title_levelup, speciesName, newStars)
                        else -> stringResource(R.string.graduation_title, speciesName)
                    },
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.graduation_body, daysReached),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (levelUpChoices != null)
                        stringResource(R.string.graduation_choose_prompt)
                    else
                        stringResource(R.string.graduation_body_secondary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                if (levelUpChoices != null) {
                    Spacer(Modifier.height(14.dp))
                    LevelUpChooser(
                        choices = levelUpChoices,
                        stars = starsAfter,
                        selected = chosen,
                        onSelect = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            chosen = it
                        }
                    )
                }
            }

            // Botones
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val shareLabel = stringResource(R.string.graduation_celebration_share)
                val preparingLabel = stringResource(R.string.share_card_preparing)
                OutlinedButton(
                    onClick = {
                        if (sharing) return@OutlinedButton
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        sharing = true
                        scope.launch {
                            try {
                                // El share ocurre ANTES de consumir la graduación,
                                // así que las stats del periodo siguen intactas.
                                val secs = Stats.readLastDays(ctx, daysReached)
                                    .sumOf { it.counts.total } * Stats.SECONDS_PER_BLOCK
                                val record = Streak.current(ctx).record
                                shareGraduationImage(
                                    ctx = ctx,
                                    species = graduatedSpecies,
                                    daysReached = daysReached,
                                    secondsSaved = secs,
                                    streakRecord = record
                                )
                            } finally {
                                sharing = false
                            }
                        }
                    },
                    enabled = !sharing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (sharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = accent
                        )
                        Spacer(Modifier.size(10.dp))
                        Text(
                            text = preparingLabel,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    } else {
                        Text(
                            text = shareLabel,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onContinue(chosen)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    val next = chosen
                    Text(
                        text = if (next != null)
                            stringResource(R.string.graduation_choose_continue, stringResource(next.displayNameRes))
                        else
                            stringResource(R.string.graduation_celebration_continue),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }

        // Konfetti — overlay encima de todo, no captura toques.
        KonfettiView(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            parties = parties
        )
    }
}

/**
 * Selector del siguiente huevo cuando el pool del usuario está completo: cada
 * especie con sus estrellas actuales; la elegida se resalta con su acento.
 * Las que ya están a ★5 siguen siendo elegibles.
 */
@Composable
private fun LevelUpChooser(
    choices: List<MascotSpecies>,
    stars: Map<MascotSpecies, Int>,
    selected: MascotSpecies?,
    onSelect: (MascotSpecies) -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        choices.forEach { species ->
            val isSelected = species == selected
            val speciesStars = stars[species] ?: 0
            val name = stringResource(species.displayNameRes)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSelected) species.accentTint.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isSelected) species.accentTint else Color.Transparent,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(species) }
                    )
                    .semantics { contentDescription = name }
                    .padding(horizontal = 6.dp, vertical = 8.dp)
            ) {
                MascotCanvas(
                    level = MascotLevel.ADULT,
                    species = species,
                    animate = false,
                    modifier = Modifier
                        .size(48.dp)
                        .starAura(speciesStars, species.accentTint)
                )
                Spacer(Modifier.height(4.dp))
                StarRow(stars = speciesStars, accent = species.accentTint, starSize = 9.dp)
            }
        }
    }
}
