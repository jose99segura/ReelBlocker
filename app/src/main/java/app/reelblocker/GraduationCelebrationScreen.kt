package app.reelblocker

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Spread
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

/**
 * Pantalla ceremonial del día 30. Sustituye al [GraduationDialog] modal
 * por una experiencia full-screen con konfetti procedural, mascota grande,
 * y botón compartir-texto (marketing viral gratis).
 *
 * El botón "Compartir" lanza un Intent.ACTION_SEND con texto plano que
 * incluye días + especie + nombre de la app. Cualquier app de mensajería,
 * red social o email lo puede recibir.
 */
@Composable
fun GraduationCelebrationScreen(
    graduatedSpecies: MascotSpecies,
    daysReached: Int,
    onContinue: () -> Unit
) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val accent = graduatedSpecies.accentTint

    // Back físico = mismo efecto que tap "Continuar" — confirma la celebración.
    BackHandler { onContinue() }

    val parties = remember(accent) {
        listOf(
            Party(
                speed = 0f,
                maxSpeed = 30f,
                damping = 0.9f,
                angle = Angle.BOTTOM,
                spread = Spread.WIDE,
                colors = listOf(
                    accent.toArgb(),
                    accent.lighten(0.3f).toArgb(),
                    accent.darken(0.15f).toArgb(),
                    0xFFFEF3C7.toInt(),
                    0xFFFCD34D.toInt()
                ),
                position = Position.Relative(0.5, 0.0),
                emitter = Emitter(duration = 1500, TimeUnit.MILLISECONDS).perSecond(120)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top label
            Text(
                text = stringResource(R.string.graduation_celebration_label).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

            // Mascota + textos centrales
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    MascotCanvas(
                        level = MascotLevel.ADULT,
                        species = graduatedSpecies,
                        animate = true,
                        modifier = Modifier.size(220.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = stringResource(
                        R.string.graduation_title,
                        stringResource(graduatedSpecies.displayNameRes)
                    ),
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.graduation_body, daysReached),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.graduation_body_secondary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Botones
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val shareLabel = stringResource(R.string.graduation_celebration_share)
                val shareText = stringResource(
                    R.string.graduation_share_text,
                    daysReached,
                    stringResource(graduatedSpecies.displayNameRes)
                )
                val chooserLabel = stringResource(R.string.graduation_share_chooser)
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        ctx.startActivity(Intent.createChooser(send, chooserLabel))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = shareLabel,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onContinue()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent)
                ) {
                    Text(
                        text = stringResource(R.string.graduation_celebration_continue),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }

        // Konfetti — overlay encima de todo, no captura toques.
        KonfettiView(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            parties = parties
        )
    }
}

