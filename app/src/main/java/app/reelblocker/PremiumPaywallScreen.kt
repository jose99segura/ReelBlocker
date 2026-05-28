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
 * Tier de UI para la paywall premium. Independiente del backend billing
 * (Fase 1 sustituirá esto por ProTier real cuando los SKUs existan en
 * Play Console).
 */
internal enum class UiTier(
    val labelRes: Int,
    val priceRes: Int,
    val subRes: Int?,
    val hasTrial: Boolean
) {
    WEEKLY(
        labelRes = R.string.ppw_tier_weekly_label,
        priceRes = R.string.ppw_tier_weekly_price,
        subRes = R.string.ppw_tier_weekly_sub,
        hasTrial = false
    ),
    MONTHLY(
        labelRes = R.string.ppw_tier_monthly_label,
        priceRes = R.string.ppw_tier_monthly_price,
        subRes = null,
        hasTrial = false
    ),
    YEARLY(
        labelRes = R.string.ppw_tier_yearly_label,
        priceRes = R.string.ppw_tier_yearly_price,
        subRes = R.string.ppw_tier_yearly_save,
        hasTrial = true
    ),
    LIFETIME(
        labelRes = R.string.ppw_tier_lifetime_label,
        priceRes = R.string.ppw_tier_lifetime_price,
        subRes = R.string.ppw_tier_lifetime_sub,
        hasTrial = false
    )
}

/**
 * Paywall full-screen — sustituye al ModalBottomSheet [PaywallSheet] cuando
 * el usuario llega aquí desde el inventario o desde una graduación que
 * agotó las especies free.
 *
 * Por ahora dispara [onPurchase] sobre el mismo flujo single-SKU. Cuando
 * Fase 1 entregue ProTier real, `onPurchase` recibirá el tier seleccionado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PremiumPaywallScreen(
    onClose: () -> Unit,
    onContinueFree: () -> Unit,
    onPurchase: (UiTier) -> Unit,
    onRestore: () -> Unit,
    nextFreeSpecies: MascotSpecies? = null
) {
    BackHandler { onClose() }
    val haptic = LocalHapticFeedback.current
    val ctx = androidx.compose.ui.platform.LocalContext.current
    // Fallback: si no se pasó la especie, leer la actual desde Collection.
    val freeSpecies = nextFreeSpecies ?: Collection.currentSpecies(ctx)
    var selected by remember { mutableStateOf(UiTier.YEARLY) }

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
                selected = selected,
                onPurchase = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onPurchase(selected)
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

            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.ppw_compare_title).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.5.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                textAlign = TextAlign.Start
            )

            // Tiers stacked, anual destacado en el centro.
            val onTierSelect: (UiTier) -> Unit = {
                if (it != selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                selected = it
            }
            TierCard(UiTier.WEEKLY, selected, onSelect = onTierSelect, compact = true)
            Spacer(Modifier.height(8.dp))
            TierCard(UiTier.MONTHLY, selected, onSelect = onTierSelect, compact = true)
            Spacer(Modifier.height(8.dp))
            TierCard(UiTier.YEARLY, selected, onSelect = onTierSelect, compact = false)
            Spacer(Modifier.height(8.dp))
            TierCard(UiTier.LIFETIME, selected, onSelect = onTierSelect, compact = true)

            Spacer(Modifier.height(20.dp))
            PrivacyPromiseBlock()

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.ppw_play_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

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
private fun TierCard(
    tier: UiTier,
    selected: UiTier,
    onSelect: (UiTier) -> Unit,
    compact: Boolean
) {
    val isSelected = tier == selected
    val isHighlighted = tier == UiTier.YEARLY
    val borderColor = when {
        isSelected && isHighlighted -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
        isHighlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }
    val borderWidth = if (isSelected || isHighlighted) 2.dp else 1.dp
    val bgAlpha = if (isHighlighted) 0.08f else 0.03f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(18.dp))
            .clickable { onSelect(tier) },
        color = MaterialTheme.colorScheme.primary.copy(alpha = bgAlpha)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 16.dp,
                vertical = if (compact) 14.dp else 18.dp
            )
        ) {
            if (isHighlighted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.ppw_tier_yearly_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    if (tier.hasTrial) {
                        Text(
                            text = stringResource(R.string.ppw_tier_yearly_trial),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                SelectionDot(isSelected = isSelected, isHighlighted = isHighlighted)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(tier.labelRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    tier.subRes?.let { sub ->
                        Text(
                            text = stringResource(sub),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (tier == UiTier.WEEKLY) {
                    // Tachado para reforzar el contraste con anual.
                    Text(
                        text = stringResource(tier.priceRes),
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = stringResource(tier.priceRes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isHighlighted) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionDot(isSelected: Boolean, isHighlighted: Boolean) {
    val color = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isHighlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outline
    }
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .border(2.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
private fun StickyCta(
    selected: UiTier,
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
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
                    text = stringResource(
                        if (selected.hasTrial) R.string.ppw_cta_trial
                        else R.string.ppw_cta_buy
                    ),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
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
