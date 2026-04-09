package com.clefie.melodies.ui

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.clefie.melodies.R
import com.clefie.melodies.viewmodel.FlowStep
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val JackOfGearsFamily = FontFamily(Font(R.font.jack_of_gears))
val PacificoFamily    = FontFamily(Font(R.font.pacifico))

fun gifImageLoader(context: android.content.Context) = ImageLoader.Builder(context)
    .components {
        if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory())
        else add(GifDecoder.Factory())
    }
    .build()

@Composable
fun PressableImageButton(
    assetPath: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "btnScale"
    )
    val btnAlpha by animateFloatAsState(
        targetValue   = if (pressed) 0.75f else 1f,
        animationSpec = tween(80),
        label         = "btnAlpha"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .alpha(btnAlpha)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = {
                        scope.launch {
                            pressed = true
                            delay(100)
                            pressed = false
                            delay(50)
                            onClick()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/$assetPath")
                .build(),
            contentDescription = null,
            modifier           = Modifier.fillMaxSize()
        )
    }
}

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
        VideoPlayer(
            assetPath = "images/Background_animated.webm",
            modifier  = Modifier.fillMaxSize().alpha(0.5f),
            loop      = true,
            speed     = 0.6f
        )

        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(vertical = 24.dp)
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/images/Logo_animated.gif")
                    .build(),
                imageLoader        = gifLoader,
                contentDescription = "Clefie Logo",
                modifier           = Modifier.fillMaxWidth(0.9f).aspectRatio(2.2f)
            )

            Spacer(Modifier.height(16.dp))

            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("file:///android_asset/images/Mascot_wave.gif")
                    .build(),
                imageLoader        = gifLoader,
                contentDescription = "Mascot waving",
                modifier           = Modifier.size(200.dp)
            )

            Spacer(Modifier.height(16.dp))

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

            Spacer(Modifier.height(24.dp))

            PressableImageButton(
                assetPath = "images/Button_create_my_sound.png",
                onClick   = onCreateSound,
                modifier  = Modifier
                    .fillMaxWidth(0.95f)
                    .height(130.dp)
                    .alpha(buttonAlphaAnim)
            )
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

    val fullText = "Creating your sound...\nGet ready to make something magical."

    var visibleChars     by remember { mutableStateOf(0) }
    var buttonAlpha      by remember { mutableStateOf(0f) }
    var showStaticMascot by remember { mutableStateOf(false) }

    // remembered flag — guarantees LaunchedEffect only ever runs once
    // even if the composable recomposes
    val started = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (started.value) return@LaunchedEffect
        started.value = true

        showStaticMascot = false
        visibleChars     = 0
        buttonAlpha      = 0f

        // Swap mascot to static after talk GIF plays once (~6.2s)
        launch {
            delay(6200)
            showStaticMascot = true
        }

        delay(800)
        fullText.forEachIndexed { index, _ ->
            delay(70)
            visibleChars = index + 1
        }
        delay(600)
        buttonAlpha = 1f
    }

    val screenAlpha by animateFloatAsState(
        targetValue   = if (step == FlowStep.ACTIVATION) 1f else 0f,
        animationSpec = tween(800),
        label         = "activationAlpha"
    )

    val buttonAlphaAnim by animateFloatAsState(
        targetValue   = buttonAlpha,
        animationSpec = tween(800),
        label         = "readyButtonAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha)
            .background(Color(0xFF361F30))
    ) {
        VideoPlayer(
            assetPath = "images/Background_animated.webm",
            modifier  = Modifier.fillMaxSize(),
            loop      = true,
            speed     = 1f
        )

        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))

        // Matched layout structure to IntroScreen — fillMaxSize Column, vertically centred
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(vertical = 24.dp)
        ) {
            // Mascot — talk once then static
            if (showStaticMascot) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("file:///android_asset/images/Mascot_complete.png")
                        .build(),
                    contentDescription = "Mascot",
                    modifier           = Modifier.size(260.dp)
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("file:///android_asset/images/Mascot_talk.gif")
                        .build(),
                    imageLoader        = gifLoader,
                    contentDescription = "Mascot talking",
                    modifier           = Modifier.size(260.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text  = fullText.take(visibleChars),
                style = TextStyle(
                    fontFamily = PacificoFamily,
                    fontSize   = 26.sp,
                    color      = Color.White,
                    textAlign  = TextAlign.Center,
                    lineHeight = 36.sp,
                    shadow     = Shadow(
                        color      = Color(0xFFE526AB),
                        offset     = Offset(0f, 4f),
                        blurRadius = 12f
                    )
                )
            )

            Spacer(Modifier.height(28.dp))

            PressableImageButton(
                assetPath = "images/Button_lets_create.png",
                onClick   = onReady,
                modifier  = Modifier
                    .fillMaxWidth(0.95f)
                    .height(130.dp)
                    .alpha(buttonAlphaAnim)
            )
        }
    }
}
