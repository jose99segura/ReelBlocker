package app.reelblocker

import android.app.Activity
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.BatteryFull
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenPaywall: () -> Unit,
    onResetOnboarding: () -> Unit
) {
    val ctx = LocalContext.current
    val isPro = Premium.isProLive
    var refreshKey by remember { mutableIntStateOf(0) }
    var showConfirmDisable by remember { mutableStateOf(false) }

    // Refrescar estados al volver de ajustes externos.
    LaunchedEffect(Unit) { refreshKey++ }

    val serviceEnabled = remember(refreshKey) { isAccessibilityEnabled(ctx) }
    val batteryExempt = remember(refreshKey) { isBatteryExempt(ctx) }
    val streakState = remember(refreshKey) { Streak.current(ctx) }
    val currentSpecies = remember(refreshKey) { Collection.currentSpecies(ctx) }
    val breakAvailable = remember(refreshKey) { Breaks.isAvailableToday(ctx) }
    val breakRemainingMs = remember(refreshKey) { Breaks.millisRemaining(ctx) }
    var showBreakDialog by remember { mutableStateOf(false) }

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
                .padding(vertical = 8.dp)
        ) {
            // ===== Apps a bloquear =====
            SectionHeader(stringResource(R.string.settings_section_apps))
            Stats.BLOCKABLE_APPS.forEach { (pkg, label) ->
                AppRow(
                    pkg = pkg,
                    label = label,
                    refreshKey = refreshKey,
                    onChanged = { refreshKey++ },
                    isPro = isPro,
                    onOpenPaywall = onOpenPaywall
                )
            }

            SectionDivider()

            // ===== Proteccion =====
            SectionHeader(stringResource(R.string.settings_section_protection))
            SettingsRow(
                icon = Icons.Outlined.Accessibility,
                title = stringResource(R.string.settings_row_accessibility_title),
                subtitle = if (serviceEnabled) stringResource(R.string.settings_row_accessibility_subtitle_on)
                           else stringResource(R.string.settings_row_accessibility_subtitle_off),
                trailing = {
                    StatusDot(ok = serviceEnabled)
                },
                onClick = {
                    ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
            SettingsRow(
                icon = Icons.Outlined.BatteryFull,
                title = stringResource(R.string.settings_row_battery_title),
                subtitle = if (batteryExempt) stringResource(R.string.settings_row_battery_subtitle_on)
                           else stringResource(R.string.settings_row_battery_subtitle_off),
                trailing = {
                    StatusDot(ok = batteryExempt)
                },
                onClick = { requestBatteryExemption(ctx) }
            )
            val oem = Build.MANUFACTURER.lowercase()
            val isAggressiveOem = oem in setOf(
                "xiaomi", "redmi", "poco", "huawei", "honor", "samsung", "oppo", "vivo", "realme"
            )
            if (isAggressiveOem && !batteryExempt) {
                Text(
                    text = ctx.getString(R.string.oem_hint, Build.MANUFACTURER),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }
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

            SettingsRow(
                icon = Icons.Outlined.Block,
                title = stringResource(R.string.settings_row_disable_title),
                subtitle = stringResource(R.string.settings_row_disable_subtitle),
                onClick = {
                    android.util.Log.d("ReelBlocker.UI", "disable row tapped, showing dialog")
                    showConfirmDisable = true
                }
            )

            SectionDivider()

            // ===== Pro =====
            SectionHeader(stringResource(R.string.settings_section_pro))
            SettingsRow(
                icon = Icons.Outlined.WorkspacePremium,
                title = if (isPro) stringResource(R.string.settings_row_pro_on_title)
                        else stringResource(R.string.settings_row_pro_off_title),
                subtitle = if (isPro) stringResource(R.string.settings_row_pro_on_subtitle)
                           else stringResource(R.string.settings_row_pro_off_subtitle),
                onClick = onOpenPaywall,
                accent = !isPro
            )
            if (!isPro) {
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

            SectionDivider()

            // ===== Acerca de =====
            SectionHeader(stringResource(R.string.settings_section_about))
            val versionName = remember {
                try {
                    ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0"
                } catch (_: Exception) { "1.0" }
            }
            SettingsRow(
                icon = Icons.Outlined.Info,
                title = stringResource(R.string.settings_row_version_title),
                subtitle = stringResource(R.string.settings_row_version_subtitle, versionName)
            )
            SettingsRow(
                icon = Icons.Outlined.School,
                title = stringResource(R.string.settings_row_tutorial_title),
                subtitle = stringResource(R.string.settings_row_tutorial_subtitle),
                onClick = onResetOnboarding
            )
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
                ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
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

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    accent: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (accent) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (accent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

@Composable
private fun StatusDot(ok: Boolean) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(
                if (ok) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.error
            )
    )
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
                .padding(horizontal = 24.dp, vertical = 12.dp),
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
                modifier = Modifier.padding(start = 72.dp, end = 24.dp, bottom = 8.dp)
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
                        text = stringResource(R.string.app_disable_confirm_body, label, streakCount),
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
