package app.reelblocker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================================
// UiKit — lenguaje visual compartido entre pantallas (Home, Ajustes, …).
// Margen lateral de las tarjetas: separarlas del borde da el aire "premium"
// frente a una lista edge-to-edge. Radio común para coherencia.
// ============================================================================

internal val CardMargin = 16.dp
internal val CardShape = RoundedCornerShape(20.dp)

/** Contenedor de sección: agrupa filas/contenido en una tarjeta redondeada. */
@Composable
internal fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CardMargin),
        shape = CardShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column { content() }
    }
}

/** Divisor fino con sangría, entre filas dentro de una misma tarjeta. */
@Composable
internal fun RowDivider(startPadding: Dp = 68.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = startPadding, end = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    )
}

/** Fila estándar: icono en caja tintada + título/subtítulo + trailing opcional. */
@Composable
internal fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    accent: Boolean = false
) {
    val iconTint = if (accent) MaterialTheme.colorScheme.primary
                   else MaterialTheme.colorScheme.onSurfaceVariant
    val iconBg = if (accent) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                 else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (accent) FontWeight.SemiBold else FontWeight.Normal,
                color = if (accent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        }
    }
}

/**
 * Tarjeta hero de estado de protección, de un vistazo. Tres estados:
 * protegido (verde/tertiary), protegido-pero-batería-en-riesgo (ámbar/secondary),
 * sin proteger (rojo/error). Clicable solo si hay un arreglo de un toque.
 */
@Composable
internal fun StatusHeroCard(
    serviceEnabled: Boolean,
    protecting: Boolean,
    batteryExempt: Boolean,
    onClick: (() -> Unit)?
) {
    // 0 = protegido, 1 = protegido pero batería en riesgo, 2 = sin proteger.
    val state = when {
        !protecting -> 2
        !batteryExempt -> 1
        else -> 0
    }
    val bg = when (state) {
        0 -> MaterialTheme.colorScheme.tertiaryContainer
        1 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val fg = when (state) {
        0 -> MaterialTheme.colorScheme.onTertiaryContainer
        1 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }
    val title = when (state) {
        0 -> stringResource(R.string.settings_status_protected_title)
        1 -> stringResource(R.string.settings_status_battery_title)
        else -> stringResource(R.string.settings_status_off_title)
    }
    val sub = when (state) {
        0 -> stringResource(R.string.settings_status_protected_sub)
        1 -> stringResource(R.string.settings_status_battery_sub)
        else -> if (serviceEnabled) stringResource(R.string.settings_status_off_sub_app)
                else stringResource(R.string.settings_status_off_sub_service)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CardMargin, vertical = 4.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(22.dp),
        color = bg
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(fg.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = fg
                )
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = fg.copy(alpha = 0.85f)
                )
            }
            if (onClick != null) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = fg
                )
            }
        }
    }
}

/** Punto de estado verde/rojo (ok / no-ok). */
@Composable
internal fun StatusDot(ok: Boolean) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(
                if (ok) MaterialTheme.colorScheme.tertiary
                else MaterialTheme.colorScheme.error
            )
    )
}
