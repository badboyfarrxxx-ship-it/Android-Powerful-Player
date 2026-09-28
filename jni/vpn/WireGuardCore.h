#ifndef WIREGUARD_CORE_H
#define WIREGUARD_CORE_H

#include <jni.h>
#include <stdint.h>
#include <stddef.h>

#ifdef __cplusplus
extern "C" {
#endif

// --- Key Generation ---
void generate_x25519_keypair(uint8_t *pub_key, uint8_t *priv_key);

// --- Stealth Masking ---
void apply_stealth_mask(uint8_t *data, size_t len, uint64_t seed, uint64_t counter);

// --- JNI Wrapper for Java/Android ---
JNIEXPORT jint JNICALL Java_com_vpn_NativeEncryptionCore_encryptPacket(
 *
 * @param env JNI environment.
 * @param clazz The calling class.
 * @param plaintext ByteBuffer containing the plaintext.
 * @param plaintext_pos Current position of the plaintext buffer.
 * @param plaintext_len Length of data to encrypt.
 * @param key ByteBuffer containing the 32-byte symmetric key.
 * @param nonce ByteBuffer containing the 12-byte nonce.
 * @param ciphertext ByteBuffer to store the resulting ciphertext and 16-byte tag.
 * @param ciphertext_pos Current position of the ciphertext buffer.
 * @return 0 on success, non-zero on failure.
 */
JNIEXPORT jint JNICALL Java_com_vpn_NativeEncryptionCore_encryptPacket(
    JNIEnv *env, jobject obj,
    jobject plaintext, jint plaintext_pos, jint plaintext_len,
    jobject key, jobject nonce,
    jobject ciphertext, jint ciphertext_pos);

/**
 * Decrypts a packet using ChaCha20-Poly1305.
 *
 * @param env JNI environment.
 * @param clazz The calling class.
 * @param ciphertext ByteBuffer containing the ciphertext and tag.
 * @param ciphertext_pos Current position of the ciphertext buffer.
 * @param ciphertext_len Total length of ciphertext including the 16-byte tag.
 * @param key ByteBuffer containing the 32-byte symmetric key.
 * @param nonce ByteBuffer containing the 12-byte nonce.
 * @param plaintext ByteBuffer to store the resulting plaintext.
 * @param plaintext_pos Current position of the plaintext buffer.
 * @return 0 on success, non-zero on failure (e.g., authentication failure).
 */
JNIEXPORT jint JNICALL Java_com_vpn_NativeEncryptionCore_decryptPacket(
    JNIEnv *env, jobject obj,
    jobject ciphertext, jint ciphertext_pos, jint ciphertext_len,
    jobject key, jobject nonce,
    jobject plaintext, jint plaintext_pos);

#ifdef __cplusplus
}
#endif

#endif // WIREGUARD_CORE_H
