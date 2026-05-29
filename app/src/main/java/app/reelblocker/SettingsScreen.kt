package app.reelblocker

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenPaywall: () -> Unit,
    onResetOnboarding: () -> Unit
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isPro = Premium.isProLive
    var refreshKey by remember { mutableIntStateOf(0) }
    var showConfirmDisable by remember { mutableStateOf(false) }

    // Refrescar estados en cada ON_RESUME — al volver de los ajustes del
    // sistema (Accesibilidad, batería) el estado debe reflejar el cambio.
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val serviceEnabled = remember(refreshKey) { isAccessibilityEnabled(ctx) }
    val batteryExempt = remember(refreshKey) { isBatteryExempt(ctx) }
    val protecting = remember(refreshKey) { Streak.shouldBeProtecting(ctx) }
    val streakState = remember(refreshKey) { Streak.current(ctx) }
    val currentSpecies = remember(refreshKey) { Collection.currentSpecies(ctx) }
    val breakAvailable = remember(refreshKey) { Breaks.isAvailableToday(ctx) }
    val breakRemainingMs = remember(refreshKey) { Breaks.millisRemaining(ctx) }
    var showBreakDialog by remember { mutableStateOf(false) }

    // El hero solo es accionable cuando hay un arreglo de un toque. Si lo que
    // falta es una app apagada, el usuario lo resuelve en la tarjeta de Apps
    // justo debajo, así que no lo hacemos clicable para no confundir.
    val heroClick: (() -> Unit)? = when {
        !serviceEnabled -> {
            { openAccessibilitySettings(ctx) }
        }
        !protecting -> null
        !batteryExempt -> {
            { requestBatteryExemption(ctx) }
        }
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 8.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // ===== Hero de estado: ¿estoy protegido? =====
            StatusHeroCard(
                serviceEnabled = serviceEnabled,
                protecting = protecting,
                batteryExempt = batteryExempt,
                onClick = heroClick
            )

            // ===== Apps a bloquear =====
            SectionHeader(stringResource(R.string.settings_section_apps))
            SettingsCard {
                Stats.BLOCKABLE_APPS.forEachIndexed { index, (pkg, label) ->
                    if (index > 0) RowDivider(startPadding = 72.dp)
                    AppRow(
                        pkg = pkg,
                        label = label,
                        refreshKey = refreshKey,
                        onChanged = { refreshKey++ },
                        isPro = isPro,
                        onOpenPaywall = onOpenPaywall
                    )
                }
            }

            // ===== Proteccion =====
            SectionHeader(stringResource(R.string.settings_section_protection))
            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.Accessibility,
                    title = stringResource(R.string.settings_row_accessibility_title),
                    subtitle = if (serviceEnabled) stringResource(R.string.settings_row_accessibility_subtitle_on)
                               else stringResource(R.string.settings_row_accessibility_subtitle_off),
                    trailing = { StatusDot(ok = serviceEnabled) },
                    onClick = {
                        openAccessibilitySettings(ctx)
                    }
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Outlined.BatteryFull,
                    title = stringResource(R.string.settings_row_battery_title),
                    subtitle = if (batteryExempt) stringResource(R.string.settings_row_battery_subtitle_on)
                               else stringResource(R.string.settings_row_battery_subtitle_off),
                    trailing = { StatusDot(ok = batteryExempt) },
                    onClick = { requestBatteryExemption(ctx) }
                )
                RowDivider()
                // Descanso — feature Pro. 1 al día de 10 min sin romper la racha.
                val breakSubtitle = when {
                    breakRemainingMs != null -> {
                        val totalSecs = breakRemainingMs / 1000
                        val mmss = String.format("%d:%02d", totalSecs / 60, totalSecs % 60)
                        stringResource(R.string.settings_row_break_subtitle_in_use, mmss)
                    }
                    !breakAvailable -> stringResource(R.string.settings_row_break_subtitle_consumed)
                    else -> stringResource(R.string.settings_row_break_subtitle_available)
                }
                SettingsRow(
                    icon = Icons.Outlined.Pause,
                    title = stringResource(R.string.settings_row_break_title),
                    subtitle = breakSubtitle,
                    onClick = {
                        when {
                            !isPro -> onOpenPaywall()
                            breakRemainingMs != null -> {
                                // En pausa → terminar.
                                Breaks.endEarly(ctx)
                                refreshKey++
                            }
                            breakAvailable -> showBreakDialog = true
                            else -> { /* consumido hoy, no hacer nada */ }
                        }
                    },
                    trailing = {
                        if (!isPro) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                )
            }

            val oem = Build.MANUFACTURER.lowercase()
            val isAggressiveOem = oem in setOf(
                "xiaomi", "redmi", "poco", "huawei", "honor", "samsung", "oppo", "vivo", "realme"
            )
            if (isAggressiveOem && !batteryExempt) {
                Text(
                    text = ctx.getString(R.string.oem_hint, Build.MANUFACTURER),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            // Desactivar protección — acción destructiva, de baja jerarquía
            // para que no compita con el resto.
            Spacer(Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = { showConfirmDisable = true }) {
                    Icon(
                        imageVector = Icons.Outlined.Block,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.settings_row_disable_title),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // ===== Pro =====
            SectionHeader(stringResource(R.string.settings_section_pro))
            if (!isPro) {
                ProUpsellCard(onClick = onOpenPaywall)
                Spacer(Modifier.height(8.dp))
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Outlined.Restore,
                        title = stringResource(R.string.settings_row_restore_title),
                        subtitle = stringResource(R.string.settings_row_restore_subtitle),
                        onClick = {
                            Premium.restore(ctx)
                            Toast.makeText(ctx, ctx.getString(R.string.toast_checking_purchases), Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            } else {
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Outlined.WorkspacePremium,
                        title = stringResource(R.string.settings_row_pro_on_title),
                        subtitle = stringResource(R.string.settings_row_pro_on_subtitle),
                        accent = true
                    )
                }
            }

            // ===== Tu privacidad — visible, no escondida =====
            SectionHeader(stringResource(R.string.privacy_promise_heading))
            SettingsCard {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    PrivacyBullet(stringResource(R.string.privacy_promise_bullet_local))
                    PrivacyBullet(stringResource(R.string.privacy_promise_bullet_no_tracking))
                    PrivacyBullet(stringResource(R.string.privacy_promise_bullet_drive))
                }
            }

            // ===== Acerca de =====
            SectionHeader(stringResource(R.string.settings_section_about))
            Text(
                text = stringResource(R.string.about_manifesto),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            Spacer(Modifier.height(12.dp))
            val versionName = remember {
                try {
                    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0"
                } catch (_: Exception) { "1.0" }
            }
            SettingsCard {
                SettingsRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.settings_row_version_title),
                    subtitle = stringResource(R.string.settings_row_version_subtitle, versionName)
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Outlined.School,
                    title = stringResource(R.string.settings_row_tutorial_title),
                    subtitle = stringResource(R.string.settings_row_tutorial_subtitle),
                    onClick = onResetOnboarding
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Outlined.PrivacyTip,
                    title = stringResource(R.string.settings_row_privacy_title),
                    subtitle = stringResource(R.string.settings_row_privacy_subtitle),
                    onClick = {
                        ctx.startActivity(
                            Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://jose99segura.github.io/ReelBlocker/privacy.html"))
                        )
                    }
                )
            }

            // ===== Dev tools (solo builds debug) =====
            if (BuildConfig.DEBUG) {
                Spacer(Modifier.height(16.dp))
                SectionHeader("Dev tools")
                var lilaEgg by remember { mutableStateOf(Stats.isDevLilaEggEnabled(ctx)) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Huevo lila (preview)",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = lilaEgg,
                        onCheckedChange = {
                            Stats.setDevLilaEggEnabled(ctx, it)
                            lilaEgg = it
                            refreshKey++
                        }
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DevButton("+1 día") {
                        Streak.devSetDays(ctx, streakState.count + 1)
                        refreshKey++
                    }
                    DevButton("+7 días") {
                        Streak.devSetDays(ctx, streakState.count + 7)
                        refreshKey++
                    }
                    DevButton("Forzar día 21 (graduación)") {
                        Streak.devSetDays(ctx, MascotLevel.ADULT.minDays)
                        refreshKey++
                    }
                    DevButton("Reset racha a 0") {
                        Streak.breakStreak(ctx, reason = "dev_reset")
                        refreshKey++
                    }
                    DevButton("Vaciar Pokédex") {
                        Collection.devClear(ctx)
                        refreshKey++
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Debug: 5 taps abajo en una zona oculta alternan Pro debug.
            if (BuildConfig.DEBUG) {
                var debugTaps by remember { mutableIntStateOf(0) }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable {
                            debugTaps++
                            if (debugTaps >= 5) {
                                val v = !Premium.isPro(ctx)
                                Premium.setProDebug(ctx, v)
                                Toast.makeText(
                                    ctx,
                                    if (v) "🔓 Pro activado (debug)" else "🔒 Pro desactivado (debug)",
                                    Toast.LENGTH_SHORT
                                ).show()
                                debugTaps = 0
                            }
                        }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showConfirmDisable) {
        ConfirmDisableDialog(
            streakCount = streakState.count,
            level = streakState.level,
            species = currentSpecies,
            onDismiss = { showConfirmDisable = false },
            onConfirmDisable = {
                // No rompemos la racha aquí. Solo abrimos los ajustes del
                // sistema; si el usuario apaga realmente el servicio, el
                // observer ON_RESUME de HomeScreen detectará la transición
                // enabled→disabled y romperá la racha entonces. Si se
                // arrepiente y vuelve sin tocar nada, la racha sigue intacta.
                showConfirmDisable = false
                openAccessibilitySettings(ctx)
            }
        )
    }

    if (showBreakDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showBreakDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Pause,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.break_dialog_title),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.break_dialog_body),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = {
                    Breaks.start(ctx)
                    showBreakDialog = false
                    refreshKey++
                }) {
                    Text(stringResource(R.string.break_dialog_confirm))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showBreakDialog = false }) {
                    Text(stringResource(R.string.break_dialog_cancel))
                }
            }
        )
    }
}

