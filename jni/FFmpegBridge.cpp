#include <jni.h>
#include <string>
#include <android/log.h>
#include "native/DecoderCore.h"

#define LOG_TAG "FFmpegBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// Global pointer to the active decoder for metrics polling
static DecoderCore* g_decoder = nullptr;

extern "C" {

// JNI Ping method to verify native bridge connectivity.
// Kotlin signature: native int ping()
JNIEXPORT jint JNICALL
Java_com_example_powerfulplayer_FFmpegBridge_ping(JNIEnv* env, jobject thiz) {
    LOGI("Native ping() called. Returning 1.");
    return 1;
}

// Signatures for Task 5 implementation
// Kotlin signature: native int decodePacket(long formatContextPtr, long packetPtr)
JNIEXPORT jint JNICALL
Java_com_example_powerfulplayer_FFmpegBridge_decodePacket(JNIEnv* env, jobject thiz, jlong formatContextPtr, jlong packetPtr) {
    LOGI("decodePacket called (stubs for Task 5)");
    return 0;
}

// Kotlin signature: native void releaseBuffer(long bufferPtr)
JNIEXPORT void JNICALL
Java_com_example_powerfulplayer_FFmpegBridge_releaseBuffer(JNIEnv* env, jobject thiz, jlong bufferPtr) {
    LOGI("releaseBuffer called (stubs for Task 5)");
}

// Metrics polling methods for Engine Room
JNIEXPORT jfloat JNICALL
Java_com_example_powerfulplayer_FFmpegBridge_getNativeFps(JNIEnv* env, jobject thiz) {
    if (!g_decoder) return 0.0f;
    return g_decoder->getFps();
}

JNIEXPORT jint JNICALL
Java_com_example_powerfulplayer_FFmpegBridge_getNativeBitrate(JNIEnv* env, jobject thiz) {
    if (!g_decoder) return 0;
    return g_decoder->getBitrateKbps();
}

JNIEXPORT jfloat JNICALL
Java_com_example_powerfulplayer_FFmpegBridge_getNativeDecodeSpeed(JNIEnv* env, jobject thiz) {
    if (!g_decoder) return 0.0f;
    return g_decoder->getAverageDecodeTimeMs();
}

} // extern "C"
