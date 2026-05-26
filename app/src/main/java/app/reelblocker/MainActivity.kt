package app.reelblocker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Status bar transparente para que el gradiente del hero "fluya" desde arriba.
        WindowCompat.setDecorFitsSystemWindows(window, true)
        Premium.init(applicationContext)
        setContent {
            ReelBlockerTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Root()
                }
            }
        }
    }
}

@Composable
private fun Root() {
    val ctx = LocalContext.current
    var onboardingDone by remember { mutableStateOf(Stats.isOnboardingDone(ctx)) }

    if (!onboardingDone) {
        val actions = remember {
            object : OnboardingActions {
                override fun openAccessibility() {
                    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                override fun requestBatteryExemption() {
                    requestBatteryExemption(ctx)
                }
                override fun isAccessibilityEnabled(): Boolean =
                    app.reelblocker.isAccessibilityEnabled(ctx)
                override fun isBatteryExempt(): Boolean =
                    app.reelblocker.isBatteryExempt(ctx)
            }
        }
        OnboardingScreen(
            actions = actions,
            onFinish = {
                Stats.setOnboardingDone(ctx)
                onboardingDone = true
            }
        )
    } else {
        AppRoot(onResetOnboarding = {
            Stats.setOnboardingDone(ctx, done = false)
            onboardingDone = false
        })
    }
}

@Composable
private fun ReelBlockerTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
private fun AppRoot(onResetOnboarding: () -> Unit) {
    val ctx = LocalContext.current
    var currentScreen: Screen by remember { mutableStateOf<Screen>(Screen.Home) }
    var showPaywall by remember { mutableStateOf(false) }
    var pendingGraduationVisible by remember { mutableStateOf(Collection.pendingGraduation(ctx) != null) }

    BackHandler(enabled = currentScreen != Screen.Home) {
        currentScreen = Screen.Home
    }

    Scaffold(
        bottomBar = {
            BottomNavBar(
                current = currentScreen,
                inventoryBadge = pendingGraduationVisible,
                onSelect = { tab -> currentScreen = tab }
            )
        }
    ) { outerPadding ->
        ScreenContainer(
            current = currentScreen,
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPadding)
        ) { screen ->
            when (screen) {
                is Screen.Home -> HomeScreen(
                    onOpenPaywall = { showPaywall = true },
                    onPendingGraduationChanged = { pendingGraduationVisible = it }
                )
                is Screen.Stats -> StatsScreen(
                    onOpenPaywall = { showPaywall = true }
                )
                is Screen.Settings -> SettingsScreen(
                    onOpenPaywall = { showPaywall = true },
                    onResetOnboarding = onResetOnboarding
                )
                is Screen.Inventory -> InventoryScreen()
            }
        }
    }

    if (showPaywall) {
        PaywallSheet(
            priceLabel = Premium.priceLabel ?: Premium.PRO_PRICE,
            onDismiss = { showPaywall = false },
            onPurchase = {
                (ctx as? Activity)?.let { Premium.launchPurchase(it) }
                showPaywall = false
            },
            onRestore = {
                Premium.restore(ctx)
                Toast.makeText(ctx, ctx.getString(R.string.toast_checking_purchases), Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    onOpenPaywall: () -> Unit,
    onPendingGraduationChanged: (Boolean) -> Unit
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var refreshKey by remember { mutableIntStateOf(0) }
    var externalDisableInfo by remember { mutableStateOf<ExternalDisableInfo?>(null) }
    var pendingGraduation by remember { mutableStateOf<MascotSpecies?>(null) }
    var breakRemainingMs by remember { mutableStateOf(Breaks.millisRemaining(ctx)) }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) {
                val enabledNow = isAccessibilityEnabled(ctx)
                val wasProtecting = Streak.wasProtecting(ctx)
                val nowProtecting = Streak.shouldBeProtecting(ctx)
                if (wasProtecting && !nowProtecting) {
                    // Capturar estado ANTES de romper, para mostrar al usuario
                    // el coste real (día y especie que pierde).
                    val priorState = Streak.current(ctx)
                    val priorSpecies = Collection.currentSpecies(ctx)
                    Streak.breakStreak(ctx, reason = "strict_disabled")
                    externalDisableInfo = ExternalDisableInfo(
                        priorCount = priorState.count,
                        priorLevel = priorState.level,
                        priorSpecies = priorSpecies
                    )
                } else if (nowProtecting) {
                    Streak.tick(ctx)
                }
                Streak.setServiceEnabledSeen(ctx, enabledNow)
                Streak.setProtectingSeen(ctx, nowProtecting)
                pendingGraduation = Collection.pendingGraduation(ctx)
                onPendingGraduationChanged(pendingGraduation != null)
                breakRemainingMs = Breaks.millisRemaining(ctx)
                refreshKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // Countdown del descanso — actualiza cada segundo mientras hay pausa.
    LaunchedEffect(breakRemainingMs != null) {
        while (breakRemainingMs != null) {
            kotlinx.coroutines.delay(1000)
            val remaining = Breaks.millisRemaining(ctx)
            if (remaining == null) {
                breakRemainingMs = null
                refreshKey++  // re-leer Streak.current y forzar repaint
                break
            }
            breakRemainingMs = remaining
        }
    }

    val serviceEnabled = remember(refreshKey) { isAccessibilityEnabled(ctx) }
    val batteryExempt = remember(refreshKey) { isBatteryExempt(ctx) }
    val isPro = Premium.isProLive
    val streakState = remember(refreshKey) { Streak.current(ctx) }
    val today = remember(refreshKey) { Stats.read(ctx) }
    val totalBlocks = remember(refreshKey) { Stats.totalBlocks(ctx) }
    var showHowItWorks by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.home_title),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                },
                actions = {
                    // Cómo funciona — único atajo a info contextual.
                    IconButton(onClick = { showHowItWorks = true }) {
                        Icon(
                            Icons.AutoMirrored.Outlined.HelpOutline,
                            contentDescription = stringResource(R.string.cd_how_it_works)
                        )
                    }
                    if (isPro) {
                        ProBadge()
                    } else {
                        UpgradeChip(onClick = onOpenPaywall)
                    }
                },
                // Sin inset extra — el Scaffold padre (con bottomBar) ya
                // reserva el inset de la status bar arriba.
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // (Las apps activas se muestran ahora abajo en el StatusFooter,
            // unidas al punto verde — una sola fila discreta.)

            // Alertas accion requerida (servicio/bateria) — solo si aplica.
            if (!serviceEnabled) {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    ActionRequiredCard(
                        title = stringResource(R.string.home_action_accessibility_title),
                        body = stringResource(R.string.home_action_accessibility_body),
                        actionLabel = stringResource(R.string.home_action_accessibility_cta),
                        onAction = { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                    )
                }
            }
            if (serviceEnabled && !batteryExempt) {
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                    ActionRequiredCard(
                        title = stringResource(R.string.home_action_battery_title),
                        body = stringResource(R.string.home_action_battery_body),
                        actionLabel = stringResource(R.string.home_action_battery_cta),
                        onAction = { requestBatteryExemption(ctx) }
                    )
                }
            }
            // ExternalDisableDialog se renderiza al final de HomeScreen como
            // modal — no consume espacio aquí dentro del scroll.

            // ===== HERO — edge-to-edge para que el glow se extienda libremente =====
            StreakCard(
                refreshKey = refreshKey,
                serviceEnabled = serviceEnabled,
                breakRemainingMs = breakRemainingMs
            )

            // ===== STRIP — metricas resumen (sin nav: para detalle vía bottom nav) =====
            Box(modifier = Modifier.padding(horizontal = 24.dp)) {
                MetricsStrip(
                    today = today.total,
                    totalBlocks = totalBlocks,
                    record = streakState.record,
                    onClick = null
                )
            }

            // ===== TIP cita =====
            TipQuote(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            )

            // ===== FOOTER fino — solo estado, sin acciones destructivas =====
            StatusFooter(
                serviceEnabled = serviceEnabled,
                refreshKey = refreshKey
            )
        }
    }

    pendingGraduation?.let { graduated ->
        GraduationDialog(
            graduatedSpecies = graduated,
            daysReached = streakState.count.coerceAtLeast(MascotLevel.ADULT.minDays),
            onConfirm = {
                Collection.consumePendingGraduation(ctx, daysReached = MascotLevel.ADULT.minDays)
                pendingGraduation = null
                refreshKey++
            }
        )
    }

    externalDisableInfo?.let { info ->
        ExternalDisableDialog(
            info = info,
            onDismiss = { externalDisableInfo = null }
        )
    }

    if (showHowItWorks) {
        HowItWorksDialog(onDismiss = { showHowItWorks = false })
    }
}

