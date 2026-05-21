package app.reelblocker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        HomeScreen()
    }
}

@Composable
private fun ReelBlockerTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme = colors, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen() {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // refreshKey cambia en cada ON_RESUME → recalcular estados.
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
    val today = remember(refreshKey) { Stats.read(ctx) }
    val history = remember(refreshKey) { Stats.readLastDays(ctx, 7) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("ReelBlocker") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusCard(
                enabled = serviceEnabled,
                onAction = { ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            )
            BatteryCard(
                exempt = batteryExempt,
                onAction = { requestBatteryExemption(ctx) }
            )
            AppsCard(refreshKey) { refreshKey++ }
            StatsCard(today = today, history = history)
            HelpCard()
        }
    }
}

@Composable
private fun StatusCard(enabled: Boolean, onAction: () -> Unit) {
    SectionCard(
        title = "Servicio de accesibilidad",
        statusText = if (enabled) "Activo" else "Inactivo",
        statusOk = enabled
    ) {
        if (!enabled) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                Text("Abrir ajustes de accesibilidad")
            }
        }
    }
}

@Composable
private fun BatteryCard(exempt: Boolean, onAction: () -> Unit) {
    val ctx = LocalContext.current
    val oem = Build.MANUFACTURER.lowercase()
    val showOemHint = !exempt && oem in setOf(
        "xiaomi", "redmi", "poco", "huawei", "honor", "samsung", "oppo", "vivo", "realme"
    )

    SectionCard(
        title = "Optimizacion de bateria",
        statusText = if (exempt) "Exenta" else "El sistema puede matar el servicio",
        statusOk = exempt
    ) {
        if (!exempt) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                Text("Excluir de la optimizacion")
            }
        }
        if (showOemHint) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = ctx.getString(R.string.oem_hint, Build.MANUFACTURER),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppsCard(refreshKey: Int, onChanged: () -> Unit) {
    val ctx = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Apps a bloquear", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Stats.BLOCKABLE_APPS.forEach { (pkg, label) ->
                val enabled = remember(refreshKey, pkg) { Stats.isAppEnabled(ctx, pkg) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { newValue ->
                            Stats.setAppEnabled(ctx, pkg, newValue)
                            onChanged()
                        }
                    )
                }
                // Sub-opcion: permitir reels desde DM, solo bajo Instagram
                // y solo si Instagram esta encendido.
                if (pkg == Stats.PKG_INSTAGRAM && enabled) {
                    val dmAllowed = remember(refreshKey) { Stats.isDmReelsAllowed(ctx) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Permitir reels desde DM",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Reels enviados por amigos en mensajes directos no se bloquean",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = dmAllowed,
                            onCheckedChange = { newValue ->
                                Stats.setDmReelsAllowed(ctx, newValue)
                                onChanged()
                            }
                        )
                    }

                    // Sub-opcion: bloquear tambien las Historias.
                    val storiesBlocked = remember(refreshKey) { Stats.isStoriesBlocked(ctx) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Bloquear Historias",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Por defecto las stories no se bloquean. Activalo si tambien quieres bloquearlas.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = storiesBlocked,
                            onCheckedChange = { newValue ->
                                Stats.setStoriesBlocked(ctx, newValue)
                                onChanged()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCard(today: Stats.Counts, history: List<Stats.DayCounts>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Hoy", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
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

            Spacer(Modifier.height(16.dp))
            Text("Ultimos 7 dias", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            WeeklyChart(history)
        }
    }
}

@Composable
private fun WeeklyChart(history: List<Stats.DayCounts>) {
    val barColor = MaterialTheme.colorScheme.primary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxValue = (history.maxOfOrNull { it.counts.total } ?: 0).coerceAtLeast(1)
    val dayFmt = remember { DateTimeFormatter.ofPattern("EEE", Locale("es")) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            history.forEach { day ->
                val fraction = day.counts.total / maxValue.toFloat()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.width(32.dp)
                ) {
                    Text(
                        text = day.counts.total.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                    Spacer(Modifier.height(2.dp))
                    Canvas(
                        modifier = Modifier
                            .width(20.dp)
                            .height((100.dp.value * fraction).coerceAtLeast(2f).dp)
                    ) {
                        drawRoundedBar(size, barColor)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            history.forEach { day ->
                Box(
                    modifier = Modifier.width(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.date.format(dayFmt).take(3),
                        style = MaterialTheme.typography.labelSmall,
                        color = labelColor
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawRoundedBar(
    size: Size,
    color: Color
) {
    drawRoundRect(
        color = color,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
    )
}

@Composable
private fun HelpCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = "Una vez activo, cada vez que entres en Reels de Instagram o " +
                "Shorts de YouTube te sacara con el boton atras. No envia nada " +
                "fuera del dispositivo.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    statusText: String,
    statusOk: Boolean,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (statusOk) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            content()
        }
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
