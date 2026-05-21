package app.reelblocker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Interfaz minima. No usamos XML de layout para mantener el proyecto ligero:
 * construimos la pantalla por codigo. Su unica funcion es decirte si el
 * servicio esta activo y llevarte a los ajustes para activarlo.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var statsText: TextView
    private lateinit var batteryText: TextView
    private lateinit var batteryButton: Button
    private lateinit var oemHint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val title = TextView(this).apply {
            text = "ReelBlocker"
            textSize = 26f
            gravity = Gravity.CENTER
        }

        statusText = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 48, 0, 48)
        }

        val button = Button(this).apply {
            text = "Abrir ajustes de accesibilidad"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        statsText = TextView(this).apply {
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 48, 0, 16)
        }

        batteryText = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 8)
        }

        batteryButton = Button(this).apply {
            text = getString(R.string.battery_action)
            setOnClickListener { requestBatteryExemption() }
        }

        oemHint = TextView(this).apply {
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 0)
        }

        val help = TextView(this).apply {
            text = "Activa \"ReelBlocker\" en la lista de servicios de " +
                "accesibilidad. Una vez activo, te sacara automaticamente de " +
                "los Reels de Instagram y los Shorts de YouTube."
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(0, 48, 0, 0)
        }

        root.addView(title)
        root.addView(statusText)
        root.addView(button)
        root.addView(statsText)
        root.addView(batteryText)
        root.addView(batteryButton)
        root.addView(oemHint)
        root.addView(help)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        statusText.text = if (isServiceEnabled())
            "Estado: ACTIVO ✅"
        else
            "Estado: inactivo ❌\nToca el boton para activarlo."
        refreshStats()
        refreshBattery()
    }

    private fun refreshStats() {
        val c = Stats.read(this)
        statsText.text = getString(R.string.stats_format, c.total, c.instagram, c.youtube)
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun refreshBattery() {
        val excluded = isIgnoringBatteryOptimizations()
        batteryText.text = getString(
            if (excluded) R.string.battery_state_excluded
            else R.string.battery_state_optimized
        )
        batteryButton.visibility = if (excluded) View.GONE else View.VISIBLE

        val needsOemNote = !excluded && Build.MANUFACTURER.lowercase() in setOf(
            "xiaomi", "redmi", "poco", "huawei", "honor", "samsung", "oppo", "vivo", "realme"
        )
        if (needsOemNote) {
            oemHint.text = getString(R.string.oem_hint, Build.MANUFACTURER)
            oemHint.visibility = View.VISIBLE
        } else {
            oemHint.visibility = View.GONE
        }
    }

    @Suppress("BatteryLife")
    private fun requestBatteryExemption() {
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:$packageName"))
            )
        } catch (_: Exception) {
            // Fallback: abrir la pantalla general si el intent directo no esta permitido.
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun isServiceEnabled(): Boolean {
        val expected = "$packageName/${BlockerService::class.java.name}"
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expected, ignoreCase = true)) return true
        }
        return false
    }
}
