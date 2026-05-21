package app.reelblocker

import android.content.Context

/**
 * Estado de la compra "Basta! Pro". Por ahora persiste en SharedPreferences
 * (placeholder hasta integrar Play Billing). Cuando se integre Billing
 * real, setPro() solo lo modifica el callback del BillingClient tras
 * verificacion.
 */
object Premium {
    private const val PREFS = "reelblocker_prefs"
    private const val KEY_IS_PRO = "is_pro_purchased"

    /** Precio mostrado al usuario en el paywall. */
    const val PRO_PRICE = "4,99 €"

    /** Product ID configurado en Play Console. */
    const val PRO_PRODUCT_ID = "basta_pro"

    fun isPro(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_PRO, false)

    fun setPro(ctx: Context, value: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IS_PRO, value)
            .apply()
    }
}
