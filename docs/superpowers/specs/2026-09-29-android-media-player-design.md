# Design Spec: Android Powerful Media Player (Local-Only)
**Date:** 2026-09-29
**Status:** Draft for Review
**Target Platform:** Android

## 1. Executive Summary
A high-performance, local-only Android media player designed to play virtually any media format by combining the modern Android playback ecosystem with the raw power of FFmpeg. The app focuses on a "power-user" experience with gesture-based controls, advanced audio processing, and robust playlist management.

## 2. Core Objectives
- **Universal Playback:** Support all common and rare codecs via a hybrid engine.
- **Modern UX:** Provide an intuitive, gesture-driven interface for primary playback controls.
- **High-Fidelity Audio:** Implement a professional-grade equalizer and audio effects chain.
- **Efficient Local Management:** Rapid indexing and organization of local device media.

## 3. Technical Architecture

### 3.1 High-Level Structure
The application follows a layered architecture to ensure separation of concerns and UI responsiveness.

- **UI Layer (Kotlin/Jetpack Compose):**
    - Reactive UI observing a central state.
    - Gesture Overlay for volume, brightness, and seeking.
    - Playlist and Library management views.
- **Controller Layer (ExoPlayer):**
    - Manages playback state, buffering, and media session.
    - Orchestrates the transition between native and software rendering.
- **Codec Extension Layer (JNI/C++):**
    - Custom `RenderersFactory` to intercept unsupported formats.
    - JNI bridge to pass raw packets to the native engine.
- **Native Engine (FFmpeg):**
    - C++ core for decoding raw bitstreams into YUV/PCM buffers.
    - Hardware acceleration hooks where available.

### 3.2 Data Flow
`Local File` $\rightarrow$ `ExoPlayer` $\rightarrow$ `FFmpeg (if necessary)` $\rightarrow$ `Android Surface/AudioTrack` $\rightarrow$ `User`.

## 4. Component Specifications

### 4.1 The Native Bridge
- **Mechanism:** Implementation of a custom `MediaCodecVideoRenderer` and `MediaCodecAudioRenderer`.
- **Fallback Logic:** If `MediaCodec` fails to instantiate for a given MIME type, the app switches to the FFmpeg-backed software renderer.
- **Buffer Management:** Use of a ring-buffer in the C++ layer to ensure a steady stream of decoded frames, preventing UI stutter.

### 4.2 Gesture Engine
| Gesture | Action | Implementation |
| :--- | :--- | :--- |
| Vertical Swipe (Left) | Volume Control | `AudioManager.setStreamVolume` |
| Vertical Swipe (Right) | Brightness Control | `WindowManager.LayoutParams.screenBrightness` |
| Horizontal Swipe (Bottom) | Seek (FF/RW) | `player.seekTo()` with variable offset |
| Double Tap (Left/Right) | Jump $\pm 10$s | Fixed seek offset |

### 4.3 Audio Processing
- **Equalizer:** 5-band frequency adjustment using the Android `Equalizer` API.
- **Dynamics Processing:** Loudness normalization to maintain consistent volume across different files.
- **Effects Chain:** Bass Boost and Virtualizer effects integrated into the ExoPlayer `AudioSink`.

### 4.4 Library & Playlist Management
- **Indexing:** SQLite database via **Room** to store metadata (file path, duration, artist, album) for all detected local media.
- **Queue Management:** A `LinkedHashMap` implementation in the ViewModel to handle shuffling, repeating, and custom queue ordering without modifying the DB.

## 5. State & Threading Model
- **Unidirectional Data Flow (UDF):**
    - `PlayerViewModel` $\rightarrow$ `StateFlow<PlayerState>` $\rightarrow$ `Compose UI`.
    - `UI Event` $\rightarrow$ `ViewModel` $\rightarrow$ `ExoPlayer`.
- **Threading Strategy:**
    - **Main Thread:** UI and Gesture interaction.
    - **Player Thread:** ExoPlayer state and buffering.
    - **Decoding Thread:** Dedicated background C++ thread for FFmpeg decoding to eliminate UI jank.

## 6. Success Criteria
- [ ] App launches and indexes local media within 2 seconds.
- [ ] Files that fail in standard Android players (e.g., certain .mkv or .flac versions) play smoothly.
- [ ] Gesture controls respond with $< 100$ms latency.
- [ ] Audio equalizer changes are audible in real-time.
