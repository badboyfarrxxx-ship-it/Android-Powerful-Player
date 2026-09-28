#include "WireGuardCore.h"
#include <openssl/evp.h>
#include <openssl/crypto.h>
#include <string.h>

/*
 * Security Constants
 * WireGuard uses ChaCha20-Poly1305 (RFC 8439)
 */
#define KEY_SIZE 32
#define NONCE_SIZE 12
#define TAG_SIZE 16

/**
 * Constant-time memory comparison to prevent timing attacks.
 * OpenSSL's CRYPTO_memcmp is designed for this purpose.
 */
static int secure_memcmp(const unsigned char *a, const unsigned char *b, size_t len) {
    return CRYPTO_memcmp(a, b, len);
}

/**
 * Helper to safely extract a direct buffer address and validate its capacity.
 */
static unsigned char* get_safe_buffer(JNIEnv *env, jobject buffer, jint pos, jint required_len, jint *out_actual_len) {
    if (buffer == nullptr) return nullptr;

    unsigned char* addr = (unsigned char*)env->GetDirectBufferAddress(buffer);
    jlong capacity = env->GetDirectBufferCapacity(buffer);

    if (addr == nullptr) return nullptr;

    // Check for buffer overflow: pos + required_len must be within capacity
    if ((jlong)pos + required_len > capacity) {
        return nullptr;
    }

    if (out_actual_len) {
        *out_actual_len = (jint)capacity;
    }

    return addr + pos;
}

JNIEXPORT jint JNICALL Java_com_vpn_NativeEncryptionCore_encryptPacket(
    JNIEnv *env, jobject obj,
    jobject plaintext_buf, jint plaintext_pos, jint plaintext_len,
    jobject key_buf, jobject nonce_buf,
    jobject ciphertext_buf, jint ciphertext_pos) {

    unsigned char *plaintext = get_safe_buffer(env, plaintext_buf, plaintext_pos, plaintext_len, nullptr);
    unsigned char *key = get_safe_buffer(env, key_buf, 0, KEY_SIZE, nullptr);
    unsigned char *nonce = get_safe_buffer(env, nonce_buf, 0, NONCE_SIZE, nullptr);
    unsigned char *ciphertext = get_safe_buffer(env, ciphertext_buf, ciphertext_pos, plaintext_len + TAG_SIZE, nullptr);

    if (!plaintext || !key || !nonce || !ciphertext) {
        return -1; // Buffer error
    }

    EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
    if (!ctx) return -2;

    int len;
    int ciphertext_len;
    unsigned char tag[TAG_SIZE];

    // Initialize ChaCha20-Poly1305
    if (1 != EVP_EncryptInit_ex(ctx, EVP_chacha20_poly1305(), nullptr, key, nonce)) {
        EVP_CIPHER_CTX_free(ctx);
        return -3;
    }

    // Encrypt plaintext
    if (1 != EVP_EncryptUpdate(ctx, ciphertext, &len, plaintext, plaintext_len)) {
        EVP_CIPHER_CTX_free(ctx);
        return -4;
    }
    ciphertext_len = len;

    // Finalize encryption
    if (1 != EVP_EncryptFinal_ex(ctx, ciphertext + len, &len)) {
        EVP_CIPHER_CTX_free(ctx);
        return -5;
    }
    ciphertext_len += len;

    // Get the authentication tag
    if (1 != EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_AEAD_GET_TAG, TAG_SIZE, tag)) {
        EVP_CIPHER_CTX_free(ctx);
        return -6;
    }

    // Append tag to the end of ciphertext (WireGuard layout)
    memcpy(ciphertext + ciphertext_len, tag, TAG_SIZE);

    EVP_CIPHER_CTX_free(ctx);
    return 0;
}

JNIEXPORT jint JNICALL Java_com_vpn_NativeEncryptionCore_decryptPacket(
    JNIEnv *env, jobject obj,
    jobject ciphertext_buf, jint ciphertext_pos, jint ciphertext_len,
    jobject key_buf, jobject nonce_buf,
    jobject plaintext_buf, jint plaintext_pos) {

    if (ciphertext_len < TAG_SIZE) return -1;

    unsigned char *ciphertext_full = get_safe_buffer(env, ciphertext_buf, ciphertext_pos, ciphertext_len, nullptr);
    unsigned char *key = get_safe_buffer(env, key_buf, 0, KEY_SIZE, nullptr);
    unsigned char *nonce = get_safe_buffer(env, nonce_buf, 0, NONCE_SIZE, nullptr);

    int plaintext_len = ciphertext_len - TAG_SIZE;
    unsigned char *plaintext = get_safe_buffer(env, plaintext_buf, plaintext_pos, plaintext_len, nullptr);

    if (!ciphertext_full || !key || !nonce || !plaintext) {
        return -1; // Buffer error
    }

    // Split ciphertext and tag
    unsigned char *ciphertext_data = ciphertext_full;
    unsigned char *tag = ciphertext_full + plaintext_len;

    EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
    if (!ctx) return -2;

    int len;
    int plaintext_out_len;

    // Initialize ChaCha20-Poly1305
    if (1 != EVP_DecryptInit_ex(ctx, EVP_chacha20_poly1305(), nullptr, key, nonce)) {
        EVP_CIPHER_CTX_free(ctx);
        return -3;
    }

    // Provide the expected tag for verification
    if (1 != EVP_CIPHER_CTX_ctrl(ctx, EVP_CTRL_AEAD_SET_TAG, TAG_SIZE, tag)) {
        EVP_CIPHER_CTX_free(ctx);
        return -4;
    }

    // Decrypt ciphertext
    if (1 != EVP_DecryptUpdate(ctx, plaintext, &len, ciphertext_data, plaintext_len)) {
        EVP_CIPHER_CTX_free(ctx);
        return -5;
    }
    plaintext_out_len = len;

    // Finalize decryption - This is where the tag is verified in constant-time by OpenSSL
    if (EVP_DecryptFinal_ex(ctx, plaintext + len, &len) <= 0) {
        // Verification failed
        EVP_CIPHER_CTX_free(ctx);
        // Zero out plaintext buffer to prevent leaking partial decryptions
        memset(plaintext, 0, plaintext_out_len);
        return -6; // Auth failure
    }
    plaintext_out_len += len;

    EVP_CIPHER_CTX_free(ctx);
    return 0;
}