/**
 * Datos capturados ANTES de romper la racha — para mostrar al usuario
 * exactamente qué perdió cuando vuelve a la app tras desactivar el servicio.
 */
internal data class ExternalDisableInfo(
    val priorCount: Int,
    val priorLevel: MascotLevel,
    val priorSpecies: MascotSpecies
)

// ===== Top bar components =====

@Composable
private fun ProBadge() {
    Row(
        modifier = Modifier
            .padding(end = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.WorkspacePremium,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.badge_pro),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun UpgradeChip(onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = stringResource(R.string.chip_upgrade),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// ===== Metrics strip =====

@Composable
private fun MetricsStrip(
    today: Int,
    totalBlocks: Int,
    record: Int,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MetricItem(label = stringResource(R.string.metric_today), value = today.toString())
        VerticalDot()
        MetricItem(label = stringResource(R.string.metric_recovered), value = formatRecoveredShort(totalBlocks))
        VerticalDot()
        MetricItem(
            label = stringResource(R.string.metric_record),
            value = if (record == 0) stringResource(R.string.metric_dash)
                    else stringResource(R.string.metric_record_days, record)
        )
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun VerticalDot() {
    Box(
        modifier = Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
    )
}

private fun formatRecoveredShort(blocks: Int): String {
    val totalSeconds = blocks * 30L
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "—"
    }
}

// ===== Action required & external disable cards (siguen siendo de home) =====

@Composable
private fun ActionRequiredCard(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                Text(actionLabel)
            }
        }
    }
}

