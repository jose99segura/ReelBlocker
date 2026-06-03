package app.reelblocker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onOpenPaywall: () -> Unit
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isPro = Premium.isProLive
    var refreshKey by remember { mutableIntStateOf(0) }

    // Refrescar las métricas en cada ON_RESUME: al volver a esta pestaña tras
    // bloquear reels los contadores deben reflejar los nuevos datos.
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val today = remember(refreshKey) { Stats.read(ctx) }
    val history7 = remember(refreshKey) { Stats.readLastDays(ctx, 7) }
    val history30 = remember(refreshKey) { Stats.readLastDays(ctx, 30) }
    val totalBlocks = remember(refreshKey) { Stats.totalBlocks(ctx) }
    val streak = remember(refreshKey) { Streak.current(ctx) }

    // Pestaña de periodo y día seleccionado del gráfico (null = hoy).
    var period by remember { mutableStateOf(StatsPeriod.WEEK) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

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

            // ===== Actividad: pestañas Semana/Mes + gráfico + detalle del día =====
            ActivityCard(
                period = period,
                isPro = isPro,
                history7 = history7,
                history30 = history30,
                selectedDate = selectedDate,
                onSelectWeek = { period = StatsPeriod.WEEK; selectedDate = null },
                onSelectMonth = {
                    if (isPro) { period = StatsPeriod.MONTH; selectedDate = null } else onOpenPaywall()
                },
                onSelectDay = { selectedDate = it }
            )

            // ===== Pareja de métricas: Tiempo recuperado + Récord =====
            MetricsPair(
                totalBlocks = totalBlocks,
                record = streak.record,
                recordDate = streak.recordDate
            )

            // ===== Estadísticas avanzadas (Pro) o teaser (Free) =====
            if (isPro) {
                AdvancedStatsSection(
                    history30 = history30,
                    lifetime = remember(refreshKey) { Stats.lifetimeBlocks(ctx) },
                    species = remember(refreshKey) { Collection.currentSpecies(ctx) },
                    level = streak.level,
                    streakRecord = streak.record
                )
            } else {
                ProHint(onClick = onOpenPaywall)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

enum class StatsPeriod { WEEK, MONTH }

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
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
private fun DistributionStrip(instagram: Int, youtube: Int, tiktok: Int) {
    // Colores de marca; se ajustan al tema para mantener contraste legible.
    val dark = isSystemInDarkTheme()
    val instagramAccent = Color(0xFFE1306C).let { if (dark) it.lighten(0.30f) else it }
    val youtubeAccent = Color(0xFFFF0000).let { if (dark) it.lighten(0.30f) else it }
    // Cian de TikTok: muy claro de base, se oscurece en light para contraste.
    val tiktokAccent = Color(0xFF25F4EE).let { if (dark) it else it.darken(0.35f) }
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
            accent = instagramAccent,
            modifier = Modifier.weight(1f)
        )
        VerticalDivider()
        DistributionItem(
            label = stringResource(R.string.stats_distribution_youtube),
            value = youtube,
            accent = youtubeAccent,
            modifier = Modifier.weight(1f)
        )
        VerticalDivider()
        DistributionItem(
            label = stringResource(R.string.stats_distribution_tiktok),
            value = tiktok,
            accent = tiktokAccent,
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

// ===== Actividad: pestañas + gráfico + detalle del día =====

@Composable
private fun ActivityCard(
    period: StatsPeriod,
    isPro: Boolean,
    history7: List<Stats.DayCounts>,
    history30: List<Stats.DayCounts>,
    selectedDate: LocalDate?,
    onSelectWeek: () -> Unit,
    onSelectMonth: () -> Unit,
    onSelectDay: (LocalDate) -> Unit
) {
    val data = if (period == StatsPeriod.MONTH) history30 else history7
    val selectedDay = data.find { it.date == selectedDate } ?: data.last()
    val hasHistory = data.any { it.counts.total > 0 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        PeriodTabs(
            period = period,
            isPro = isPro,
            onWeek = onSelectWeek,
            onMonth = onSelectMonth
        )
        Spacer(Modifier.height(20.dp))
        if (hasHistory) {
            BarChart(
                history = data,
                selectedDate = selectedDay.date,
                compact = period == StatsPeriod.MONTH,
                onSelect = onSelectDay
            )
            Spacer(Modifier.height(20.dp))
            SelectedDayDetail(selectedDay)
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
private fun PeriodTabs(
    period: StatsPeriod,
    isPro: Boolean,
    onWeek: () -> Unit,
    onMonth: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TabPill(
            text = stringResource(R.string.stats_tab_week),
            selected = period == StatsPeriod.WEEK,
            locked = false,
            onClick = onWeek,
            modifier = Modifier.weight(1f)
        )
        TabPill(
            text = stringResource(R.string.stats_tab_month),
            selected = period == StatsPeriod.MONTH,
            locked = !isPro,
            onClick = onMonth,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabPill(
    text: String,
    selected: Boolean,
    locked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = fg
        )
        if (locked) {
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Gráfico de barras unificado. [compact]=false (semana): barras anchas con
 * número y etiqueta de día. [compact]=true (mes): 30 barras finas que llenan el
 * ancho, sin etiquetas por barra. La barra seleccionada se resalta; tocar una
 * barra invoca [onSelect].
 */
@Composable
private fun BarChart(
    history: List<Stats.DayCounts>,
    selectedDate: LocalDate,
    compact: Boolean,
    onSelect: (LocalDate) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxValue = (history.maxOfOrNull { it.counts.total } ?: 0).coerceAtLeast(1)
    val dayFmt = remember { DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) }
    val labelToday = stringResource(R.string.stats_chart_today)
    val today = remember { LocalDate.now() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (compact) Arrangement.spacedBy(3.dp) else Arrangement.SpaceEvenly
        ) {
            history.forEach { day ->
                val isSelected = day.date == selectedDate
                val fraction = day.counts.total / maxValue.toFloat()
                val barHeight = (104f * fraction).coerceAtLeast(if (compact) 2f else 4f).dp
                val colModifier = (if (compact) Modifier.weight(1f) else Modifier.width(34.dp))
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onSelect(day.date) }
                Column(
                    modifier = colModifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (!compact) {
                        Text(
                            text = day.counts.total.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) primary else labelColor
                        )
                        Spacer(Modifier.height(3.dp))
                    }
                    Box(
                        modifier = Modifier
                            .then(if (compact) Modifier.fillMaxWidth() else Modifier.width(22.dp))
                            .height(barHeight)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (isSelected) primary else primary.copy(alpha = 0.30f))
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        if (compact) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.stats_chart_30d_start),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
                Text(
                    text = labelToday,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = primary
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                history.forEach { day ->
                    val isToday = day.date == today
                    Box(modifier = Modifier.width(34.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isToday) labelToday else day.date.format(dayFmt).take(3),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (day.date == selectedDate) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.date == selectedDate) primary else labelColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDayDetail(day: Stats.DayCounts) {
    val today = remember { LocalDate.now() }
    val isToday = day.date == today
    val dateLabel = if (isToday) stringResource(R.string.stats_chart_today) else formatDate(day.date.toString())

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.stats_day_blocks, day.counts.total),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (day.counts.total > 0) {
            Spacer(Modifier.height(12.dp))
            DistributionStrip(
                instagram = day.counts.instagram,
                youtube = day.counts.youtube,
                tiktok = day.counts.tiktok
            )
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
            value = formatRecoveredFull(totalBlocks.toLong()),
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
                text = stringResource(R.string.stats_advanced_locked_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.stats_advanced_locked_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ===== Estadísticas avanzadas (Pro) =====

@Composable
private fun AdvancedStatsSection(
    history30: List<Stats.DayCounts>,
    lifetime: Long,
    species: MascotSpecies,
    level: MascotLevel,
    streakRecord: Int
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var sharing by remember { mutableStateOf(false) }

    val sum30 = history30.sumOf { it.counts.total }
    val avg = sum30 / 30
    val best = history30.maxByOrNull { it.counts.total }
    // Últimos 7 días vs los 7 anteriores (índices 16..22 de los 30 ascendentes).
    val last7 = history30.takeLast(7).sumOf { it.counts.total }
    val prev7 = history30.drop(16).take(7).sumOf { it.counts.total }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Cabecera con sello Pro.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.stats_section_advanced),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Totales de por vida.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CompactMetric(
                label = stringResource(R.string.stats_metric_lifetime_blocks),
                value = lifetime.toString(),
                modifier = Modifier.weight(1f)
            )
            CompactMetric(
                label = stringResource(R.string.stats_metric_lifetime_time),
                value = formatRecoveredFull(lifetime),
                modifier = Modifier.weight(1f)
            )
        }

        // Media diaria + mejor día.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CompactMetric(
                label = stringResource(R.string.stats_metric_daily_avg),
                value = avg.toString(),
                modifier = Modifier.weight(1f)
            )
            CompactMetric(
                label = stringResource(R.string.stats_metric_best_day),
                value = (best?.counts?.total ?: 0).toString(),
                caption = best?.takeIf { it.counts.total > 0 }?.let { formatDate(it.date.toString()) },
                modifier = Modifier.weight(1f)
            )
        }

        TrendLine(current = last7, previous = prev7)

        // Export como imagen tipo tarjeta.
        Button(
            onClick = {
                if (sharing) return@Button
                sharing = true
                scope.launch {
                    try {
                        shareStatsImage(
                            ctx = ctx,
                            species = species,
                            level = level,
                            lifetimeBlocks = lifetime,
                            streakRecord = streakRecord
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
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.share_card_preparing),
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = stringResource(R.string.stats_export_button),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun CompactMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
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

@Composable
private fun TrendLine(current: Int, previous: Int) {
    // Sin semana previa con datos no hay comparación posible.
    if (previous <= 0) return
    val pct = ((current - previous) * 100f / previous).toInt()
    val text = when {
        pct > 0 -> stringResource(R.string.stats_trend_up, pct)
        pct < 0 -> stringResource(R.string.stats_trend_down, -pct)
        else -> stringResource(R.string.stats_trend_flat)
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
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

private fun formatRecoveredFull(blocks: Long): String {
    val totalSeconds = blocks * Stats.SECONDS_PER_BLOCK
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
