# Clefie Melodies — Complete Technical Handover

## 1. Project Overview
- Name: Clefie Melodies
- Platform: Android (Kotlin + Jetpack Compose)
- Build: Gradle 8.4, Java 17
- Purpose: Reactive biometric music app, premium feel, multi-sensor input
- Prototype: Biotune (Next.js/React) at github.com/Stremiouser666/Biotune

## 2. App Flow
1. intro — animated opening, animated background, logo, mascot waving, letter-by-letter text, CREATE MY SOUND button
2. activation — 3 second transition, animated background, mascot talking
3. magic — 4 second magic moment
4. dashboard — main screen with fullscreen visualizer button

## 3. Current Status

### Phase 1 COMPLETE
### Phase 2 COMPLETE
### Phase 3 COMPLETE

### Phase 4 IN PROGRESS
- FlowViewModel, IntroScreen, ActivationScreen, FullscreenVisualizer, MascotView, VideoPlayer all built
- GIFs loading via Coil + coil-gif
- WebM backgrounds via ExoPlayer
- Known issues being fixed — see UI Observations section below

## 4. UI Observations (fixes needed next session)

### Intro Screen
- Logo needs to be 50% bigger
- Background should be animated (Background_animated.webm) not static webp
- Text needs to be bigger with shadow
- Text animation needs to be smooth letter by letter, not word by word (was choppy)
- Button text needs to be centered and bold

### Activation / Magic Screen (Screen 2)
- Keep animated background
- Text animation choppy — needs smooth letter by letter, bigger font
- Text position needs to be slightly higher
- Mascot should be Mascot_talk.gif not dance
- Mascot position needs to be slightly higher

### Main / Dashboard Screen
- Clefie Melodies title needs to be twice the size, bold, and higher up

### Fullscreen Visualizer
- Needs major redo — leave for later session

## 5. Permission Delay Strategy (implement next session)
- Currently: permissions requested on app launch (bad UX)
- Fix: delay permission request until user taps CREATE MY SOUND
- Flow: intro screen shows with no permissions → user taps CREATE MY SOUND → request RECORD_AUDIO + BODY_SENSORS → on grant → start sensors → proceed to activation
- MainActivity should NOT call startSensors() or requestPermissions() in onCreate
- IntroScreen onCreateSound callback triggers permission request in MainActivity
- MainActivity.onRequestPermissionsResult → startSensors() → then proceed with flow

## 6. Memory / Rules
- Full file fixes only (no snippets)
- Termux: cat << 'EOF' > file — always include heredoc wrapper
- Always git commit + push after any fix
- Phases locked — work only on current phase tasks
- All code delivered via HTML artifact with one-tap COPY buttons
- Angle brackets in Kotlin must be HTML-encoded in artifacts

## 7. Phases

### Phase 4 — Intro Flow + Fullscreen Visualizer (IN PROGRESS)
See UI Observations for pending fixes.
Remaining after fixes:
- Fullscreen visualizer major redo
- Wire coil-gif dependency properly
- Sequencer dots driven by SequencerEngine step callback

### Phase 5 — Premium Feel
- Fingerprint auth via BiometricPrompt (unlock moment)
- Smooth flow transitions
- Presets: LO-FI, AMBIENT, UPTEMPO
- Chord mode: finger count -> complexity
- Scale maps: C/D/E/F/G pentatonic
- Swing amount, ambient mode, share session

### Phase 6 — Full Dashboard UI
- BiometricsSection, ScenesSection, MelodySection, RhythmSection
- SoundSection, EngineSection, MemorySection
- Sensitivity sliders, input toggles, debug overlay
- Onboarding tutorial (TutorialScreen.kt)
- Custom sprite font (Uppercase_font.png + Lowercase_font.png) — not finished yet

### Phase 7 — Packaging
- Performance, battery, permissions, release APK

## 8. Visual Identity
- Background: #361F30
- Primary: #E526AB
- Accent: #EE80FF
- Fonts: Jack of Gears (titles/BPM), Pacifico (body)
- Custom sprite font coming later (Phase 6)

## 9. Asset List (app/src/main/assets/images/)
- Mascot_body.png         — base layer, always visible
- Mascot_face.png         — idle face layer
- mascot_eyes.png         — blink layer
- Mascot_mouth.png        — static mouth
- Mascot_complete.png     — fallback single image
- Mascot_wave.gif         — wave state (idle/greeting)
- Mascot_talk.gif         — talking state (amplitude > 0.15)
- Mascot_dance.gif        — dance state (BPM > 100 or beatPulse)
- Logo_animated.gif       — intro reveal animation
- logo.png                — static logo
- Clefie_melodies.png     — full logo static
- Background_static.webp  — fallback static background
- Background_animated.webm — main app background (gears, speed = BPM)
- Background_dance.webm   — dance mode background (galaxy swirl)

Pending (not finished):
- For Android/Uppercase_font.png   — custom sprite font uppercase
- For Android/Lowercase_font.png   — custom sprite font lowercase
- For Android/Clefie_melodies.png  — logo variant

## 10. Fonts (app/src/main/res/font/)
- jack_of_gears.ttf — titles, BPM display, headers
- pacifico.ttf      — body text, labels, buttons

## 11. Dependencies (app/build.gradle)
- androidx.media3:media3-exoplayer:1.3.1
- androidx.media3:media3-ui:1.3.1
- androidx.media3:media3-datasource:1.3.1
- io.coil-kt:coil-compose:2.6.0
- io.coil-kt:coil-gif:2.6.0
- androidx.compose.material:material-icons-extended:1.5.0

## 12. CI / Workflows
- android.yml — builds on every push, no APK
- release.yml — manual trigger, produces APK

## 13. Git Commit Convention
Phase1/2/3/4/5/6/7: description
Fix: description
Docs: description

## 14. Biotune Prototype Reference
Repo: github.com/Stremiouser666/Biotune
Files reviewed: page.tsx, mascot.tsx, fullscreen-visualizer.tsx,
audio-engine.ts, TutorialScreen.tsx, blueprint.md
