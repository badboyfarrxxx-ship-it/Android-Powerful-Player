# Task 2 Implementation Report: Central State Management

## Goal
Implement the central state management for the media player using Unidirectional Data Flow (UDF).

## Implementation Details

### 1. PlayerState
Created `player/PlayerState.kt` as a data class to represent the UI state.
- **Properties**:
    - `isPlaying: Boolean`: Track playback status.
    - `currentPosition: Long`: Current playback head position.
    - `duration: Long`: Total length of the current track.
    - `currentTrack: MediaEntity?`: Reference to the active media file.
    - `playbackState: PlaybackState`: Enum representing `IDLE`, `BUFFERING`, `READY`, `ENDED`.

### 2. PlayerViewModel
Created `player/PlayerViewModel.kt` to manage state transitions.
- **State Exposure**: Uses `MutableStateFlow` internally and exposes a read-only `StateFlow<PlayerState>` for the UI layer.
- **Intents**:
    - `play()`: Sets `isPlaying` to `true`.
    - `pause()`: Sets `isPlaying` to `false`.
    - `seekTo(position: Long)`: Updates `currentPosition`.
    - `setCurrentTrack(track: MediaEntity)`: Sets the active track, resets position to `0`, and sets duration from the entity.

### 3. Testing
Implemented `tests/player/PlayerViewModelTest.kt` using JUnit and Coroutines `runTest`.
- **Verified Transitions**:
    - Play/Pause toggle logic.
    - Seek position updates.
    - Track change resets (position/duration).

## Deliverables
- `player/PlayerState.kt`
- `player/PlayerViewModel.kt`
- `tests/player/PlayerViewModelTest.kt`
