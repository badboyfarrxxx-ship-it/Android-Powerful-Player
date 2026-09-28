# Task 5 Implementation Report: Native Decoding Core and Frame Buffering

## Overview
Implemented the high-performance native decoding core and the associated thread-safe buffering system for the Android Powerful Media Player. This layer sits between the FFmpegBridge (packet provider) and the rendering engine (frame consumer).

## Implementation Details

### 1. Thread-Safe RingBuffer (`jni/native/RingBuffer.h`)
Implemented as a template-based circular buffer to support any data type, specifically optimized for `AVFrame*`.
- **Thread Safety**: Utilizes `std::mutex` for mutual exclusion and `std::condition_variable` (`cond_empty_`, `cond_full_`) to implement the producer-consumer pattern.
- **Memory Safety**: Added a cleanup callback (`std::function<void(T)>`) to the constructor. When the buffer is cleared or destroyed, the callback is invoked for each remaining element, ensuring `av_frame_free` is called for `AVFrame*` pointers.
- **Blocking Behavior**: 
    - `push()` blocks when the buffer is full, preventing the decoder from overrunning the renderer.
    - `pop()` blocks when the buffer is empty, ensuring the renderer waits for decoded frames.
- **Complexity**: All operations (`push`, `pop`, `size`, `isEmpty`) operate in $O(1)$ time.

### 2. DecoderCore (`jni/native/DecoderCore.cpp/h`)
Implemented a wrapper around FFmpeg's `AVCodec` API to encapsulate the decoding lifecycle.
- **Initialization**: Handles `avcodec_find_decoder` and `avcodec_alloc_context3` to set up the codec context based on the provided `AVCodecID`.
- **Decoding Loop**: Implements the standard FFmpeg decoding flow with proper draining:
    - `avcodec_send_packet()`: Feeds raw compressed data into the decoder.
    - `avcodec_receive_frame()`: Wrapped in a `while` loop that continues until `AVERROR(EAGAIN)` or `AVERROR_EOF` is returned. This ensures that B-frames and all available frames are captured from a single packet.
- **Error Handling**: Correctly handles `AVERROR(EAGAIN)` (indicating more packets are needed) and `AVERROR_EOF` (indicating the end of the stream).
- **Memory Management**: The `destroy()` method ensures `avcodec_free_context` is called, preventing leaks of the codec context.

### 3. TDD and Verification (`tests/native/DecoderTest.cpp`)
Developed a comprehensive test suite to verify the native components.
- **RingBuffer Test**: Validates circularity, capacity limits, and basic push/pop logic.
- **DecoderCore Test**: Simulates the decoding process by feeding a mock packet and verifying that a valid `AVFrame` is produced.
- **Memory Leak Verification**: Included logic to ensure that for every allocated `AVFrame` and `AVPacket`, the corresponding free/unref calls are executed.

## Deliverables
- `jni/native/RingBuffer.h` & `jni/native/RingBuffer.cpp`
- `jni/native/DecoderCore.h` & `jni/native/DecoderCore.cpp`
- `tests/native/DecoderTest.cpp`

## Key Findings
- Using a template for `RingBuffer` allows the system to be reused for different buffer types (e.g., audio vs. video) without duplicating logic.
- Strict adherence to the `avcodec_send_packet`/`avcodec_receive_frame` loop is critical for supporting codecs that require multiple packets before producing a single frame (e.g., H.264 B-frames).
