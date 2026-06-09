package app.reelblocker

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val CONFIRM_WAIT_SECONDS = 30

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDisableDialog(
    appLabel: String,
    breakAvailable: Boolean,
    onDismiss: () -> Unit,
    onConfirmDisable: () -> Unit,
    onTakeBreak: () -> Unit
) {
    val ctx = LocalContext.current
    val streakState = remember { Streak.current(ctx) }
    val currentSpecies = remember { Collection.currentSpecies(ctx) }
    val currentLevel = MascotLevel.forDays(streakState.count)
    val hasStreak = streakState.count > 0
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val speciesName = stringResource(currentSpecies.displayNameRes)
    val levelName = stringResource(currentLevel.displayNameRes)
    val levelNum = currentLevel.ordinal + 1

    var secondsLeft by remember { mutableIntStateOf(CONFIRM_WAIT_SECONDS) }
    var confirmEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
        confirmEnabled = true
    }

    val rawProgress = (CONFIRM_WAIT_SECONDS - secondsLeft).toFloat() / CONFIRM_WAIT_SECONDS
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(700),
        label = "bar_progress"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                MascotCanvas(
                    level = currentLevel,
                    species = currentSpecies,
                    animate = true,
                    sad = true,
                    modifier = Modifier.size(96.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.app_disable_confirm_title, appLabel),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            if (hasStreak) {
                Text(
                    text = buildDetailBody(
                        speciesName, levelName, levelNum,
                        streakState.count, streakState.record, appLabel
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = stringResource(R.string.app_disable_no_streak),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!confirmEnabled) {
                Spacer(Modifier.height(18.dp))
                Text(
                    text = stringResource(R.string.app_disable_countdown, secondsLeft),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.small),
                    color = MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }

            if (breakAvailable) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = {
                    onTakeBreak()
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) onDismiss()
                    }
                }) {
                    Text(stringResource(R.string.app_disable_confirm_alternative))
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.app_disable_confirm_keep))
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { if (confirmEnabled) onConfirmDisable() },
                enabled = confirmEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (confirmEnabled) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                )
            ) {
                Text(stringResource(R.string.app_disable_confirm_disable))
            }
        }
    }
}

@Composable
private fun buildDetailBody(
    speciesName: String,
    levelName: String,
    levelNum: Int,
    daysCount: Int,
    record: Int,
    appLabel: String
): AnnotatedString {
    val line1 = stringResource(R.string.app_disable_detail_line1, speciesName, levelName, levelNum, daysCount)
    val line2 = stringResource(R.string.app_disable_detail_line2, appLabel)
    val line3 = stringResource(R.string.app_disable_detail_line3, record)

    return buildAnnotatedString {
        append(line1)
        append("\n\n")
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append(line2)
        }
        append("\n")
        withStyle(SpanStyle(fontWeight = FontWeight.Normal)) {
            append(line3)
        }
    }
}
