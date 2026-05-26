package app.reelblocker

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen() {
    val ctx = LocalContext.current
    val entries = remember { Collection.read(ctx) }
    val firstByMember: Map<MascotSpecies, Collection.CollectedMascot> = remember {
        entries.groupBy { it.species }.mapValues { it.value.first() }
    }
    val uniqueCount = firstByMember.size
    val totalSpecies = MascotSpecies.entries.size
    val currentSpecies = remember { Collection.currentSpecies(ctx) }
    val streakState = remember { Streak.current(ctx) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Inventario",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            HeroCount(unique = uniqueCount, total = totalSpecies)

            ActiveSpeciesRow(
                species = currentSpecies,
                level = streakState.level,
                count = streakState.count
            )

            if (uniqueCount == 0) {
                EmptyState()
            } else {
                SpeciesGrid(firstByMember = firstByMember)
                if (entries.size > uniqueCount) {
                    RepeatsHint(repeats = entries.size - uniqueCount)
                }
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

@Composable
private fun ActiveSpeciesRow(species: MascotSpecies, level: MascotLevel, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            MascotCanvas(
                level = level,
                species = species,
                animate = false,
                modifier = Modifier.size(52.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.inventory_active_now),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.inventory_active_species_level,
                    stringResource(species.displayNameRes),
                    stringResource(level.displayNameRes).lowercase()
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (count == 0)
                    stringResource(R.string.inventory_active_waiting)
                else
                    stringResource(R.string.inventory_active_day_progress, count, MascotLevel.ADULT.minDays),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            MascotCanvas(
                level = MascotLevel.EGG,
                species = MascotSpecies.CLASICA,
                animate = false,
                modifier = Modifier.size(120.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.inventory_empty_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.inventory_empty_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SpeciesGrid(firstByMember: Map<MascotSpecies, Collection.CollectedMascot>) {
    // Grid 2-cols hecho a mano (sin LazyVerticalGrid para no añadir scrolls anidados).
    val species = MascotSpecies.entries
    val rows = species.chunked(2)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { sp ->
                    val collected = firstByMember[sp]
                    SpeciesSlot(
                        species = sp,
                        collected = collected,
                        modifier = Modifier
                            .weight(1f)
                    )
                }
                if (row.size == 1) {
                    // Hueco para mantener la cuadrícula alineada.
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SpeciesSlot(
    species: MascotSpecies,
    collected: Collection.CollectedMascot?,
    modifier: Modifier = Modifier
) {
    val unlocked = collected != null
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (unlocked)
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (unlocked) {
                MascotCanvas(
                    level = MascotLevel.ADULT,
                    species = species,
                    animate = false,
                    modifier = Modifier.size(110.dp)
                )
            } else {
                Text(
                    text = "?",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (unlocked) stringResource(species.displayNameRes)
                   else stringResource(R.string.inventory_slot_locked_name),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (unlocked)
                MaterialTheme.colorScheme.onSurface
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            textAlign = TextAlign.Center
        )
        if (collected != null) {
            Text(
                text = formatAcquired(collected.acquiredDate),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = stringResource(R.string.inventory_slot_locked_caption),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
        // Locale.getDefault() respeta el idioma del sistema (ES o EN).
        val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
        date.format(fmt)
    } catch (_: Exception) {
        isoDate
    }
}