/**
 * Modal post-fact que aparece al volver a la app tras desactivar el servicio
 * desde fuera (ajustes del sistema). Más fuerte que el banner antiguo:
 * mascota triste, copy emocional con el coste real de la acción.
 */
@Composable
private fun ExternalDisableDialog(info: ExternalDisableInfo, onDismiss: () -> Unit) {
    val speciesName = stringResource(info.priorSpecies.displayNameRes)
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                    MascotCanvas(
                        level = info.priorLevel,
                        species = info.priorSpecies,
                        animate = true,
                        sad = true,
                        modifier = Modifier.size(96.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.home_external_disable_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(
                        R.string.external_disable_dialog_body,
                        info.priorCount,
                        speciesName
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.action_dismiss))
                }
            }
        }
    }
}

@Composable
private fun StatusFooter(
    serviceEnabled: Boolean,
    refreshKey: Int
) {
    val ctx = LocalContext.current
    if (!serviceEnabled) return
    // Una sola fila discreta: punto verde + "Activo en" + chips de las apps
    // que se están bloqueando. Sustituye al BlockedAppsIndicator anterior.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.status_active_in),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Stats.BLOCKABLE_APPS.forEach { (pkg, label) ->
            AppIconChip(
                pkg = pkg,
                label = label,
                refreshKey = refreshKey,
                onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
            Spacer(Modifier.width(6.dp))
        }
    }
}

@Composable
private fun AppIconChip(
    pkg: String,
    label: String,
    refreshKey: Int,
    onClick: () -> Unit
) {
    val ctx = LocalContext.current
    val installed = remember(refreshKey, pkg) { isAppInstalled(ctx, pkg) }
    val enabled = remember(refreshKey, pkg) { Stats.isAppEnabled(ctx, pkg) }
    val drawable = remember(refreshKey, pkg) { loadAppIcon(ctx, pkg) }
    val active = installed && enabled
    val alpha = if (active) 1f else 0.3f

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (drawable != null) {
            val bitmap = remember(drawable) { drawable.toBitmap(64, 64).asImageBitmap() }
            Image(
                bitmap = bitmap,
                contentDescription = label,
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp)),
                alpha = alpha
            )
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = label.first().toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

// ===== Helpers de servicio/sistema (internal para que las usen otras pantallas) =====

internal fun isAccessibilityEnabled(ctx: Context): Boolean {
    val expected = "${ctx.packageName}/${BlockerService::class.java.name}"
    val enabled = Settings.Secure.getString(
        ctx.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val splitter = TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabled)
    while (splitter.hasNext()) {
        if (splitter.next().equals(expected, ignoreCase = true)) return true
    }
    return false
}

internal fun isBatteryExempt(ctx: Context): Boolean {
    val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(ctx.packageName)
}

@Suppress("BatteryLife")
internal fun requestBatteryExemption(ctx: Context) {
    try {
        ctx.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:${ctx.packageName}"))
        )
    } catch (_: Exception) {
        ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}

internal fun isAppInstalled(ctx: Context, pkg: String): Boolean = try {
    ctx.packageManager.getPackageInfo(pkg, 0)
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}

internal fun loadAppIcon(ctx: Context, pkg: String): Drawable? = try {
    ctx.packageManager.getApplicationIcon(pkg)
} catch (_: PackageManager.NameNotFoundException) {
    null
}

