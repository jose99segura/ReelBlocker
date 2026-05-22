package app.reelblocker

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * Estado de la compra "Basta! Pro" + wrapper de Google Play Billing.
 *
 * Doble naturaleza:
 *  - SharedPreferences (isPro/grant/revoke): cache persistente. Es la
 *    fuente de verdad para BlockerService, que corre como servicio aparte.
 *  - isProLive / priceLabel: estado Compose vivo para que la UI reaccione
 *    al instante a una compra.
 *
 * Verificacion local (sin backend): queryPurchasesAsync solo devuelve
 * compras que Google ya valido. Acknowledge local. Suficiente para esta app.
 */
object Premium {
    private const val TAG = "BastaBilling"
    private const val PREFS = "reelblocker_prefs"
    private const val KEY_IS_PRO = "is_pro_purchased"

    /** Precio de respaldo si Google aun no ha devuelto el ProductDetails. */
    const val PRO_PRICE = "4,99 €"

    /** Product ID configurado en Play Console. */
    const val PRO_PRODUCT_ID = "basta_pro"

    /** Estado vivo para Compose. */
    var isProLive by mutableStateOf(false)
        private set
    var priceLabel by mutableStateOf<String?>(null)
        private set

    private var billingClient: BillingClient? = null
    private var productDetails: ProductDetails? = null

    // ---- Cache persistente (lo que lee BlockerService) ----

    fun isPro(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_PRO, false)

    private fun setProPersisted(ctx: Context, value: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_IS_PRO, value).apply()
    }

    /** Solo para el toggle de debug. NO usar en release. */
    fun setProDebug(ctx: Context, value: Boolean) {
        setProPersisted(ctx, value)
        isProLive = value
    }

    // ---- Billing ----

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        } else {
            Log.d(TAG, "PurchasesUpdated: code=${result.responseCode}")
        }
    }

    /** Llamar una vez en MainActivity.onCreate con applicationContext. */
    fun init(appContext: Context) {
        // Estado inicial desde la cache, para que la UI no parpadee.
        isProLive = isPro(appContext)
        if (billingClient?.isReady == true) return

        val client = BillingClient.newBuilder(appContext)
            .setListener(purchasesListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()
        billingClient = client

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "Billing conectado")
                    queryProductDetails()
                    refreshPurchases(appContext)
                } else {
                    Log.w(TAG, "Billing setup fallo: ${result.responseCode}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.d(TAG, "Billing desconectado")
            }
        })
    }

    private fun queryProductDetails() {
        val client = billingClient ?: return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRO_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        client.queryProductDetailsAsync(params) { result, productList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = productList.firstOrNull()
                productDetails = details
                priceLabel = details?.oneTimePurchaseOfferDetails?.formattedPrice
                Log.d(TAG, "ProductDetails: precio=$priceLabel")
            } else {
                Log.w(TAG, "queryProductDetails fallo: ${result.responseCode}")
            }
        }
    }

    /** Consulta las compras existentes. Restaura Pro o lo quita si no hay. */
    fun refreshPurchases(ctx: Context) {
        val client = billingClient ?: return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            val owned = purchases.any {
                it.products.contains(PRO_PRODUCT_ID) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            if (owned) {
                purchases.forEach { handlePurchase(it) }
                grantPro(ctx)
            } else {
                revokePro(ctx)
            }
        }
    }

    /** Lanza el flujo de compra. Necesita el Activity actual. */
    fun launchPurchase(activity: Activity) {
        val client = billingClient ?: return
        val details = productDetails ?: run {
            Log.w(TAG, "launchPurchase: ProductDetails aun no cargado")
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .build()
                )
            )
            .build()
        client.launchBillingFlow(activity, params)
    }

    /** Para el boton "Restaurar compras". */
    fun restore(ctx: Context) = refreshPurchases(ctx)

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.isAcknowledged) {
            val client = billingClient ?: return
            val ack = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(ack) {
                Log.d(TAG, "Compra acknowledged")
            }
        }
    }

    private fun grantPro(ctx: Context) {
        setProPersisted(ctx, true)
        isProLive = true
    }

    private fun revokePro(ctx: Context) {
        setProPersisted(ctx, false)
        isProLive = false
    }
}
