package app.reelblocker

import android.content.Context
import android.util.Log

/**
 * Nivel de perfil basado en XP acumulado de POR VIDA. A diferencia de la racha
 * ([Streak]) o la colección ([Collection]), este contador NUNCA se resetea: ni
 * al romper la racha, ni al graduarse, ni al desinstalar (se respalda con el
 * resto de prefs). Es la única capa de progresión permanente de la app.
 *
 * Tres fuentes de XP, cada una enganchada donde el evento ya ocurre una sola vez:
 *  - cada bloqueo detectado     → [addBlockXp]      (desde [Stats.increment])
 *  - cada día protegido nuevo    → [addDayXp]        (desde [Streak.tick])
 *  - cada graduación             → [addGraduationXp] (desde [Collection.consumePendingGraduation])
 *
 * Comparte el mismo SharedPreferences que [Stats], [Streak] y [Collection].
 */
object Profile {
    private const val TAG = "ReelBlocker.Profile"
    private const val PREFS = "reelblocker_prefs"

    private const val KEY_XP = "profile_xp"
    private const val KEY_SEEDED = "profile_seeded"

    // XP por evento. Ajustables: un día protegido vale bastante más que un
    // bloqueo suelto, y una graduación es el hito gordo.
    const val XP_PER_BLOCK = 2
    const val XP_PER_DAY = 15
    const val XP_PER_GRADUATION = 150

    data class State(
        val level: Int,
        val xp: Int,
        /** XP acumulado dentro del nivel actual (0..xpForNextLevel). */
        val xpIntoLevel: Int,
        /** XP que cuesta el tramo del nivel actual al siguiente. */
        val xpForNextLevel: Int,
        /** Progreso 0..1 dentro del nivel actual. */
        val progress: Float
    )

    private fun prefs(ctx: Context) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- Curva (funciones puras, sin Context, testeables) -------------------

    /**
     * XP acumulado total necesario para ALCANZAR el nivel [n] (1-indexado).
     * Curva cuadrática suave: L1=0, L2=100, L3=300, L4=600, L5=1000, L6=1500…
     */
    fun xpToReachLevel(n: Int): Int = 50 * (n - 1) * n

    /** Nivel (≥1) correspondiente a un total de [xp]. Sin tope. */
    fun levelForXp(xp: Int): Int {
        var level = 1
        while (xpToReachLevel(level + 1) <= xp) level++
        return level
    }

    /** Deriva el [State] completo a partir de un total de XP. Pura. */
    fun stateForXp(xp: Int): State {
        val level = levelForXp(xp)
        val floor = xpToReachLevel(level)
        val ceil = xpToReachLevel(level + 1)
        val span = (ceil - floor).coerceAtLeast(1)
        val into = (xp - floor).coerceIn(0, span)
        return State(
            level = level,
            xp = xp,
            xpIntoLevel = into,
            xpForNextLevel = span,
            progress = into.toFloat() / span.toFloat()
        )
    }

    // ---- Lectura ------------------------------------------------------------

    fun current(ctx: Context): State {
        seedIfNeeded(ctx)
        return stateForXp(prefs(ctx).getInt(KEY_XP, 0))
    }

    // ---- Escritura ----------------------------------------------------------

    private fun addXp(ctx: Context, amount: Int) {
        if (amount <= 0) return
        seedIfNeeded(ctx)
        val p = prefs(ctx)
        val current = p.getInt(KEY_XP, 0)
        p.edit().putInt(KEY_XP, current + amount).apply()
    }

    fun addBlockXp(ctx: Context) = addXp(ctx, XP_PER_BLOCK)
    fun addDayXp(ctx: Context) = addXp(ctx, XP_PER_DAY)
    fun addGraduationXp(ctx: Context) = addXp(ctx, XP_PER_GRADUATION)

    /**
     * One-shot para usuarios que ya tenían progreso antes de existir el sistema
     * de niveles: siembra un XP aproximado a partir de lo que aún se puede leer
     * (récord de racha, bloqueos de los últimos 30 días, graduaciones). El
     * historial real >30 días ya se purgó, así que es deliberadamente aproximado:
     * solo evita que un usuario fiel arranque en Nv.1.
     */
    fun seedIfNeeded(ctx: Context) {
        val p = prefs(ctx)
        if (p.getBoolean(KEY_SEEDED, false)) return
        val seed = Streak.current(ctx).record * XP_PER_DAY +
            Stats.totalBlocks(ctx) * XP_PER_BLOCK +
            Collection.read(ctx).size * XP_PER_GRADUATION
        p.edit()
            .putInt(KEY_XP, seed)
            .putBoolean(KEY_SEEDED, true)
            .apply()
        Log.d(TAG, "seedIfNeeded: XP inicial sembrado = $seed (nivel ${levelForXp(seed)})")
    }
}
