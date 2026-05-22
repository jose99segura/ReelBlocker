package app.reelblocker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    isAccessibilityEnabled(ctx)
                override fun isBatteryExempt(): Boolean =
                    isBatteryExempt(ctx)
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
        HomeScreen(onResetOnboarding = {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(onResetOnboarding: () -> Unit = {}) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var refreshKey by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val serviceEnabled = remember(refreshKey) { isAccessibilityEnabled(ctx) }
    val batteryExempt = remember(refreshKey) { isBatteryExempt(ctx) }
    // Estado vivo de Pro: reacciona al instante a una compra.
    val isPro = Premium.isProLive
    val today = remember(refreshKey) { Stats.read(ctx) }
    val history = remember(refreshKey) { Stats.readLastDays(ctx, 7) }

    var showPaywall by remember { mutableStateOf(false) }
    // Tap counter — 5 taps al wordmark alterna Pro. SOLO en builds debug.
    var secretTapCount by remember { mutableIntStateOf(0) }

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
                Toast.makeText(ctx, "Comprobando compras…", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        if (!BuildConfig.DEBUG) return@clickable
                        secretTapCount++
                        if (secretTapCount >= 5) {
                            val newValue = !Premium.isPro(ctx)
                            Premium.setProDebug(ctx, newValue)
                            Toast.makeText(
                                ctx,
                                if (newValue) "🔓 Pro activado (debug)" else "🔒 Pro desactivado (debug)",
                                Toast.LENGTH_SHORT
                            ).show()
                            secretTapCount = 0
                        }
                    }
                ) {
                    Text(
                        text = "Basta",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = if (isPro) "Reel Blocker · Pro" else "Reel Blocker",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPro) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isPro) FontWeight.Bold else FontWeight.Normal
                    )
                }
            })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Alertas: solo si requieren accion del usuario.
            if (!serviceEnabled) {
                ActionRequiredCard(
                    title = "Activa el servicio de accesibilidad",
                    body = "Sin esto, Basta! no puede detectar los Reels. Tócalo y activa Basta! en la lista.",
                    actionLabel = "Abrir ajustes de accesibilidad",
                    onAction = { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
                )
            }
            if (serviceEnabled && !batteryExempt) {
                ActionRequiredCard(
                    title = "Excluye de la optimización de batería",
                    body = "Sin esto, el sistema puede matar el servicio al cabo de unas horas.",
                    actionLabel = "Excluir de la batería",
                    onAction = { requestBatteryExemption(ctx) },
                    showOemHint = true
                )
            }

            // Contenido principal.
            StatsCard(today = today, history = history)
            TipCard()
            AppsCard(
                refreshKey = refreshKey,
                isPro = isPro,
                onChanged = { refreshKey++ },
                onOpenPaywall = { showPaywall = true }
            )
            if (!isPro) {
                ProUpsellCard(onClick = { showPaywall = true })
            }
            HelpCard()
            AboutCard(onResetOnboarding = onResetOnboarding)

            // Status pequeño al final.
            Spacer(Modifier.height(4.dp))
            StatusFooter(serviceEnabled = serviceEnabled, batteryExempt = batteryExempt)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ActionRequiredCard(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    showOemHint: Boolean = false
) {
    val ctx = LocalContext.current
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
            if (showOemHint) {
                val oem = Build.MANUFACTURER.lowercase()
                val isAggressiveOem = oem in setOf(
                    "xiaomi", "redmi", "poco", "huawei", "honor", "samsung", "oppo", "vivo", "realme"
                )
                if (isAggressiveOem) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = ctx.getString(R.string.oem_hint, Build.MANUFACTURER),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun StatusFooter(serviceEnabled: Boolean, batteryExempt: Boolean) {
    if (!serviceEnabled) return  // Si esta inactivo, el aviso ya esta arriba.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusChip(text = "Servicio activo", ok = true)
        Spacer(Modifier.width(12.dp))
        if (batteryExempt) {
            StatusChip(text = "Batería exenta", ok = true)
        }
    }
}

@Composable
private fun StatusChip(text: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (ok) Color(0xFF2E7D32) else Color(0xFFC62828))
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppsCard(
    refreshKey: Int,
    isPro: Boolean,
    onChanged: () -> Unit,
    onOpenPaywall: () -> Unit
) {
    val ctx = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Apps a bloquear", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Stats.BLOCKABLE_APPS.forEachIndexed { idx, (pkg, label) ->
                val installed = remember(refreshKey, pkg) { isAppInstalled(ctx, pkg) }
                val icon = remember(refreshKey, pkg) { loadAppIcon(ctx, pkg) }
                val enabled = remember(refreshKey, pkg) { Stats.isAppEnabled(ctx, pkg) }
                // Si la app no esta instalada, el switch se queda como esta
                // (apagado por defecto) y no podemos activarlo.

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconBadge(icon = icon, fallbackLetter = label.first().toString(), enabled = installed)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (installed) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        if (!installed) {
                            Text(
                                text = "No instalada",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = enabled && installed,
                        enabled = installed,
                        onCheckedChange = { newValue ->
                            Stats.setAppEnabled(ctx, pkg, newValue)
                            onChanged()
                        }
                    )
                }

                // Sub-opciones de Instagram (solo si esta instalado y activado).
                if (pkg == Stats.PKG_INSTAGRAM && enabled && installed) {
                    InstagramSubOptions(
                        refreshKey = refreshKey,
                        isPro = isPro,
                        onChanged = onChanged,
                        onOpenPaywall = onOpenPaywall
                    )
                }

                // Separador entre apps (no tras la ultima).
                if (idx < Stats.BLOCKABLE_APPS.lastIndex) {
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun InstagramSubOptions(
    refreshKey: Int,
    isPro: Boolean,
    onChanged: () -> Unit,
    onOpenPaywall: () -> Unit
) {
    val ctx = LocalContext.current
    val dmAllowed = remember(refreshKey) { Stats.isDmReelsAllowed(ctx) }
    val storiesBlocked = remember(refreshKey) { Stats.isStoriesBlocked(ctx) }

    SubOptionRow(
        title = "Permitir reels desde DM",
        description = "Reels enviados por amigos en mensajes directos no se bloquean",
        checked = isPro && dmAllowed,
        isPro = isPro,
        onCheckedChange = {
            Stats.setDmReelsAllowed(ctx, it)
            onChanged()
        },
        onLockedClick = onOpenPaywall
    )
    SubOptionRow(
        title = "Bloquear Historias",
        description = "Por defecto las stories no se bloquean. Actívalo si también quieres bloquearlas.",
        checked = isPro && storiesBlocked,
        isPro = isPro,
        onCheckedChange = {
            Stats.setStoriesBlocked(ctx, it)
            onChanged()
        },
        onLockedClick = onOpenPaywall
    )
}

@Composable
private fun SubOptionRow(
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
            .let { if (!isPro) it.clickable { onLockedClick() } else it }
            .padding(start = 52.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isPro) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (!isPro) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = "Pro",
                        modifier = Modifier.size(14.dp),
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
        Switch(
            checked = checked,
            enabled = isPro,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun AppIconBadge(icon: Drawable?, fallbackLetter: String, enabled: Boolean) {
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

@Composable
private fun StatsCard(today: Stats.Counts, history: List<Stats.DayCounts>) {
    val hasAnyHistory = history.any { it.counts.total > 0 }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Hoy", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            if (today.total == 0) {
                Text(
                    text = "0",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = "Sin caídas en Reels todavía. Bien.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = today.total.toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Instagram: ${today.instagram}  ·  YouTube: ${today.youtube}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Últimos 7 días", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            if (hasAnyHistory) {
                WeeklyChart(history)
            } else {
                Text(
                    text = "Aún no hay historial. Vuelve mañana — los datos se acumulan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun WeeklyChart(history: List<Stats.DayCounts>) {
    val primary = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxValue = (history.maxOfOrNull { it.counts.total } ?: 0).coerceAtLeast(1)
    val dayFmt = remember { DateTimeFormatter.ofPattern("EEE", Locale("es")) }
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
                // Hoy con gradiente lleno; los demás días más tenues.
                val barBrush = if (isToday) {
                    Brush.verticalGradient(listOf(primary, primary.copy(alpha = 0.65f)))
                } else {
                    Brush.verticalGradient(
                        listOf(primary.copy(alpha = 0.45f), primary.copy(alpha = 0.25f))
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
        Spacer(Modifier.height(5.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            history.forEachIndexed { idx, day ->
                val isToday = idx == lastIdx
                Box(
                    modifier = Modifier.width(34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isToday) "Hoy" else day.date.format(dayFmt).take(3),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) primary else labelColor
                    )
                }
            }
        }
    }
}

@Composable
private fun TipCard() {
    // Tip rotativo cada 8 segundos con fade transition.
    var tip by remember { mutableStateOf(Tips.random()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(8000)
            // Forzar uno nuevo distinto al actual.
            var next: String
            do { next = Tips.random() } while (next == tip)
            tip = next
        }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "💡 ¿Sabías que…",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(6.dp))
            AnimatedContent(
                targetState = tip,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "tip"
            ) { currentTip ->
                Text(
                    text = currentTip,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun AboutCard(onResetOnboarding: () -> Unit) {
    val ctx = LocalContext.current
    val versionName = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) { "1.0" }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "Acerca de",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Basta! Reel Blocker · v$versionName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    ctx.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://jose99segura.github.io/ReelBlocker/privacy.html"))
                    )
                }
            ) { Text("Política de privacidad") }

            TextButton(onClick = onResetOnboarding) {
                Text("Volver a ver tutorial")
            }
        }
    }
}

@Composable
private fun ProUpsellCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Basta! Pro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = Premium.priceLabel ?: Premium.PRO_PRICE,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Pago único, sin suscripción.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(12.dp))
            ProComparisonLine(text = "Bloqueo de Reels y Shorts", inFree = true)
            ProComparisonLine(text = "Estadísticas del día y la semana", inFree = true)
            ProComparisonLine(text = "Permitir reels desde DM de amigos", inFree = false)
            ProComparisonLine(text = "Bloquear Historias de Instagram", inFree = false)

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Desbloquear Pro")
            }
        }
    }
}

@Composable
private fun ProComparisonLine(text: String, inFree: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (inFree) Icons.Outlined.Check else Icons.Outlined.Lock,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (inFree) "Gratis" else "Pro",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                alpha = if (inFree) 0.6f else 1f
            )
        )
    }
}

@Composable
private fun HelpCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Cuando entres en Reels de Instagram o Shorts de YouTube " +
                "te sacará con el botón atrás. No envía nada fuera del dispositivo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

// ---- helpers fuera de Compose ----

private fun isAccessibilityEnabled(ctx: Context): Boolean {
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

private fun isBatteryExempt(ctx: Context): Boolean {
    val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(ctx.packageName)
}

@Suppress("BatteryLife")
private fun requestBatteryExemption(ctx: Context) {
    try {
        ctx.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:${ctx.packageName}"))
        )
    } catch (_: Exception) {
        ctx.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
}

private fun isAppInstalled(ctx: Context, pkg: String): Boolean = try {
    ctx.packageManager.getPackageInfo(pkg, 0)
    true
} catch (_: PackageManager.NameNotFoundException) {
    false
}

private fun loadAppIcon(ctx: Context, pkg: String): Drawable? = try {
    ctx.packageManager.getApplicationIcon(pkg)
} catch (_: PackageManager.NameNotFoundException) {
    null
}
