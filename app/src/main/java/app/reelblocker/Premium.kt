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
    private const val KEY_PURCHASE_DATE_MS = "pro_purchase_date_ms"
    private const val KEY_IS_FOUNDER = "pro_is_founder"

    /** Precio durante la ventana Founder (display fallback). */
    const val FOUNDER_PRICE = "4,99 €"

    /** Precio post-cutoff. Anchor mostrado en el banner Founder y fallback
     *  cuando la ventana ha pasado. */
    const val POST_FOUNDER_PRICE = "8,99 €"

    /**
     * Fallback display si Play Billing aún no ha devuelto ProductDetails.
     * Se ajusta automáticamente cuando la ventana Founder pasa, para que el
     * fallback siempre coincida con lo que Play Console debería estar
     * cobrando (el dev tiene que subir Play Console al pasar la ventana).
     */
    fun fallbackPrice(): String =
        if (System.currentTimeMillis() < FOUNDER_CUTOFF_MS) FOUNDER_PRICE
        else POST_FOUNDER_PRICE

    /** Product ID configurado en Play Console. */
    const val PRO_PRODUCT_ID = "basta_pro"

    /**
     * Founder Edition cutoff. Cualquier compra de Pro antes de este instante
     * marca al usuario como Founder permanente (badge en Ajustes, copy
     * "Founder · Pro since X" en lugar de "Pro since X"). Crea urgencia
     * legítima al lanzamiento sin truco — la fecha es pública y fija.
     *
     * 1_790_812_799_000L = 2026-09-30 23:59:59 UTC (Founder hasta septiembre).
     * Ajustar este valor antes de release si el lanzamiento se mueve.
     * Sin servidor, esto es lo más honesto que se puede hacer.
     *
     * Verificación rápida del valor:
     *   [DateTimeOffset]::FromUnixTimeMilliseconds(1790812799000).UtcDateTime
     */
    const val FOUNDER_CUTOFF_MS = 1_790_812_799_000L

    /** Estado vivo para Compose. */
    var isProLive by mutableStateOf(false)
        private set
    var priceLabel by mutableStateOf<String?>(null)
        private set

    private var billingClient: BillingClient? = null
    private var productDetails: ProductDetails? = null
    private var appContext: Context? = null

    // ---- Cache persistente (lo que lee BlockerService) ----

    fun isPro(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_PRO, false)

    /** Millis epoch de cuando el usuario activó Pro. Null si nunca compró. */
    fun purchaseDateMs(ctx: Context): Long? {
        val ms = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_PURCHASE_DATE_MS, 0L)
        return if (ms == 0L) null else ms
    }

    /** True si la compra cayó dentro de la ventana Founder. Permanente. */
    fun isFounder(ctx: Context): Boolean =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_IS_FOUNDER, false)

    private fun setProPersisted(ctx: Context, value: Boolean) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_IS_PRO, value).apply()
    }

    /** Solo para el toggle de debug. NO usar en release. */
    fun setProDebug(ctx: Context, value: Boolean) {
        setProPersisted(ctx, value)
        isProLive = value
        StreakWidget.refresh(ctx)
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
        this.appContext = appContext
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

    /**
     * Cierra la conexión de Billing. Llamar cuando la app termina de verdad
     * (Activity.isFinishing), no en recreaciones por rotación. Evita dejar
     * abierta la conexión al servicio de Google Play.
     */
    fun teardown() {
        billingClient?.endConnection()
        billingClient = null
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
        // Billing 8+: the callback delivers a QueryProductDetailsResult, which
        // also carries the products Play could not fetch, instead of a bare list.
        client.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val details = queryResult.productDetailsList.firstOrNull()
                productDetails = details
                // getOneTimePurchaseOfferDetailsList() is the Billing 8+ shape;
                // the singular getter stays as a fallback for simple products.
                priceLabel = details?.oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
                    ?: details?.oneTimePurchaseOfferDetails?.formattedPrice
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
            } else {
                revokePro(ctx)
            }
        }
    }

    /**
     * Lanza el flujo de compra. Devuelve true si Play aceptó abrir el diálogo,
     * false si todavía no está listo (billing sin conectar, ProductDetails sin
     * cargar, o el propio launchBillingFlow rechaza). La UI usa el bool para
     * mantener el paywall abierto y enseñar feedback en lugar de cerrarlo en
     * silencio.
     */
    fun launchPurchase(activity: Activity): Boolean {
        val client = billingClient ?: run {
            Log.w(TAG, "launchPurchase: BillingClient no inicializado")
            return false
        }
        val details = productDetails ?: run {
            Log.w(TAG, "launchPurchase: ProductDetails aun no cargado")
            return false
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
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, "launchBillingFlow rechazado: code=${result.responseCode}")
            return false
        }
        return true
    }

    /** Para el boton "Restaurar compras". */
    fun restore(ctx: Context) = refreshPurchases(ctx)

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        // Conceder Pro en cuanto la compra llega (cubre el flujo en vivo desde
        // PurchasesUpdatedListener, no solo el restore al arrancar).
        appContext?.let { grantPro(it) }
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
        // Stamp purchase date + founder flag una sola vez, en la primera
        // concesión. Restores y re-queries no deben pisarlo.
        val prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getLong(KEY_PURCHASE_DATE_MS, 0L) == 0L) {
            val now = System.currentTimeMillis()
            prefs.edit()
                .putLong(KEY_PURCHASE_DATE_MS, now)
                .putBoolean(KEY_IS_FOUNDER, now < FOUNDER_CUTOFF_MS)
                .apply()
        }
        isProLive = true
        // Desbloquea el widget Pro al instante.
        StreakWidget.refresh(ctx)
    }

    private fun revokePro(ctx: Context) {
        setProPersisted(ctx, false)
        isProLive = false
        StreakWidget.refresh(ctx)
    }
}

/**
 * Cooldown del auto-trigger del paywall post-graduación. Sin esto, un free
 * user que cicla repeticiones de las 2 especies free recibe paywall cada
 * 30 días (día 60, 90, 120…). Demasiado agresivo. Apps premium tipo
 * Opal/One Sec lo hacen con cooldown.
 *
 * Solo aplica al auto-trigger del flujo de graduación. Las entradas
 * manuales (chip Upgrade en Home, slot Pro del Bestiario, fila Pro en
 * Settings) NO pasan por este filtro — siempre funcionan.
 */
internal object PaywallThrottle {
    private const val PREFS = "reelblocker_prefs"
    private const val KEY_LAST_SHOWN_MS = "paywall_last_shown_ms"
    private const val COOLDOWN_DAYS = 14L
    private const val COOLDOWN_MS = COOLDOWN_DAYS * 24L * 60L * 60L * 1000L

    fun shouldShow(ctx: Context): Boolean {
        val last = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_SHOWN_MS, 0L)
        return (System.currentTimeMillis() - last) >= COOLDOWN_MS
    }

    fun markShown(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_SHOWN_MS, System.currentTimeMillis()).apply()
    }
}
