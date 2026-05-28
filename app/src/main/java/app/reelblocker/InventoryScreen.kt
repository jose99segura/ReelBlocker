package app.reelblocker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    onOpenPaywall: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Refresca al volver a la app (incluido tras consumir una graduación
    // mientras el Bestiario sigue siendo la pantalla actual). Mismo patrón
    // que HomeScreen en MainActivity.kt.
    var refreshKey by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val entries = remember(refreshKey) { Collection.read(ctx) }
    val firstByMember: Map<MascotSpecies, Collection.CollectedMascot> = remember(refreshKey) {
        entries.groupBy { it.species }.mapValues { it.value.first() }
    }
    val uniqueCount = firstByMember.size
    val totalSpecies = MascotSpecies.entries.size
    val currentSpecies = remember(refreshKey) { Collection.currentSpecies(ctx) }
    val streakState = remember(refreshKey) { Streak.current(ctx) }
    val isPro = Premium.isProLive
    val haptic = LocalHapticFeedback.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.nav_inventory),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                },
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            HeroCount(unique = uniqueCount, total = totalSpecies)

            Constellation(
                activeSpecies = currentSpecies,
                activeLevel = streakState.level,
                activeDays = streakState.count,
                firstByMember = firstByMember,
                isPro = isPro,
                onProTap = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onOpenPaywall()
                }
            )

            if (entries.size > uniqueCount) {
                RepeatsHint(repeats = entries.size - uniqueCount)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HeroCount(unique: Int, total: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.inventory_hero_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.inventory_hero_value, unique, total),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = if (unique == 0)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            else
                MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (unique == total)
                stringResource(R.string.inventory_hero_subtitle_complete)
            else
                stringResource(R.string.inventory_hero_subtitle_progress),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Constelación orgánica: el huevo activo en el centro, las otras 4 especies
 * orbitando en posiciones ligeramente aleatorias con un punto de rotación.
 * El seed se calcula una vez por (especie activa + colección) para que los
 * tiles no salten en cada recomposición, pero se reordenen al cambiar de
 * mascota o al desbloquear una nueva.
 */
@Composable
private fun Constellation(
    activeSpecies: MascotSpecies,
    activeLevel: MascotLevel,
    activeDays: Int,
    firstByMember: Map<MascotSpecies, Collection.CollectedMascot>,
    isPro: Boolean,
    onProTap: () -> Unit
) {
    // Si la especie activa YA está graduada (firstByMember la contiene), el
    // usuario está en una repetición. En ese caso ELLA TAMBIÉN debe aparecer
    // como satélite (con su fecha original) — si no, su graduación previa
    // queda invisible. Si es ciclo virgen, mantenemos los 4 satélites del
    // diseño original (no metemos un slot "?" redundante junto al centro).
    val activeIsRepeat = activeSpecies in firstByMember
    val satellitesSpecies = if (activeIsRepeat) {
        MascotSpecies.entries.toList()
    } else {
        MascotSpecies.entries.filter { it != activeSpecies }
    }

    // Anclas: 4 (quincuncio asimétrico) para ciclo virgen, 5 (pentagonal)
    // cuando la activa es repetición.
    val anchors = if (activeIsRepeat) {
        listOf(
            DpPoint(x = 0.dp, y = (-135).dp),
            DpPoint(x = 130.dp, y = (-45).dp),
            DpPoint(x = 85.dp, y = 115.dp),
            DpPoint(x = (-85).dp, y = 115.dp),
            DpPoint(x = (-130).dp, y = (-45).dp)
        )
    } else {
        listOf(
            DpPoint(x = (-100).dp, y = (-115).dp),
            DpPoint(x = 100.dp, y = (-105).dp),
            DpPoint(x = (-110).dp, y = 95.dp),
            DpPoint(x = 105.dp, y = 110.dp)
        )
    }

    val placements = remember(activeSpecies, firstByMember.size, activeIsRepeat) {
        val r = Random(System.nanoTime())
        satellitesSpecies.shuffled(r).mapIndexed { i, sp ->
            val base = anchors[i]
            TilePlacement(
                species = sp,
                x = base.x + (r.nextFloat() * 24f - 12f).dp,
                y = base.y + (r.nextFloat() * 24f - 12f).dp,
                rotation = r.nextFloat() * 10f - 5f
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Active tile en el centro — el huevo/criatura en curso, con su
        // nivel actual. Si es una repetición, la versión graduada ya está
        // visible como satélite (no se duplica info en el centro).
        ActiveFeaturedTile(
            species = activeSpecies,
            level = activeLevel,
            days = activeDays
        )

        // Tiles satélite — colocados con offset + rotación aleatoria.
        placements.forEach { p ->
            val collected = firstByMember[p.species]
            SatelliteTile(
                species = p.species,
                collected = collected,
                isPro = isPro,
                onProTap = onProTap,
                modifier = Modifier
                    .offset(x = p.x, y = p.y)
                    .graphicsLayer { rotationZ = p.rotation }
            )
        }
    }
}

private data class DpPoint(val x: Dp, val y: Dp)

private data class TilePlacement(
    val species: MascotSpecies,
    val x: Dp,
    val y: Dp,
    val rotation: Float
)

@Composable
private fun ActiveFeaturedTile(
    species: MascotSpecies,
    level: MascotLevel,
    days: Int
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            species.accentTint.copy(alpha = 0.32f),
                            species.accentTint.copy(alpha = 0.08f)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    color = species.accentTint,
                    shape = RoundedCornerShape(28.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            MascotCanvas(
                level = level,
                species = species,
                animate = true,
                modifier = Modifier.size(126.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(species.displayNameRes),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "$days / ${MascotLevel.ADULT.minDays}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = species.accentTint
        )
    }
}

@Composable
private fun SatelliteTile(
    species: MascotSpecies,
    collected: Collection.CollectedMascot?,
    isPro: Boolean,
    onProTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unlocked = collected != null
    // Pro-locked: especie Pro y el usuario es free. El usuario VE la especie
    // (silueta tinted + nombre) pero con candado — comunica "esto existe,
    // está disponible, paga para coleccionarla".
    val proLocked = !isPro && species.isPro && !unlocked
    val lockedCd = stringResource(R.string.cd_inventory_slot_locked_pro, stringResource(species.displayNameRes))

    val tileModifier = if (proLocked) {
        modifier
            .clickable(onClick = onProTap)
            .semantics { contentDescription = lockedCd }
    } else modifier

    Column(
        modifier = tileModifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    when {
                        unlocked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                        proLocked -> species.accentTint.copy(alpha = 0.10f)
                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f)
                    }
                )
                .then(
                    if (proLocked) Modifier.border(
                        width = 1.dp,
                        color = species.accentTint.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(20.dp)
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                unlocked -> MascotCanvas(
                    level = MascotLevel.ADULT,
                    species = species,
                    animate = false,
                    modifier = Modifier.size(82.dp)
                )
                proLocked -> {
                    // Silueta tinted al 35% para que se vea la especie sin
                    // entregarla. Lock badge superpuesto arriba-derecha.
                    MascotCanvas(
                        level = MascotLevel.ADULT,
                        species = species,
                        animate = false,
                        modifier = Modifier
                            .size(82.dp)
                            .alpha(0.35f)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(species.accentTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
                else -> Text(
                    text = "?",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = when {
                unlocked -> stringResource(species.displayNameRes)
                proLocked -> stringResource(species.displayNameRes)
                else -> stringResource(R.string.inventory_slot_locked_name)
            },
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                unlocked -> MaterialTheme.colorScheme.onSurface
                proLocked -> species.accentTint
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            },
            textAlign = TextAlign.Center
        )
        if (collected != null) {
            Text(
                text = formatAcquired(collected.acquiredDate),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else if (proLocked) {
            Text(
                text = stringResource(R.string.inventory_slot_pro_caption),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RepeatsHint(repeats: Int) {
    Text(
        text = if (repeats == 1)
            stringResource(R.string.inventory_repeats_singular)
        else
            stringResource(R.string.inventory_repeats_plural, repeats),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        textAlign = TextAlign.Center
    )
}

private fun formatAcquired(isoDate: String): String {
    return try {
        val date = LocalDate.parse(isoDate)
        val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
        date.format(fmt)
    } catch (_: Exception) {
        isoDate
    }
}
