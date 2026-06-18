package app.reelblocker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Avanza la racha diaria en segundo plano, sin depender de que el usuario abra
 * la app.
 *
 * Contexto del bug que arregla: antes [Streak.tick] solo se llamaba en el
 * ON_RESUME de [MainActivity]. Como [Streak.tick] reinicia la racha a 1 cuando
 * detecta un hueco de 2+ días naturales, bastaba con que el usuario se saltara
 * UN día de abrir la app (aunque la protección siguiera activa) para que el
 * siguiente open calculara `daysBetween >= 2` y reiniciara la racha a 1 —
 * típicamente hacia el día 4-5, justo cuando deja de abrir la app a diario.
 *
 * Este worker corre varias veces al día; cada ejecución, si la protección está
 * realmente activa ([Streak.shouldBeProtecting]), hace tick. Los ticks dentro
 * del mismo día natural son no-ops (daysBetween == 0), así que correr varias
 * veces es seguro y da resiliencia frente a ventanas de ejecución perdidas por
 * Doze. NO rompe la racha: la detección de "se desactivó la protección" sigue
 * siendo responsabilidad del ON_RESUME (con su diálogo). El worker solo suma.
 */
class StreakWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (Streak.shouldBeProtecting(ctx)) {
            val count = Streak.tick(ctx)
            Log.d(TAG, "doWork: protegiendo → tick (racha=$count)")
            StreakWidget.refresh(ctx)
        } else {
            Log.d(TAG, "doWork: no protegiendo → no-op")
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "ReelBlocker.Streak"
        private const val UNIQUE_NAME = "streak_daily_tick"

        /**
         * Programa el trabajo periódico. Idempotente: usa enqueue único con
         * política UPDATE para aplicar cambios de configuración sin duplicar.
         * Intervalo corto (cada 6 h) para tener varias oportunidades de tick al
         * día aunque Doze retrase alguna ventana; los ticks intra-día son no-op.
         */
        fun schedule(ctx: Context) {
            val request = PeriodicWorkRequestBuilder<StreakWorker>(6, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                UNIQUE_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
