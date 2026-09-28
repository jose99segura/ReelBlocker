package app.reelblocker

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.review.ReviewManagerFactory

private const val TAG = "ReelBlocker.Review"

/**
 * Google Play in-app review prompt. Fires once, the first time the streak
 * reaches day 7: motivation is still high, the habit signal is already
 * there, and it's well before day 21 (graduation), which stays a separate,
 * uncluttered celebration screen.
 *
 * Google's ReviewManager gives no reliable signal about whether the dialog
 * was actually shown (internal quota/cooldown on Google's side), so this
 * marks itself done regardless of outcome — it only ever asks once per
 * install, never re-prompts on a later day-7 after a streak reset.
 *
 * Must be called from a live Activity context (UI thread, post-resume) —
 * never from [StreakWorker], which has no Activity to attach the flow to.
 */
object ReviewPrompt {

    fun maybeRequest(ctx: Context) {
        if (Streak.pendingMilestone(ctx) != 7) return

        if (Streak.isReviewRequested(ctx)) {
            // Ya se pidió en una racha anterior: solo limpiar el milestone
            // para no dejarlo pendiente eternamente.
            Streak.consumePendingMilestone(ctx)
            return
        }

        val activity = ctx as? Activity ?: return
        Streak.consumePendingMilestone(ctx)
        Streak.setReviewRequested(ctx)

        try {
            val manager = ReviewManagerFactory.create(activity)
            val request = manager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    manager.launchReviewFlow(activity, task.result)
                    Log.d(TAG, "maybeRequest: review flow launched at day 7")
                } else {
                    Log.d(TAG, "maybeRequest: requestReviewFlow failed", task.exception)
                }
            }
        } catch (e: Exception) {
            // Nunca debe tumbar la app por un fallo de una API opcional.
            Log.d(TAG, "maybeRequest: unexpected error", e)
        }
    }
}
