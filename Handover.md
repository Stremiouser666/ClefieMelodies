# Clefie Melodies — Complete Technical Handover

## 1. Project Overview
- Name: Clefie Melodies
- Platform: Android (Kotlin + Jetpack Compose)
- Build: Gradle 8.4, Java 17, minSdk 24 (Android 10+)
- Purpose: Reactive biometric music app, premium feel, multi-sensor input
- Prototype: Biotune (Next.js/React) at github.com/Stremiouser666/Biotune

## 2. App Flow
1. intro — black window bg, animated background, logo GIF, mascot waving GIF, letter-by-letter text, Create my Sound button (PNG)
2. activation — animated background, mascot talk GIF plays once then holds static, letter-by-letter text, Lets Create button (PNG) appears after ALL text done
3. magic — 2 second transition (triggered by button press only, no auto-advance)
4. dashboard — main screen with fullscreen visualizer button
5. fullscreen — canvas visualizer, animated backgrounds, mascot, aura, particles

## 3. Current Status

### Phase 1 COMPLETE
### Phase 2 COMPLETE
### Phase 3 COMPLETE

### Phase 4 IN PROGRESS — most fixes done
Completed:
- FlowViewModel screen state machine
- IntroScreen — animated bg, logo GIF, wave GIF loops, letter text 70ms, PNG button full width with press scale effect
- ActivationScreen — talk GIF plays once (6.2s) then swaps to Mascot_complete.png, letter text 70ms, button after ALL text, no auto-advance
- FullscreenVisualizer — canvas aura, particles, rotating rings, mascot states, animated backgrounds, HUD
- MascotView — wave/talk/dance/idle states
- VideoPlayer — ExoPlayer WebM backgrounds with speed control
- Back navigation — one step back, does nothing on intro
- Black window background (no splash for now)
- Custom PNG steampunk buttons with press animation
- Coil + coil-gif for GIF and PNG loading

Remaining:
- Fullscreen visualizer major redo (deferred)
- Visualizer PNG frame overlay (steampunk observatory frame)

## 4. Memory / Rules
- Full file fixes only (no snippets)
- Termux: cat << 'EOF' > file — always include heredoc wrapper
- Always git commit + push after any fix
- Phases locked — work only on current phase tasks
- All code delivered via HTML artifact with one-tap COPY buttons
- Angle brackets in Kotlin must be HTML-encoded in artifacts
- GIF loop control: use static image swap not repeatCount() API
- Coil GIF: gifImageLoader() for loop forever, static swap for play-once

## 5. Phases

### Phase 4 IN PROGRESS
See above. Next: visualizer redo + frame overlay.

### Phase 5 — Premium Feel
- Fingerprint auth via BiometricPrompt
- Smooth flow transitions
- Presets: LO-FI, AMBIENT, UPTEMPO
- Chord mode: finger count -> complexity
- Scale maps: C/D/E/F/G pentatonic
- Swing, ambient mode, share session

### Phase 6 — Full Dashboard UI
- BiometricsSection, ScenesSection, MelodySection, RhythmSection
- SoundSection, EngineSection, MemorySection
- Sensitivity sliders, input toggles, debug overlay
- Onboarding tutorial (TutorialScreen.kt)
- Custom sprite font (Phase 6)

### Phase 7 — Packaging
- Performance, battery, permissions, release APK

## 6. Visual Identity
- Background: #361F30
- Primary: #E526AB
- Accent: #EE80FF
- Fonts: Jack of Gears (titles/BPM), Pacifico (body)
- Window background: black (#000000)

## 7. Asset List (app/src/main/assets/images/)
- Mascot_body.png
- Mascot_face.png
- mascot_eyes.png
- Mascot_mouth.png
- Mascot_complete.png         — static fallback / post-talk hold frame
- Mascot_wave.gif             — loops forever on intro
- Mascot_talk.gif             — plays once (6.2s) then swaps to Mascot_complete.png
- Mascot_dance.gif            — dance state (BPM > 100 or beatPulse)
- Logo_animated.gif           — loops on intro
- logo.png                    — static logo
- Button_create_my_sound.png  — intro screen button (transparent PNG)
- Button_lets_create.png      — screen 2 button (transparent PNG)
- Background_static.webp      — fallback
- Background_animated.webm    — main app (gears, speed = BPM)
- Background_dance.webm       — dance mode (galaxy swirl)

Pending:
- Visualizer_frame.png        — steampunk observatory frame overlay for visualizer

## 8. Fonts (app/src/main/res/font/)
- jack_of_gears.ttf
- pacifico.ttf

## 9. Dependencies (app/build.gradle)
- androidx.media3:media3-exoplayer:1.3.1
- androidx.media3:media3-ui:1.3.1
- androidx.media3:media3-datasource:1.3.1
- io.coil-kt:coil-compose:2.6.0
- io.coil-kt:coil-gif:2.6.0
- androidx.compose.material:material-icons-extended:1.5.0

## 10. CI / Workflows
- android.yml — builds on every push, no APK
- release.yml — manual trigger, produces APK

## 11. Git Commit Convention
Phase1/2/3/4/5/6/7: description
Fix: description
Docs: description

## 12. Biotune Reference
Repo: github.com/Stremiouser666/Biotune
Files reviewed: page.tsx, mascot.tsx, fullscreen-visualizer.tsx,
audio-engine.ts, TutorialScreen.tsx, blueprint.md
