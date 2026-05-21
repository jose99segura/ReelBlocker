package app.reelblocker

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
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
    }

    private fun refreshStats() {
        val c = Stats.read(this)
        statsText.text = getString(R.string.stats_format, c.total, c.instagram, c.youtube)
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