/** Tarjeta de upsell Pro destacada (cuando el usuario aún no es Pro). */
@Composable
private fun ProUpsellCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CardMargin)
            .clickable { onClick() },
        shape = CardShape,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.WorkspacePremium,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_row_pro_off_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(R.string.settings_row_pro_off_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun DevButton(label: String, onClick: () -> Unit) {
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

/** Viñeta de la promesa de privacidad (antes vivía en el borrado PaywallSheet.kt). */
@Composable
private fun PrivacyBullet(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "·",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppRow(
    pkg: String,
    label: String,
    refreshKey: Int,
    onChanged: () -> Unit,
    isPro: Boolean,
    onOpenPaywall: () -> Unit
) {
    val ctx = LocalContext.current
    val installed = remember(refreshKey, pkg) { isAppInstalled(ctx, pkg) }
    val icon = remember(refreshKey, pkg) { loadAppIcon(ctx, pkg) }
    val enabled = remember(refreshKey, pkg) { Stats.isAppEnabled(ctx, pkg) }
    val isInstagram = pkg == Stats.PKG_INSTAGRAM
    val canExpand = isInstagram && installed && enabled
    // Por defecto las sub-opciones de IG se muestran si IG está activado —
    // así el usuario no tiene que descubrir que existen.
    var expanded by remember(refreshKey, canExpand) { mutableStateOf(canExpand) }
    // Confirmación al apagar: el usuario no debería desactivar el bloqueo
    // de una red social sin querer.
    var showDisableConfirm by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (canExpand) Modifier.clickable { expanded = !expanded } else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIcon(icon = icon, fallbackLetter = label.first().toString(), enabled = installed)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (installed) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Text(
                    text = when {
                        !installed -> stringResource(R.string.settings_app_status_not_installed)
                        canExpand && expanded -> stringResource(R.string.settings_app_status_hide_advanced)
                        canExpand -> stringResource(R.string.settings_app_status_show_advanced)
                        else -> if (enabled) stringResource(R.string.settings_app_status_blocking)
                                else stringResource(R.string.settings_app_status_disabled)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = enabled && installed,
                enabled = installed,
                onCheckedChange = { newValue ->
                    if (newValue) {
                        // Activar es directo, no requiere advertencia.
                        Stats.setAppEnabled(ctx, pkg, true)
                        onChanged()
                    } else {
                        // Desactivar: pedir confirmación.
                        showDisableConfirm = true
                    }
                }
            )
        }

        AnimatedVisibility(
            visible = canExpand && expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(start = 72.dp, end = 16.dp, bottom = 8.dp)
            ) {
                IgSubOption(
                    title = stringResource(R.string.ig_suboption_dm_title),
                    description = stringResource(R.string.ig_suboption_dm_description),
                    checked = isPro && Stats.isDmReelsAllowed(ctx),
                    isPro = isPro,
                    onCheckedChange = {
                        Stats.setDmReelsAllowed(ctx, it)
                        onChanged()
                    },
                    onLockedClick = onOpenPaywall
                )
                IgSubOption(
                    title = stringResource(R.string.ig_suboption_stories_title),
                    description = stringResource(R.string.ig_suboption_stories_description),
                    checked = isPro && Stats.isStoriesBlocked(ctx),
                    isPro = isPro,
                    onCheckedChange = {
                        Stats.setStoriesBlocked(ctx, it)
                        onChanged()
                    },
                    onLockedClick = onOpenPaywall
                )
            }
        }
    }

    if (showDisableConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDisableConfirm = false },
            icon = {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.app_disable_confirm_title, label),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                val streakCount = Streak.current(ctx).count
                val breakAvailable = isPro && Breaks.isAvailableToday(ctx) && !Breaks.isOnBreak(ctx)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = pluralStringResource(R.plurals.app_disable_confirm_body, streakCount, label, streakCount),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (breakAvailable) {
                        Spacer(Modifier.height(16.dp))
                        androidx.compose.material3.TextButton(
                            onClick = {
                                Breaks.start(ctx)
                                onChanged()
                                showDisableConfirm = false
                            }
                        ) {
                            Text(stringResource(R.string.app_disable_confirm_alternative))
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(onClick = { showDisableConfirm = false }) {
                    Text(stringResource(R.string.app_disable_confirm_keep))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        if (Streak.current(ctx).count > 0) {
                            Streak.breakStreak(ctx, reason = "app_disabled_$pkg")
                        }
                        Stats.setAppEnabled(ctx, pkg, false)
                        onChanged()
                        showDisableConfirm = false
                    },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.app_disable_confirm_disable))
                }
            }
        )
    }
}

@Composable
private fun IgSubOption(
    title: String,
    description: String,
    checked: Boolean,
    isPro: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onLockedClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!isPro) Modifier.clickable { onLockedClick() } else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPro) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                if (!isPro) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = stringResource(R.string.ig_suboption_pro_cd),
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            enabled = isPro,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun AppIcon(icon: android.graphics.drawable.Drawable?, fallbackLetter: String, enabled: Boolean) {
    val alpha = if (enabled) 1f else 0.35f
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (icon == null) MaterialTheme.colorScheme.surfaceVariant
                else Color.Transparent
            ),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            val bitmap = remember(icon) { icon.toBitmap(96, 96).asImageBitmap() }
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp)),
                alpha = alpha
            )
        } else {
            Text(
                text = fallbackLetter,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            )
        }
    }
}