@Composable
private fun BottomNavBar(
    current: Screen,
    inventoryBadge: Boolean,
    onSelect: (Screen) -> Unit
) {
    val inventoryBadgeDescription = stringResource(R.string.cd_inventory_pending_badge)
    val labelHome = stringResource(R.string.nav_home)
    val labelStats = stringResource(R.string.nav_stats)
    val labelInventory = stringResource(R.string.nav_inventory)
    val labelSettings = stringResource(R.string.nav_settings)
    NavigationBar {
        Screen.bottomTabs.forEach { tab ->
            val selected = tab == current
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab) },
                icon = {
                    val icon = when (tab) {
                        is Screen.Home -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
                        is Screen.Stats -> if (selected) Icons.AutoMirrored.Filled.ShowChart
                                        else Icons.AutoMirrored.Outlined.ShowChart
                        is Screen.Inventory -> if (selected) Icons.Filled.Pets else Icons.Outlined.Pets
                        is Screen.Settings -> if (selected) Icons.Filled.Tune else Icons.Outlined.Tune
                    }
                    if (tab is Screen.Inventory && inventoryBadge) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    modifier = Modifier.semantics {
                                        contentDescription = inventoryBadgeDescription
                                    }
                                )
                            }
                        ) {
                            Icon(icon, contentDescription = null)
                        }
                    } else {
                        Icon(icon, contentDescription = null)
                    }
                },
                label = {
                    Text(
                        text = when (tab) {
                            is Screen.Home -> labelHome
                            is Screen.Stats -> labelStats
                            is Screen.Inventory -> labelInventory
                            is Screen.Settings -> labelSettings
                        }
                    )
                },
                colors = NavigationBarItemDefaults.colors()
            )
        }
    }
}

@Composable
private fun HowItWorksDialog(onDismiss: () -> Unit) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.heightIn(max = 560.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.howitworks_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 3.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.howitworks_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )

                HowSection(
                    title = stringResource(R.string.howitworks_section_what_title),
                    body = stringResource(R.string.howitworks_section_what_body)
                )
                HowSection(
                    title = stringResource(R.string.howitworks_section_streak_title),
                    body = stringResource(R.string.howitworks_section_streak_body)
                )
                HowSection(
                    title = stringResource(R.string.howitworks_section_grows_title),
                    body = stringResource(R.string.howitworks_section_grows_body)
                )
                // Mini-timeline visual de la evolución.
                Spacer(Modifier.height(12.dp))
                EvolutionTimeline()

                HowSection(
                    title = stringResource(R.string.howitworks_section_graduation_title),
                    body = stringResource(R.string.howitworks_section_graduation_body)
                )
                HowSection(
                    title = stringResource(R.string.howitworks_section_inventory_title),
                    body = stringResource(R.string.howitworks_section_inventory_body)
                )
                HowSection(
                    title = stringResource(R.string.howitworks_section_important_title),
                    body = stringResource(R.string.howitworks_section_important_body)
                )

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.action_dismiss))
                }
            }
        }
    }
}

/** Mini-timeline visual de la evolución — se usa dentro del HowItWorksDialog. */
@Composable
private fun EvolutionTimeline() {
    val species = Collection.currentSpecies(LocalContext.current)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        TimelineStep(level = MascotLevel.EGG, dayLabel = "0", species = species)
        TimelineStep(level = MascotLevel.CRACKING, dayLabel = "3", species = species)
        TimelineStep(level = MascotLevel.HATCHLING, dayLabel = "7", species = species)
        TimelineStep(level = MascotLevel.JUVENILE, dayLabel = "14", species = species)
        TimelineStep(level = MascotLevel.ADULT, dayLabel = "30", species = species, highlight = true)
    }
}

@Composable
private fun TimelineStep(
    level: MascotLevel,
    dayLabel: String,
    species: MascotSpecies,
    highlight: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MascotCanvas(
            level = level,
            species = species,
            animate = false,
            modifier = Modifier.size(42.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = "Día $dayLabel",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HowSection(title: String, body: String) {
    Spacer(Modifier.height(16.dp))
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun GraduationDialog(
    graduatedSpecies: MascotSpecies,
    daysReached: Int,
    onConfirm: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { /* no dismiss casual — debe confirmar */ }
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.graduation_label),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 3.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.graduation_title, stringResource(graduatedSpecies.displayNameRes)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MascotCanvas(
                        level = MascotLevel.ADULT,
                        species = graduatedSpecies,
                        animate = true,
                        modifier = Modifier.size(180.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.graduation_body, daysReached),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.graduation_body_secondary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.graduation_confirm))
                }
            }
        }
    }
}
