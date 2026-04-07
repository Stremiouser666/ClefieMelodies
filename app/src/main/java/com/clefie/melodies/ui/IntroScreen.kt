package com.clefie.melodies.ui

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.clefie.melodies.R
import com.clefie.melodies.viewmodel.FlowStep
import kotlinx.coroutines.delay

val JackOfGearsFamily = FontFamily(Font(R.font.jack_of_gears))
val PacificoFamily    = FontFamily(Font(R.font.pacifico))

fun gifImageLoader(context: android.content.Context) = ImageLoader.Builder(context)
    .components {
        if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
        else add(GifDecoder.Factory())
    }
    .build()

@Composable
fun IntroScreen(
    step: FlowStep,
    onCreateSound: () -> Unit
) {
    val context   = LocalContext.current
    val gifLoader = remember { gifImageLoader(context) }

    val fullText = "Feel the music. Live the moment."
    var visibleChars by remember { mutableStateOf(0) }
    var buttonAlpha  by remember { mutableStateOf(0f) }

    val buttonAlphaAnim by animateFloatAsState(
        targetValue   = buttonAlpha,
        animationSpec = tween(1000),
        label         = "buttonAlpha"
    )

    val screenAlpha by animateFloatAsState(
        targetValue   = if (step == FlowStep.INTRO) 1f else 0f,
        animationSpec = tween(800),
        label         = "screenAlpha"
    )

    LaunchedEffect(Unit) {
        delay(600)
        fullText.forEachIndexed { index, _ ->
            delay(70)
            visibleChars = index + 1
        }
        delay(500)
        buttonAlpha = 1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha)
            .background(Color(0xFF361F30)),
        contentAlignment = Alignment.Center
    ) {
        // Animated background
        VideoPlayer(
            assetPath = "images/Background_animated.webm",
            modifier  = Modifier.fillMaxSize().alpha(0.5f),
            loop      = true,
            speed     = 0.6f
        )

        // Dark overlay
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            // Animated logo — loops forever
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/images/Logo_animated.gif")
                    .build(),
                imageLoader        = gifLoader,
                contentDescription = "Clefie Logo",
                modifier           = Modifier.fillMaxWidth(0.9f).aspectRatio(2.2f)
            )

            // Mascot waving — loops forever
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/images/Mascot_wave.gif")
                    .build(),
                imageLoader        = gifLoader,
                contentDescription = "Mascot waving",
                modifier           = Modifier.size(200.dp)
            )

            // Letter by letter text
            Text(
                text  = fullText.take(visibleChars),
                style = TextStyle(
                    fontFamily = PacificoFamily,
                    fontSize   = 26.sp,
                    color      = Color.White,
                    textAlign  = TextAlign.Center,
                    lineHeight = 38.sp,
                    shadow     = Shadow(
                        color      = Color(0xFFE526AB),
                        offset     = Offset(0f, 4f),
                        blurRadius = 12f
                    )
                )
            )

            Spacer(Modifier.height(8.dp))

            // Custom PNG button — Create my Sound
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .aspectRatio(3.2f)
                    .alpha(buttonAlphaAnim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null
                    ) { onCreateSound() },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("file:///android_asset/images/Button_create_my_sound.png")
                        .build(),
                    contentDescription = "Create my Sound",
                    modifier           = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun ActivationScreen(
    step: FlowStep,
    onReady: () -> Unit
) {
    val context   = LocalContext.current
    val gifLoader = remember { gifImageLoader(context) }

    val fullText = if (step == FlowStep.MAGIC)
        "Your sound is alive..." else "Creating your sound..."

    var visibleChars by remember(step) { mutableStateOf(0) }
    var buttonAlpha  by remember(step) { mutableStateOf(0f) }

    val buttonAlphaAnim by animateFloatAsState(
        targetValue   = buttonAlpha,
        animationSpec = tween(800),
        label         = "readyButtonAlpha"
    )

    // Talk GIF plays twice — approx duration of 2 loops before button shows
    // Coil doesn't expose loop count directly so we time it:
    // text reveal + 500ms delay before button appears after text fully done
    LaunchedEffect(step) {
        visibleChars = 0
        buttonAlpha  = 0f
        fullText.forEachIndexed { index, _ ->
            delay(70)
            visibleChars = index + 1
        }
        // Wait for text to fully finish before showing button
        delay(600)
        buttonAlpha = 1f
    }

    val alpha by animateFloatAsState(
        targetValue   = if (step == FlowStep.ACTIVATION || step == FlowStep.MAGIC) 1f else 0f,
        animationSpec = tween(800),
        label         = "activationAlpha"
    )

    if (alpha > 0f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha)
                .background(Color(0xFF361F30))
        ) {
            // Animated background
            VideoPlayer(
                assetPath = "images/Background_animated.webm",
                modifier  = Modifier.fillMaxSize(),
                loop      = true,
                speed     = 1f
            )

            // Dark overlay
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-40).dp)
            ) {
                // Mascot talking — plays twice via timed repeatCount
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("file:///android_asset/images/Mascot_talk.gif")
                        .repeatCount(1)  // 0 = once, 1 = twice, INFINITE = forever
                        .build(),
                    imageLoader        = gifLoader,
                    contentDescription = "Mascot talking",
                    modifier           = Modifier.size(240.dp)
                )

                Spacer(Modifier.height(24.dp))

                // Letter by letter text
                Text(
                    text  = fullText.take(visibleChars),
                    style = TextStyle(
                        fontFamily = PacificoFamily,
                        fontSize   = 26.sp,
                        color      = Color.White,
                        textAlign  = TextAlign.Center,
                        shadow     = Shadow(
                            color      = Color(0xFFE526AB),
                            offset     = Offset(0f, 4f),
                            blurRadius = 12f
                        )
                    ),
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(Modifier.height(28.dp))

                // Custom PNG button — Let's Create
                // Only appears after ALL text has finished
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .aspectRatio(3.2f)
                        .alpha(buttonAlphaAnim)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) { onReady() },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data("file:///android_asset/images/Button_lets_create.png")
                            .build(),
                        contentDescription = "Let's Create",
                        modifier           = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
