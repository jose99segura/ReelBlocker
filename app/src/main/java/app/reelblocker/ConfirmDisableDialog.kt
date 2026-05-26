package app.reelblocker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Dialog que aparece cuando el usuario intenta desactivar la proteccion desde
 * dentro de la app. Muestra la mascota triste y el coste emocional del clic.
 */
@Composable
fun ConfirmDisableDialog(
    streakCount: Int,
    level: MascotLevel,
    species: MascotSpecies = MascotSpecies.CLASICA,
    onDismiss: () -> Unit,
    onConfirmDisable: () -> Unit
) {
    val levelName = stringResource(level.displayNameRes).lowercase()
    val daysText = pluralStringResource(R.plurals.plural_days_count, streakCount, streakCount)
    val part1 = stringResource(R.string.confirm_disable_body_part1)
    val part2 = stringResource(R.string.confirm_disable_body_part2)
    val part3 = stringResource(R.string.confirm_disable_body_part3)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                MascotCanvas(
                    level = level,
                    species = species,
                    animate = true,
                    sad = true,
                    modifier = Modifier.size(72.dp)
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.confirm_disable_title),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = buildBodyText(daysText, levelName, part1, part2, part3),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.confirm_disable_warning),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            // El boton "Cancelar" es el primario para sesgar a quedarse.
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.confirm_disable_keep))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onConfirmDisable,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.confirm_disable_action))
            }
        }
    )
}

private fun buildBodyText(
    daysText: String,
    levelName: String,
    part1: String,
    part2: String,
    part3: String
): AnnotatedString {
    return buildAnnotatedString {
        append(part1)
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(daysText)
        }
        append(part2)
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(levelName)
        }
        append(part3)
    }
}
