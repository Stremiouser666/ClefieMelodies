# Clefie Melodies — Complete Technical Handover

## 1. Project Overview
- Name: Clefie Melodies
- Platform: Android (Kotlin)
- Build: Gradle 8.4, Java 17
- Purpose: Reactive music visualizer, premium feel, multi-sensor input

## 2. Current Status (Phase 1 — COMPLETE)
- SensorController.kt — StateFlow-based, accelerometer + gyroscope + proximity working
- Manifest fixed — RECORD_AUDIO + BODY_SENSORS permissions declared
- XML files fixed — colours.xml, themes.xml (light + night) valid
- AppCompat dependency added to build.gradle
- Build passing on GitHub Actions
- Mic stub wired through stack — ready for Phase 2 implementation

## 3. Memory / Rules
- Full file fixes only (no snippets)
- Termux-ready: cat << 'EOF' > file — always include heredoc wrapper
- Always include git commit + push after any fix
- Phases are locked: work only on current phase tasks
- SensorController is central hub for all input (accelerometer, gyroscope, proximity, mic, touch)
- All code delivered via HTML artifact with one-tap COPY buttons (includes heredoc wrapper)

## 4. Phases & Tasks

### Phase 1 — Core Stability COMPLETE
- Goal: stable, continuous inputs
- Tasks completed: accelerometer, gyroscope, proximity wired as StateFlow
- Mic stub in place (real implementation in Phase 2)
- Build passing

### Phase 2 — Audio + Beat Engine
- Real mic input via AudioRecord
- Amplitude -> affects BPM
- BPM detection (onset/energy-based)
- Beat timing
- Tap-to-beat input

### Phase 3 — Motion + Interaction
- Motion mapping (accelerometer + gyroscope -> visual parameters)
- Gestures: shake, tilt, hold
- Hold finger on screen = energy boost
- Proximity triggers
- Multi-touch input:
  - Swipe -> pitch
  - Hold -> intensity
  - Pressure -> energy level (with fallback for devices that return constant 1.0)
  - Velocity tracking -> expression/dynamics
  - Multi-finger -> chords
  - Uses MotionEvent.getPressure() with graceful degradation

### Phase 4 — Visual Engine (Mascot)
- Map BPM -> animation speed
- Map motion -> position / scale / pulse
- Map touch/hold -> intensity
- Idle animations

### Phase 5 — Premium Feel Layer
- Smooth transitions, micro-interactions
- Visual FX (glow, pulse)
- Input blending without jitter
- Biometric fingerprint auth via BiometricPrompt API:
  - Fingerprint scan = app unlock moment
  - Auth tied to first beat drop / visual burst
  - Feels like a real premium unlock experience
  - BiometricPrompt.authenticate() -> onAuthenticationSucceeded -> trigger start sequence

### Phase 6 — UI / UX
- Sensitivity sliders
- Toggle inputs (mic, motion, tap, hold, proximity)
- Optional debug overlay

### Phase 7 — Packaging
- Optimize performance
- Reduce battery drain
- Clean permissions
- Build release APK

## 5. Source Files
- AndroidManifest.xml
- app/build.gradle
- app/src/main/res/values/colours.xml
- app/src/main/res/values/themes.xml
- app/src/main/res/values-night/themes.xml
- app/src/main/java/com/clefie/melodies/MainActivity.kt
- app/src/main/java/com/clefie/melodies/sensor/SensorController.kt
- app/src/main/java/com/clefie/melodies/viewmodel/MainViewModel.kt
- app/src/main/java/com/clefie/melodies/ui/MainScreen.kt
- app/src/main/java/com/clefie/melodies/engine/SequencerEngine.kt

## 6. Next Steps (Phase 2)
- Replace mic stub in SensorController with real AudioRecord loop
- Run on background thread (avoid blocking main/sensor threads)
- Emit RMS amplitude as StateFlow
- Feed amplitude into BPM calculation in MainViewModel

## 7. Git Commit Convention
Phase1: description
Phase2: description
Fix: description

## 8. Git Push Example
git add .
git commit -m "Phase1: complete — sensors, build fixes, StateFlow wiring"
git push
