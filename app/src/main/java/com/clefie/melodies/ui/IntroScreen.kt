package com.clefie.melodies.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.clefie.melodies.R
import com.clefie.melodies.viewmodel.FlowStep
import kotlinx.coroutines.delay

val JackOfGearsFamily = FontFamily(Font(R.font.jack_of_gears))
val PacificoFamily    = FontFamily(Font(R.font.pacifico))

@Composable
fun IntroScreen(
    step: FlowStep,
    onCreateSound: () -> Unit
) {
    val context = LocalContext.current

    // Word-by-word text reveal
    val words = listOf("Feel", "the", "music.", "Live", "the", "moment.")
    var visibleWords by remember { mutableStateOf(0) }

    // Button fade in
    var buttonAlpha by remember { mutableStateOf(0f) }
    val buttonAlphaAnim by animateFloatAsState(
        targetValue = buttonAlpha,
        animationSpec = tween(1000),
        label = "buttonAlpha"
    )

    // Screen overall alpha
    val screenAlpha by animateFloatAsState(
        targetValue = if (step == FlowStep.INTRO) 1f else 0f,
        animationSpec = tween(800),
        label = "screenAlpha"
    )

    LaunchedEffect(Unit) {
        delay(800)
        words.forEachIndexed { index, _ ->
            delay(350)
            visibleWords = index + 1
        }
        delay(600)
        buttonAlpha = 1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha)
            .background(Color(0xFF361F30)),
        contentAlignment = Alignment.Center
    ) {
        // Background image — blurred, dark
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/images/Background_static.webp")
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.3f)
                .blur(12.dp)
        )

        // Sparkle dot overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.08f)
                .background(Color.White)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated logo
            VideoPlayer(
                assetPath = "images/Logo_animated.webm",
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .aspectRatio(2.5f),
                loop = true,
                speed = 1f
            )

            // Mascot waving
            MascotView(
                amplitude = 0f,
                bpm = 80f,
                beatPulse = false,
                holdIntensity = 0f,
                tiltX = 0f,
                tiltY = 0f,
                forceWave = true,
                modifier = Modifier.size(220.dp)
            )

            // Word by word text
            Text(
                text = words.take(visibleWords).joinToString(" "),
                fontFamily = PacificoFamily,
                fontSize = 22.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp
            )

            Spacer(Modifier.height(8.dp))

            // CREATE MY SOUND button
            Button(
                onClick = onCreateSound,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(60.dp)
                    .alpha(buttonAlphaAnim),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE526AB),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text(
                    text = "CREATE MY SOUND",
                    fontFamily = JackOfGearsFamily,
                    fontSize = 18.sp,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}

@Composable
fun ActivationScreen(step: FlowStep) {
    val alpha by animateFloatAsState(
        targetValue = if (step == FlowStep.ACTIVATION || step == FlowStep.MAGIC) 1f else 0f,
        animationSpec = tween(800),
        label = "activationAlpha"
    )

    if (alpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
                .background(Color(0xFF361F30)),
            contentAlignment = Alignment.Center
        ) {
            VideoPlayer(
                assetPath = "images/Background_animated.webm",
                modifier = Modifier.fillMaxSize(),
                loop = true,
                speed = 1f
            )

            MascotView(
                amplitude = 0.5f,
                bpm = 120f,
                beatPulse = true,
                holdIntensity = 0.3f,
                tiltX = 0f,
                tiltY = 0f,
                modifier = Modifier.size(280.dp)
            )

            Text(
                text = if (step == FlowStep.MAGIC) "Your sound is alive..." else "Creating your sound...",
                fontFamily = PacificoFamily,
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            )
        }
    }
}
