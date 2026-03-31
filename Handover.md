# Clefie Melodies — Complete Technical Handover

## 1. Project Overview
- Name: Clefie Melodies
- Platform: Android (Kotlin)
- Build: Gradle 8.4, Java 17
- Purpose: Reactive music visualizer, premium feel, multi-sensor input
- Prototype: Biotune (Next.js/React) at github.com/Stremiouser666/Biotune

## 2. Current Status

### Phase 1 COMPLETE
- SensorController.kt — StateFlow-based, accelerometer + gyroscope + proximity
- Manifest — RECORD_AUDIO + BODY_SENSORS permissions
- XML files — colours, themes (light + night) valid
- AppCompat dependency added
- Build passing on GitHub Actions

### Phase 2 COMPLETE
- Real mic input via AudioRecord — RMS amplitude as StateFlow
- BeatDetector.kt — energy onset detection, smoothed stable BPM
- TapTempo.kt — tap to set BPM + tap to sync phase
- BPM priority: tap tempo > beat detection > mic/motion fallback

### Phase 3 COMPLETE
- GestureController.kt — swipe velocity (signed), pitch (vertical), hold, pressure, finger count
- Shake detection — threshold 22f, 800ms cooldown
- Tilt X/Y — with 3 degree dead zone
- Velocity wired into BPM
- All gesture/sensor data exposed as StateFlows

## 3. Memory / Rules
- Full file fixes only (no snippets)
- Termux-ready: cat << 'EOF' > file — always include heredoc wrapper
- Always include git commit + push after any fix
- Phases are locked: work only on current phase tasks
- SensorController is central hub for all sensor input
- GestureController handles all touch input
- All code delivered via HTML artifact with one-tap COPY buttons
- Angle brackets in Kotlin must be HTML-encoded in artifacts (< >)

## 4. Phases & Tasks

### Phase 1 COMPLETE
### Phase 2 COMPLETE
### Phase 3 COMPLETE

### Phase 4 — Visual Engine (Mascot) — NEXT
- Full Canvas composable — fullscreen behind mascot
- Background: #361F30 + background image asset
- Radial aura — frequency bars in circle, driven by amplitude
- Particle burst — on beatPulse + shake
- Rotating energy rings — driven by time + BPM
- Heart gem pulse — magenta glow on every beatPulse
- Mascot states:
  - Idle + wave — low amplitude, no beat
  - Talking mouth — mic amplitude > threshold
  - Dancing — BPM > 100 or beatPulse
  - Scale up — intensity/holdIntensity
  - Body drift — tiltX/tiltY offsets position
- 16-step sequencer dots at bottom
- Ambient mode (moon button) — dims everything
- BPM + intensity display
- Logo watermark (low opacity)
- Colour palette: bg #361F30, primary #E526AB, accent #EE80FF
- Font: PT Sans

### Phase 5 — Premium Feel Layer
- Smooth transitions, micro-interactions
- Visual FX (glow, pulse)
- Input blending without jitter
- Biometric fingerprint auth via BiometricPrompt API:
  - Fingerprint scan = app unlock moment
  - Auth tied to first beat drop / visual burst
  - BiometricPrompt.authenticate() -> onAuthenticationSucceeded -> trigger start sequence
- Presets: LO-FI, AMBIENT, UPTEMPO (from Biotune audio engine)
- Chord mode: finger count -> chord complexity (1=single, 2=add 4th, 3=full chord)
- Scale maps: C/D/E/F/G pentatonic (from Biotune)
- Swing amount

### Phase 6 — UI / UX
- Sensitivity sliders (mic, motion)
- Toggle inputs (mic, motion, tap, hold, proximity)
- Optional debug overlay
- Onboarding tutorial screen (first launch only):
  - Mascot in corner with speech bubble
  - Steps: Welcome, Make a Melody, Add Beats, AI Mode, Visuals, Ready
  - Each step highlights relevant UI element with cyan glow
  - Mascot state/intensity/dancing changes per step
  - Don't show again saved to SharedPreferences
  - Progress dots at bottom, Back/Next/Start buttons
  - Stored in TutorialScreen.kt

### Phase 7 — Packaging
- Optimize performance
- Reduce battery drain
- Clean permissions
- Build release APK

## 5. Visual Identity
- Background colour: #361F30 (dark magenta-purple)
- Primary colour: #E526AB (deep magenta)
- Accent colour: #EE80FF (bright violet)
- Font: PT Sans (headline + body)
- Logo: Clefie jewelled gold filigree lettering, pink heart gem on i dot
- Background image: https://i.postimg.cc/nhW8Thn8/Background.png

## 6. Mascot Assets
New mascot: gold jewelled DNA helix body, pink heart gem centrepiece, expressive cartoon eyes
All assets transparent PNG, hosted on postimg

- mascot_body.png — base layer
- mascot_face.png — expression layer (blinks, reacts)
- mascot_wave.png — idle/greeting animation frame
- mascot_mouth.png — talking mouth, reacts to mic amplitude
- mascot_dance.png — dancing state, triggers BPM > 100 or beatPulse

Current hosted assets (from Biotune prototype):
- Body: https://i.postimg.cc/CxDqyny4/Mascot_Body.png
- Face: https://i.postimg.cc/4xt9CHCv/Mascot_Face.png
- Background: https://i.postimg.cc/nhW8Thn8/Background.png

New mascot (Phase 4 target):
- Body: https://i.postimg.cc/FK6vK3ky/1774849240243.png

## 7. Source Files
- AndroidManifest.xml
- app/build.gradle
- app/src/main/res/values/colours.xml
- app/src/main/res/values/themes.xml
- app/src/main/res/values-night/themes.xml
- app/src/main/java/com/clefie/melodies/MainActivity.kt
- app/src/main/java/com/clefie/melodies/sensor/SensorController.kt
- app/src/main/java/com/clefie/melodies/sensor/GestureController.kt
- app/src/main/java/com/clefie/melodies/viewmodel/MainViewModel.kt
- app/src/main/java/com/clefie/melodies/ui/MainScreen.kt
- app/src/main/java/com/clefie/melodies/engine/SequencerEngine.kt
- app/src/main/java/com/clefie/melodies/engine/BeatDetector.kt
- app/src/main/java/com/clefie/melodies/engine/TapTempo.kt

## 8. CI / Workflows
- .github/workflows/android.yml — builds on every push, no APK
- .github/workflows/release.yml — manual trigger only, produces APK
  To get APK: GitHub -> Actions -> Build Release APK -> Run workflow

## 9. Git Commit Convention
Phase1: description
Phase2: description
Phase3: description
Fix: description
Docs: description

## 10. Biotune Prototype Reference
Repo: github.com/Stremiouser666/Biotune
Key files ported/referenced:
- mascot.tsx -> Phase 4 Compose Canvas mascot
- fullscreen-visualizer.tsx -> Phase 4 visual engine
- audio-engine.ts -> Phase 5 audio presets + chord/scale logic
- TutorialScreen -> Phase 6 onboarding
