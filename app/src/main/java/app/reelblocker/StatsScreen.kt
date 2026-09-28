package app.reelblocker

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

// ============================================================================
// Estadísticas — cuatro pestañas. "Resumen" es gratis; "Tendencias", "Apps" y
// "Hábitos" son Pro. Un usuario Free ve esas pestañas con una vista previa
// difuminada (datos de ejemplo, nunca los suyos) y una tarjeta de desbloqueo.
// ============================================================================

enum class StatsTab(@StringRes val labelRes: Int, val proOnly: Boolean) {
    OVERVIEW(R.string.stats_tab_overview, false),
    TRENDS(R.string.stats_tab_trends, true),
    APPS(R.string.stats_tab_apps, true),
    HABITS(R.string.stats_tab_habits, true)
}

enum class StatsPeriod(val days: Int) { WEEK(7), MONTH(30) }

/** Instantánea de todo lo que pintan las pestañas; se recalcula en ON_RESUME. */
private class StatsData(
    val today: Stats.Counts,
    val history30: List<Stats.DayCounts>,
    val lifetime: Long,
    val hourly: IntArray,
    val record: Int,
    val recordDate: String?
) {
    val yesterday: Int get() = history30.getOrNull(history30.size - 2)?.counts?.total ?: 0
    val sum30: Int get() = history30.sumOf { it.counts.total }
}

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

    val data = remember(refreshKey) {
        val streak = Streak.current(ctx)
        StatsData(
            today = Stats.read(ctx),
            history30 = Stats.readLastDays(ctx, 30),
            lifetime = Stats.lifetimeBlocks(ctx),
            hourly = Stats.hourlyBlocks(ctx),
            record = streak.record,
            recordDate = streak.recordDate
        )
    }
    var tab by rememberSaveable { mutableStateOf(StatsTab.OVERVIEW) }

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
        ) {
            StatsTabRow(selected = tab, isPro = isPro, onSelect = { tab = it })
            Crossfade(targetState = tab, label = "statsTab", modifier = Modifier.weight(1f)) { t ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when {
                        t == StatsTab.OVERVIEW -> OverviewTab(data, isPro, onOpenPaywall)
                        !isPro -> LockedTab(t, onOpenPaywall)
                        t == StatsTab.TRENDS -> TrendsTab(data)
                        t == StatsTab.APPS -> AppsTab(data)
                        else -> HabitsTab(data)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsTabRow(selected: StatsTab, isPro: Boolean, onSelect: (StatsTab) -> Unit) {
    ScrollableTabRow(
        selectedTabIndex = selected.ordinal,
        edgePadding = CardMargin,
        containerColor = Color.Transparent,
        divider = {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        }
    ) {
        StatsTab.values().forEach { t ->
            val isSelected = t == selected
            Tab(
                selected = isSelected,
                onClick = { onSelect(t) },
                selectedContentColor = MaterialTheme.colorScheme.primary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(t.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                        if (t.proOnly && !isPro) {
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            )
        }
    }
}

// ===== Pestaña: Resumen (gratis) =====

@Composable
private fun OverviewTab(data: StatsData, isPro: Boolean, onOpenPaywall: () -> Unit) {
    TodayHeroCard(today = data.today, yesterday = data.yesterday)

    var selected by remember { mutableStateOf<LocalDate?>(null) }
    ActivityChartCard(
        title = stringResource(R.string.stats_section_weekly),
        history = data.history30.takeLast(7),
        compact = false,
        selectedDate = selected,
        onSelect = { selected = it }
    )

    KpiRow {
        KpiTile(
            icon = Icons.Outlined.HourglassBottom,
            label = stringResource(R.string.stats_metric_time_recovered),
            value = formatRecoveredFull(data.sum30.toLong()),
            caption = if (data.sum30 > 0)
                stringResource(R.string.stats_metric_time_recovered_caption, data.sum30)
            else stringResource(R.string.stats_metric_time_recovered_empty),
            modifier = Modifier.weight(1f)
        )
        KpiTile(
            icon = Icons.Outlined.EmojiEvents,
            label = stringResource(R.string.stats_metric_record),
            value = if (data.record == 1)
                stringResource(R.string.stats_metric_record_value_singular, data.record)
            else stringResource(R.string.stats_metric_record_value_plural, data.record),
            caption = data.recordDate?.let { stringResource(R.string.stats_metric_record_caption, formatDate(it)) },
            modifier = Modifier.weight(1f)
        )
    }

    if (isPro) ExportStatsButton(lifetime = data.lifetime, record = data.record)
    else ProTeaserCard(onClick = onOpenPaywall)
}

@Composable
private fun TodayHeroCard(today: Stats.Counts, yesterday: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CardMargin),
        shape = CardShape,
        color = base
    ) {
        Column(
            modifier = Modifier
                .background(Brush.verticalGradient(listOf(primary.copy(alpha = 0.16f), base)))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CardLabel(stringResource(R.string.stats_hero_today), Modifier.weight(1f))
                val diff = today.total - yesterday
                if (today.total > 0 || yesterday > 0) {
                    DeltaChip(
                        when {
                            diff > 0 -> stringResource(R.string.stats_vs_yesterday_up, diff)
                            diff < 0 -> stringResource(R.string.stats_vs_yesterday_down, -diff)
                            else -> stringResource(R.string.stats_vs_yesterday_same)
                        }
                    )
                }
            }
            Text(
                text = today.total.toString(),
                fontSize = 64.sp,
                fontWeight = FontWeight.Black,
                color = if (today.total == 0)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (today.total == 0)
                    stringResource(R.string.stats_hero_zero_subtitle)
                else stringResource(R.string.stats_hero_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (today.total > 0) {
                Spacer(Modifier.height(16.dp))
                DistributionStrip(today)
            }
        }
    }
}

@Composable
private fun ExportStatsButton(lifetime: Long, record: Int) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var sharing by remember { mutableStateOf(false) }
    FilledTonalButton(
        onClick = {
            if (sharing) return@FilledTonalButton
            sharing = true
            scope.launch {
                try {
                    shareStatsImage(
                        ctx = ctx,
                        species = Collection.currentSpecies(ctx),
                        level = Streak.current(ctx).level,
                        lifetimeBlocks = lifetime,
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
            .padding(horizontal = CardMargin)
            .height(52.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        if (sharing) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.share_card_preparing), fontWeight = FontWeight.SemiBold)
        } else {
            Icon(Icons.Outlined.IosShare, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.stats_export_button), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProTeaserCard(onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CardMargin),
        shape = CardShape,
        color = base
    ) {
        Column(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(primary.copy(alpha = 0.22f), base)))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Outlined.AutoAwesome)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.stats_advanced_locked_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                ProBadge()
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.stats_advanced_locked_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(stringResource(R.string.stats_teaser_cta), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ===== Pestaña: Tendencias (Pro) =====

@Composable
private fun TrendsTab(data: StatsData) {
    var period by remember { mutableStateOf(StatsPeriod.WEEK) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    val history = data.history30.takeLast(period.days)
    val total = history.sumOf { it.counts.total }
    val avg = total / period.days.toFloat()
    val best = history.maxByOrNull { it.counts.total }?.takeIf { it.counts.total > 0 }

    PeriodTabs(period = period, onSelect = { period = it; selected = null })

    ActivityChartCard(
        title = stringResource(
            if (period == StatsPeriod.WEEK) R.string.stats_section_weekly else R.string.stats_section_monthly
        ),
        history = history,
        compact = period == StatsPeriod.MONTH,
        selectedDate = selected,
        onSelect = { selected = it },
        average = avg,
        // Solo hay 30 días de historial: la comparación con el periodo
        // anterior solo es posible para la semana.
        delta = if (period == StatsPeriod.WEEK) {
            trendText(total, data.history30.dropLast(7).takeLast(7).sumOf { it.counts.total })
        } else null
    )

    KpiRow {
        KpiTile(
            icon = Icons.Outlined.ShowChart,
            label = stringResource(R.string.stats_metric_daily_avg),
            value = formatAverage(avg),
            modifier = Modifier.weight(1f)
        )
        KpiTile(
            icon = Icons.Outlined.Star,
            label = stringResource(R.string.stats_metric_best_day),
            value = (best?.counts?.total ?: 0).toString(),
            caption = best?.let { formatDate(it.date.toString()) },
            modifier = Modifier.weight(1f)
        )
    }
    KpiRow {
        KpiTile(
            icon = Icons.Outlined.Functions,
            label = stringResource(R.string.stats_metric_lifetime_blocks),
            value = data.lifetime.toString(),
            modifier = Modifier.weight(1f)
        )
        KpiTile(
            icon = Icons.Outlined.HourglassBottom,
            label = stringResource(R.string.stats_metric_lifetime_time),
            value = formatRecoveredFull(data.lifetime),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PeriodTabs(period: StatsPeriod, onSelect: (StatsPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CardMargin)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TabPill(
            text = stringResource(R.string.stats_tab_week),
            selected = period == StatsPeriod.WEEK,
            onClick = { onSelect(StatsPeriod.WEEK) },
            modifier = Modifier.weight(1f)
        )
        TabPill(
            text = stringResource(R.string.stats_tab_month),
            selected = period == StatsPeriod.MONTH,
            onClick = { onSelect(StatsPeriod.MONTH) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = fg
        )
    }
}

// ===== Pestaña: Apps (Pro) =====

private class AppBreakdown(
    val name: String,
    val color: Color,
    val series: List<Int>
) {
    val total: Int = series.sum()
    val last7: Int = series.takeLast(7).sum()
    val prev7: Int = series.dropLast(7).takeLast(7).sum()
}

@Composable
private fun AppsTab(data: StatsData) {
    val apps = listOf(
        AppBreakdown(
            stringResource(R.string.stats_distribution_instagram),
            appAccent(Stats.PKG_INSTAGRAM),
            data.history30.map { it.counts.instagram }
        ),
        AppBreakdown(
            stringResource(R.string.stats_distribution_youtube),
            appAccent(Stats.PKG_YOUTUBE),
            data.history30.map { it.counts.youtube }
        ),
        AppBreakdown(
            stringResource(R.string.stats_distribution_tiktok),
            appAccent(Stats.PKG_TIKTOK),
            data.history30.map { it.counts.tiktok }
        )
    ).sortedByDescending { it.total }
    val grand = apps.sumOf { it.total }

    StatCard {
        CardLabel(stringResource(R.string.stats_section_by_app_30))
        Text(
            text = stringResource(R.string.stats_day_blocks, grand),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(16.dp))
        if (grand == 0) {
            EmptyText(stringResource(R.string.stats_apps_empty))
        } else {
            // Barra apilada con la cuota de cada app.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp)),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                apps.filter { it.total > 0 }.forEach { app ->
                    Box(
                        Modifier
                            .weight(app.total.toFloat())
                            .fillMaxHeight()
                            .background(app.color)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            apps.forEach { app ->
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ColorDot(app.color)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = app.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${percent(app.total, grand)}%",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (grand > 0) apps.forEach { AppDetailCard(it, grand) }
}

@Composable
private fun AppDetailCard(app: AppBreakdown, grand: Int) {
    StatCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ColorDot(app.color, size = 12.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = app.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            trendText(app.last7, app.prev7)?.let { DeltaChip(it) }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = app.total.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.stats_apps_share, percent(app.total, grand)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 5.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        MiniBars(
            values = app.series.map { it.toFloat() },
            color = app.color,
            height = 40.dp,
            highlight = app.series.lastIndex
        )
        Spacer(Modifier.height(6.dp))
        AxisLabels(listOf(stringResource(R.string.stats_chart_30d_start), stringResource(R.string.stats_chart_today)))
    }
}

// ===== Pestaña: Hábitos (Pro) =====

@Composable
private fun HabitsTab(data: StatsData) {
    val primary = MaterialTheme.colorScheme.primary
    val locale = Locale.getDefault()

    // Media por día de la semana (lunes..domingo) sobre los últimos 30 días.
    val weekdayAvg = remember(data) {
        val sums = IntArray(7)
        val counts = IntArray(7)
        data.history30.forEach {
            val i = it.date.dayOfWeek.value - 1
            sums[i] += it.counts.total
            counts[i]++
        }
        List(7) { if (counts[it] == 0) 0f else sums[it] / counts[it].toFloat() }
    }
    val toughest = weekdayAvg.indices.maxByOrNull { weekdayAvg[it] }?.takeIf { weekdayAvg[it] > 0f }

    val hourly = data.hourly
    val hourlySum = hourly.sum()
    val peakHour = hourly.indices.maxByOrNull { hourly[it] }?.takeIf { hourlySum > 0 }
    val nightShare = if (hourlySum > 0)
        percent((22..23).sumOf { hourly[it] } + (0..5).sumOf { hourly[it] }, hourlySum)
    else 0

    KpiRow {
        KpiTile(
            icon = Icons.Outlined.CalendarMonth,
            label = stringResource(R.string.stats_habits_toughest_day),
            value = toughest?.let {
                DayOfWeek.of(it + 1).getDisplayName(TextStyle.FULL, locale)
                    .replaceFirstChar { c -> c.titlecase(locale) }
            } ?: "—",
            caption = toughest?.let {
                stringResource(R.string.stats_habits_toughest_day_caption, formatAverage(weekdayAvg[it]))
            },
            modifier = Modifier.weight(1f)
        )
        KpiTile(
            icon = Icons.Outlined.Schedule,
            label = stringResource(R.string.stats_habits_peak_hour),
            value = peakHour?.let { formatHour(it) } ?: "—",
            caption = peakHour?.let {
                stringResource(R.string.stats_habits_peak_hour_caption, percent(hourly[it], hourlySum))
            },
            modifier = Modifier.weight(1f)
        )
    }

    StatCard {
        CardLabel(stringResource(R.string.stats_habits_by_weekday))
        CardCaption(stringResource(R.string.stats_habits_by_weekday_caption))
        Spacer(Modifier.height(16.dp))
        if (toughest == null) {
            EmptyText(stringResource(R.string.stats_no_history))
        } else {
            MiniBars(values = weekdayAvg, color = primary, height = 96.dp, highlight = toughest)
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                (1..7).forEach { d ->
                    Text(
                        text = DayOfWeek.of(d).getDisplayName(TextStyle.SHORT, locale).take(3),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (d - 1 == toughest) FontWeight.Bold else FontWeight.Normal,
                        color = if (d - 1 == toughest) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    StatCard {
        CardLabel(stringResource(R.string.stats_habits_by_hour))
        CardCaption(stringResource(R.string.stats_habits_by_hour_caption))
        Spacer(Modifier.height(16.dp))
        if (peakHour == null) {
            EmptyText(stringResource(R.string.stats_habits_hour_empty))
        } else {
            MiniBars(
                values = hourly.map { it.toFloat() },
                color = primary,
                height = 96.dp,
                highlight = peakHour
            )
            Spacer(Modifier.height(6.dp))
            AxisLabels(listOf("00", "06", "12", "18", "23"))
            Spacer(Modifier.height(14.dp))
            HorizontalRule()
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.stats_habits_night_share, nightShare),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ===== Pestañas bloqueadas (Free) =====

@Composable
private fun LockedTab(tab: StatsTab, onOpenPaywall: () -> Unit) {
    val sample = remember { sampleStatsData() }
    // blur() solo existe desde Android 12; por debajo compensamos con menos alpha
    // para que la vista previa no se lea como datos reales.
    val previewAlpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.55f else 0.15f
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .blur(10.dp)
                .alpha(previewAlpha)
                .clearAndSetSemantics { },
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (tab) {
                StatsTab.TRENDS -> TrendsTab(sample)
                StatsTab.APPS -> AppsTab(sample)
                else -> HabitsTab(sample)
            }
        }
        // Capa que captura los toques: la vista previa no es interactiva.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenPaywall
                )
        )
        LockedCard(
            tab = tab,
            onUnlock = onOpenPaywall,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
        )
    }
}

@Composable
private fun LockedCard(tab: StatsTab, onUnlock: () -> Unit, modifier: Modifier = Modifier) {
    val (titleRes, bodyRes) = when (tab) {
        StatsTab.TRENDS -> R.string.stats_locked_trends_title to R.string.stats_locked_trends_body
        StatsTab.APPS -> R.string.stats_locked_apps_title to R.string.stats_locked_apps_body
        else -> R.string.stats_locked_habits_title to R.string.stats_locked_habits_body
    }
    val price = Premium.priceLabel ?: Premium.fallbackPrice()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            ProBadge()
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    R.string.stats_locked_point_history,
                    R.string.stats_locked_point_apps,
                    R.string.stats_locked_point_habits,
                    R.string.stats_locked_point_export
                ).forEach { res ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(res),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onUnlock,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.stats_locked_cta, price), fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.paywall_one_time),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Datos de ejemplo, deterministas, para la vista previa difuminada de Free. */
private fun sampleStatsData(): StatsData {
    val today = LocalDate.now()
    val pattern = intArrayOf(
        6, 9, 4, 11, 7, 14, 12, 5, 8, 10, 6, 13, 9, 7, 4,
        8, 11, 6, 9, 12, 15, 7, 5, 8, 6, 10, 9, 4, 7, 5
    )
    val history = pattern.mapIndexed { i, t ->
        val ig = t * (4 + i % 3) / 10
        val yt = t * 3 / 10
        Stats.DayCounts(
            today.minusDays((pattern.size - 1 - i).toLong()),
            Stats.Counts(t, ig, yt, t - ig - yt)
        )
    }
    val hourly = intArrayOf(3, 1, 0, 0, 0, 0, 1, 2, 4, 3, 2, 3, 5, 4, 3, 3, 4, 6, 7, 8, 10, 12, 14, 9)
    return StatsData(history.last().counts, history, 1240, hourly, 18, null)
}

// ===== Gráficos =====

/**
 * Tarjeta de actividad: total del periodo, gráfico de barras seleccionable y
 * detalle del día elegido. [average] dibuja una línea discontinua de media.
 */
@Composable
private fun ActivityChartCard(
    title: String,
    history: List<Stats.DayCounts>,
    compact: Boolean,
    selectedDate: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    average: Float? = null,
    delta: String? = null
) {
    val total = history.sumOf { it.counts.total }
    val selectedDay = history.find { it.date == selectedDate } ?: history.last()
    StatCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                CardLabel(title)
                Text(
                    text = stringResource(R.string.stats_day_blocks, total),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
            }
            if (delta != null) DeltaChip(delta)
        }
        Spacer(Modifier.height(20.dp))
        if (total == 0) {
            EmptyText(stringResource(R.string.stats_no_history))
        } else {
            BarChart(
                history = history,
                selectedDate = selectedDay.date,
                compact = compact,
                onSelect = onSelect,
                average = average
            )
            if (average != null) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(14.dp)
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    Spacer(Modifier.width(6.dp))
                    CardCaption(stringResource(R.string.stats_chart_avg_legend, formatAverage(average)))
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalRule()
            Spacer(Modifier.height(16.dp))
            SelectedDayDetail(selectedDay)
        }
    }
}

/**
 * Gráfico de barras. [compact]=false (semana): barras anchas con número y
 * etiqueta de día. [compact]=true (mes): 30 barras finas que llenan el ancho.
 * La barra seleccionada se resalta; tocar una barra invoca [onSelect].
 */
@Composable
private fun BarChart(
    history: List<Stats.DayCounts>,
    selectedDate: LocalDate,
    compact: Boolean,
    onSelect: (LocalDate) -> Unit,
    average: Float? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxValue = (history.maxOfOrNull { it.counts.total } ?: 0).coerceAtLeast(1)
    val dayFmt = remember { DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) }
    val labelToday = stringResource(R.string.stats_chart_today)
    val today = remember { LocalDate.now() }
    val maxBar = 104.dp

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .drawWithContent {
                    drawContent()
                    if (average != null && average > 0f) {
                        val y = size.height - maxBar.toPx() * (average / maxValue)
                        drawLine(
                            color = labelColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                        )
                    }
                },
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (compact) Arrangement.spacedBy(3.dp) else Arrangement.SpaceEvenly
        ) {
            history.forEach { day ->
                val isSelected = day.date == selectedDate
                val fraction = day.counts.total / maxValue.toFloat()
                val barHeight = (maxBar.value * fraction).coerceAtLeast(if (compact) 2f else 4f).dp
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
            AxisLabels(listOf(stringResource(R.string.stats_chart_30d_start), labelToday))
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

/** Barras mínimas sin etiquetas; [highlight] resalta un índice. */
@Composable
private fun MiniBars(
    values: List<Float>,
    color: Color,
    height: Dp,
    highlight: Int? = null
) {
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        values.forEachIndexed { i, v ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight((v / max).coerceIn(0.04f, 1f))
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(if (highlight == null || i == highlight) color else color.copy(alpha = 0.35f))
            )
        }
    }
}

@Composable
private fun SelectedDayDetail(day: Stats.DayCounts) {
    val today = remember { LocalDate.now() }
    val dateLabel = if (day.date == today) stringResource(R.string.stats_chart_today)
                    else formatDate(day.date.toString())

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
            DistributionStrip(day.counts)
        }
    }
}

@Composable
private fun DistributionStrip(counts: Stats.Counts) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DistributionItem(
            label = stringResource(R.string.stats_distribution_instagram),
            value = counts.instagram,
            accent = appAccent(Stats.PKG_INSTAGRAM),
            modifier = Modifier.weight(1f)
        )
        VerticalDivider()
        DistributionItem(
            label = stringResource(R.string.stats_distribution_youtube),
            value = counts.youtube,
            accent = appAccent(Stats.PKG_YOUTUBE),
            modifier = Modifier.weight(1f)
        )
        VerticalDivider()
        DistributionItem(
            label = stringResource(R.string.stats_distribution_tiktok),
            value = counts.tiktok,
            accent = appAccent(Stats.PKG_TIKTOK),
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
            ColorDot(accent)
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Black
        )
    }
}

// ===== Piezas visuales compartidas =====

@Composable
private fun StatCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CardMargin),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
private fun KpiRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CardMargin)
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun KpiTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            IconBadge(icon, size = 32.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                // Valores largos (p. ej. "Donnerstag") bajan de tamaño para no
                // partirse a mitad de palabra en una tarjeta de media anchura.
                style = if (value.length > 8) MaterialTheme.typography.titleLarge
                        else MaterialTheme.typography.headlineSmall,
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
}

@Composable
private fun IconBadge(icon: ImageVector, size: Dp = 38.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

@Composable
private fun ProBadge() {
    Text(
        text = stringResource(R.string.stats_locked_badge),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun DeltaChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
private fun CardLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

@Composable
private fun CardCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun EmptyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    )
}

@Composable
private fun AxisLabels(labels: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEach {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ColorDot(color: Color, size: Dp = 8.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
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

/** Colores de marca por app, ajustados al tema para mantener contraste legible. */
@Composable
private fun appAccent(pkg: String): Color {
    val dark = isSystemInDarkTheme()
    return when (pkg) {
        Stats.PKG_INSTAGRAM -> Color(0xFFE1306C).let { if (dark) it.lighten(0.30f) else it }
        Stats.PKG_YOUTUBE -> Color(0xFFFF0000).let { if (dark) it.lighten(0.30f) else it }
        // Cian de TikTok: muy claro de base, se oscurece en light para contraste.
        else -> Color(0xFF25F4EE).let { if (dark) it else it.darken(0.35f) }
    }
}

// ===== Formato =====

/** Variación porcentual frente al periodo anterior; null si no hay base. */
@Composable
private fun trendText(current: Int, previous: Int): String? {
    if (previous <= 0) return null
    val pct = ((current - previous) * 100f / previous).toInt()
    return when {
        pct > 0 -> stringResource(R.string.stats_trend_up, pct)
        pct < 0 -> stringResource(R.string.stats_trend_down, -pct)
        else -> stringResource(R.string.stats_trend_flat)
    }
}

private fun percent(part: Int, whole: Int): Int =
    if (whole <= 0) 0 else Math.round(part * 100f / whole)

private fun formatAverage(value: Float): String =
    if (value < 10f) String.format(Locale.getDefault(), "%.1f", value)
    else Math.round(value).toString()

private fun formatHour(hour: Int): String = String.format(Locale.getDefault(), "%02d:00", hour)

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
        val date = LocalDate.parse(isoDate)
        val fmt = DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG)
            .withLocale(Locale.getDefault())
        date.format(fmt)
    } catch (_: Exception) {
        isoDate
    }
}
