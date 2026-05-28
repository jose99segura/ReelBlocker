package app.reelblocker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onOpenPaywall: () -> Unit
) {
    val ctx = LocalContext.current
    val isPro = Premium.isProLive
    val refreshKey by remember { mutableIntStateOf(0) }
    val today = remember(refreshKey) { Stats.read(ctx) }
    val history = remember(refreshKey) { Stats.readLastDays(ctx, 7) }
    val totalBlocks = remember(refreshKey) { Stats.totalBlocks(ctx) }
    val streak = remember(refreshKey) { Streak.current(ctx) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.stats_title),
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
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // ===== HERO — Hoy =====
            HeroToday(today = today.total)

            // ===== Distribución IG / YT (solo si hay datos) =====
            if (today.total > 0) {
                DistributionStrip(
                    instagram = today.instagram,
                    youtube = today.youtube
                )
            }

            // ===== Gráfico 7 días =====
            val hasHistory = history.any { it.counts.total > 0 }
            WeeklySection(history = history, hasHistory = hasHistory)

            // ===== Pareja de métricas: Tiempo recuperado + Récord =====
            MetricsPair(
                totalBlocks = totalBlocks,
                record = streak.record,
                recordDate = streak.recordDate
            )

            // ===== Pro upsell — sutil, sin card pesada =====
            if (!isPro) {
                ProHint(onClick = onOpenPaywall)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ===== Hero =====

@Composable
private fun HeroToday(today: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.stats_hero_today),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = today.toString(),
            fontSize = 72.sp,
            fontWeight = FontWeight.Black,
            color = if (today == 0)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (today == 0)
                stringResource(R.string.stats_hero_zero_subtitle)
            else stringResource(R.string.stats_hero_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ===== Distribución =====

@Composable
private fun DistributionStrip(instagram: Int, youtube: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DistributionItem(
            label = stringResource(R.string.stats_distribution_instagram),
            value = instagram,
            accent = Color(0xFFE1306C),
            modifier = Modifier.weight(1f)
        )
        VerticalDivider()
        DistributionItem(
            label = stringResource(R.string.stats_distribution_youtube),
            value = youtube,
            accent = Color(0xFFFF0000),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DistributionItem(
    label: String,
    value: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black
        )
    }
}

// ===== Sección semanal =====

@Composable
private fun WeeklySection(history: List<Stats.DayCounts>, hasHistory: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        SectionLabel(stringResource(R.string.stats_section_weekly))
        Spacer(Modifier.height(16.dp))
        if (hasHistory) {
            WeeklyChart(history)
        } else {
            Text(
                text = stringResource(R.string.stats_no_history),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
            )
        }
    }
}

@Composable
private fun WeeklyChart(history: List<Stats.DayCounts>) {
    val primary = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxValue = (history.maxOfOrNull { it.counts.total } ?: 0).coerceAtLeast(1)
    val dayFmt = remember { DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) }
    val labelToday = stringResource(R.string.stats_chart_today)
    val lastIdx = history.lastIndex

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(124.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            history.forEachIndexed { idx, day ->
                val isToday = idx == lastIdx
                val fraction = day.counts.total / maxValue.toFloat()
                val barHeight = (100f * fraction).coerceAtLeast(3f).dp
                val barBrush = if (isToday) {
                    Brush.verticalGradient(listOf(primary, primary.copy(alpha = 0.65f)))
                } else {
                    Brush.verticalGradient(
                        listOf(primary.copy(alpha = 0.40f), primary.copy(alpha = 0.20f))
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.width(34.dp)
                ) {
                    Text(
                        text = day.counts.total.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) primary else labelColor
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp))
                            .background(barBrush)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            history.forEachIndexed { idx, day ->
                val isToday = idx == lastIdx
                Box(modifier = Modifier.width(34.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isToday) labelToday else day.date.format(dayFmt).take(3),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) primary else labelColor
                    )
                }
            }
        }
    }
}

// ===== Métricas pareadas =====

@Composable
private fun MetricsPair(
    totalBlocks: Int,
    record: Int,
    recordDate: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        BigMetric(
            label = stringResource(R.string.stats_metric_time_recovered),
            value = formatRecoveredFull(totalBlocks),
            caption = if (totalBlocks > 0)
                stringResource(R.string.stats_metric_time_recovered_caption, totalBlocks)
            else
                stringResource(R.string.stats_metric_time_recovered_empty)
        )

        if (record > 0) {
            Spacer(Modifier.height(24.dp))
            HorizontalRule()
            Spacer(Modifier.height(24.dp))
            BigMetric(
                label = stringResource(R.string.stats_metric_record),
                value = if (record == 1)
                    stringResource(R.string.stats_metric_record_value_singular, record)
                else
                    stringResource(R.string.stats_metric_record_value_plural, record),
                caption = recordDate?.let { stringResource(R.string.stats_metric_record_caption, formatDate(it)) }
            )
        }
    }
}

@Composable
private fun BigMetric(label: String, value: String, caption: String?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (caption != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ===== Pro hint =====

@Composable
private fun ProHint(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.stats_pro_upsell_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.stats_pro_upsell_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ===== Auxiliares visuales =====

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun HorizontalRule() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(36.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

private fun formatRecoveredFull(blocks: Int): String {
    val totalSeconds = blocks * 30L
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "—"
    }
}

private fun formatDate(isoDate: String): String {
    return try {
        val date = java.time.LocalDate.parse(isoDate)
        val fmt = DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG)
            .withLocale(Locale.getDefault())
        date.format(fmt)
    } catch (_: Exception) {
        isoDate
    }
}
