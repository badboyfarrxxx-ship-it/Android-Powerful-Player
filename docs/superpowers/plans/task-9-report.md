# Task 9 Report: Final Validation & Stress Test

## 1. Automated Validation Results

### Audio Focus Handling
Implemented in `tests/integration/StressTest.kt`. The test mocks the `AudioManager` to simulate `AUDIOFOCUS_LOSS` and `AUDIOFOCUS_GAIN`. 
- **Verification:** Confirmed that the player invokes `pause()` upon focus loss and `resume()` upon focus gain.

### Corrupted File Handling ("Evil" Tests)
Implemented in `tests/integration/StressTest.kt`. The test creates a malformed file (text renamed to `.mp3`) and attempts playback.
- **Verification:** Confirmed that the player transitions to `PlaybackState.ERROR` rather than crashing the JVM/ART process.

## 2. Manual Stress Test Plan

### Soak Test (Memory Leak Check)
- **Duration:** 2 hours of continuous playback.
- **Method:** 
    - Loop a high-bitrate 4K video file.
    - Navigate between the Library view and Player view every 5 minutes.
    - Toggle the Equalizer and Gesture controls repeatedly.
- **Metrics to Monitor (Android Studio Profiler):**
    - **Memory Heap:** Look for a "sawtooth" pattern. A steady increase in the baseline (bottom of the sawtooth) indicates a memory leak.
    - **Native Memory:** Since FFmpeg is used via JNI, monitor `Native` memory specifically to ensure C++ buffers are being freed.
    - **GC Events:** Monitor frequency of Garbage Collection; excessive GC indicates memory pressure.

### AV Sync Stress Test (Lip-Sync Drift)
- **Duration:** 1 hour of playback at 1.5x speed.
- **Method:**
    - Play a "sync-test" video (e.g., a ticking clock or a person clapping).
    - Record the device screen and system audio using a high-resolution external capture card.
- **Measurement:**
    - Use a video editor (e.g., Premiere or DaVinci Resolve) to align the audio peak of a clap with the visual frame of the clap.
    - **Pass Criteria:** Drift must remain below $\pm 40$ms.
    - **Analysis:** If drift increases linearly over time, the internal clock synchronization between the FFmpeg decoder and the Android `AudioTrack` needs adjustment.

## 3. Conclusion
The automated infrastructure is in place to prevent regressions in basic stability and system integration. The manual plan provides a rigorous path to certify "power-user" grade performance.
