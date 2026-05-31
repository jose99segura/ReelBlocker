package app.reelblocker

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Paywall full-screen del único SKU one-time (Basta! Pro). El precio se
 * lee de [Premium.priceLabel] (set por Play Billing tras queryProductDetails)
 * con fallback a [Premium.PRO_PRICE]. No hay tiers, ni trial, ni selección:
 * la decisión es comprar o no.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PremiumPaywallScreen(
    onClose: () -> Unit,
    onContinueFree: () -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    nextFreeSpecies: MascotSpecies? = null
) {
    BackHandler { onClose() }
    val haptic = LocalHapticFeedback.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // Fallback: si no se pasó la especie, leer la actual desde Collection.
    val freeSpecies = nextFreeSpecies ?: Collection.currentSpecies(ctx)
    val price = Premium.priceLabel ?: Premium.PRO_PRICE

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.cd_paywall_close)
                    )
                }
            }
        },
        bottomBar = {
            StickyCta(
                price = price,
                onPurchase = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPurchase()
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProMascotHero()
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.ppw_headline),
                fontSize = 28.sp,
                lineHeight = 34.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.ppw_sub),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))
            ComparisonCard()

            Spacer(Modifier.height(20.dp))
            PrivacyPromiseBlock()

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onRestore) {
                Text(text = stringResource(R.string.ppw_restore))
            }
            TextButton(onClick = onContinueFree) {
                Text(
                    text = stringResource(
                        R.string.ppw_secondary_continue_free,
                        stringResource(freeSpecies.displayNameRes)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProMascotHero() {
    // Glow radial + 3 mascotas Pro en fila escalonada (centro algo más grande).
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        Color.Transparent
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MascotCanvas(
                level = MascotLevel.ADULT,
                species = MascotSpecies.TORTUGA,
                animate = false,
                modifier = Modifier.size(92.dp)
            )
            MascotCanvas(
                level = MascotLevel.ADULT,
                species = MascotSpecies.LOBO,
                animate = false,
                modifier = Modifier.size(116.dp)
            )
            MascotCanvas(
                level = MascotLevel.ADULT,
                species = MascotSpecies.BUHO,
                animate = false,
                modifier = Modifier.size(92.dp)
            )
        }
    }
}

@Composable
private fun ComparisonCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)) {
            ComparisonHeader()
            ComparisonRow(
                feature = stringResource(R.string.ppw_compare_block),
                freeText = stringResource(R.string.ppw_compare_yes),
                proText = stringResource(R.string.ppw_compare_yes),
                freeIsCheck = true,
                proIsCheck = true
            )
            ComparisonRow(
                feature = stringResource(R.string.ppw_compare_species),
                freeText = stringResource(R.string.ppw_compare_species_free),
                proText = stringResource(R.string.ppw_compare_species_pro),
                freeIsCheck = false,
                proIsCheck = false
            )
            ComparisonRow(
                feature = stringResource(R.string.ppw_compare_breaks),
                freeText = stringResource(R.string.ppw_compare_no),
                proText = stringResource(R.string.ppw_compare_yes),
                freeIsCheck = false,
                proIsCheck = true
            )
            ComparisonRow(
                feature = stringResource(R.string.ppw_compare_stats),
                freeText = stringResource(R.string.ppw_compare_no),
                proText = stringResource(R.string.ppw_compare_yes),
                freeIsCheck = false,
                proIsCheck = true
            )
            ComparisonRow(
                feature = stringResource(R.string.ppw_compare_widget),
                freeText = stringResource(R.string.ppw_compare_no),
                proText = stringResource(R.string.ppw_compare_yes),
                freeIsCheck = false,
                proIsCheck = true,
                isLast = true
            )
        }
    }
}

@Composable
private fun ComparisonHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(R.string.ppw_compare_free),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.ppw_compare_pro),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ComparisonRow(
    feature: String,
    freeText: String,
    proText: String,
    freeIsCheck: Boolean,
    proIsCheck: Boolean,
    isLast: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (isLast) 8.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        ComparisonCell(text = freeText, isCheck = freeIsCheck, accent = false)
        ComparisonCell(text = proText, isCheck = proIsCheck, accent = true)
    }
}

@Composable
private fun ComparisonCell(text: String, isCheck: Boolean, accent: Boolean) {
    Box(
        modifier = Modifier.width(72.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isCheck) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = if (accent) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (accent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun StickyCta(
    price: String,
    onPurchase: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onPurchase,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Text(
                    text = "${stringResource(R.string.ppw_cta_buy)} · $price",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.paywall_one_time),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Promesa de privacidad: encabezado + 3 viñetas. Antes vivía en PaywallSheet.kt
 * (eliminado); re-alojada aquí, el superviviente del paywall.
 */
@Composable
internal fun PrivacyPromiseBlock() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.privacy_promise_heading),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        PaywallPrivacyBullet(stringResource(R.string.privacy_promise_bullet_local))
        PaywallPrivacyBullet(stringResource(R.string.privacy_promise_bullet_no_tracking))
        PaywallPrivacyBullet(stringResource(R.string.privacy_promise_bullet_drive))
    }
}

@Composable
private fun PaywallPrivacyBullet(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "·",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(12.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
