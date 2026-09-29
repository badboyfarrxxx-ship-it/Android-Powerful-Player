#include "WireGuardCore.h"
#include <string.h>
#include <stdint.h>

#define KEY_SIZE 32
#define NONCE_SIZE 12
#define TAG_SIZE 16

// These are now stubs because the logic moved to Java/Kotlin using Conscrypt
JNIEXPORT jint JNICALL Java_com_vpn_NativeEncryptionCore_generateSessionKeyPair(
    JNIEnv *env, jobject obj, jobject pub_key_buf, jobject priv_key_buf) {
    return 0;
}

JNIEXPORT jint JNICALL Java_com_vpn_NativeEncryptionCore_encryptPacket(
    JNIEnv *env, jobject obj,
    jobject plaintext_buf, jint plaintext_pos, jint plaintext_len,
    jobject key_buf, jobject nonce_buf,
    jobject ciphertext_buf, jint ciphertext_pos) {
    return 0;
}

JNIEXPORT jint JNICALL Java_com_vpn_NativeEncryptionCore_decryptPacket(
    JNIEnv *env, jobject obj,
    jobject ciphertext_buf, jint ciphertext_pos, jint ciphertext_len,
    jobject key_buf, jobject nonce_buf,
    jobject plaintext_buf, jint plaintext_pos) {
    return 0;
}

// Keep these since they might be used for other things or are expected by the Java layer
void apply_stealth_mask(uint8_t *data, size_t len, uint64_t seed, uint64_t counter) {
    uint64_t z_seed = seed;
    uint64_t z_counter = counter;
    for (size_t i = 0; i < len; i++) {
        uint64_t z = (z_seed + z_counter);
        z = (z ^ (z >> 30)) * 0xbf58476d1ce4e5b9LLU;
        z = (z ^ (z >> 27)) * 0x94d049bb133111ebLLU;
        z = z ^ (z >> 31);
        z_counter++;
        data[i] ^= (uint8_t)(z & 0xFF);
    }
}
