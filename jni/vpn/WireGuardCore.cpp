#include "WireGuardCore.h"
#include <openssl/evp.h>
#include <openssl/crypto.h>
#include <openssl/pem.h>
#include <string.h>
#include <stdint.h>

#define KEY_SIZE 32
#define NONCE_SIZE 12
#define TAG_SIZE 16

void generate_x25519_keypair(uint8_t *pub_key, uint8_t *priv_key) {
    EVP_PKEY *pkey = nullptr;
    EVP_PKEY_CTX *pctx = EVP_PKEY_CTX_new_id(EVP_PKEY_X25519, nullptr);
    if (!pctx) return;

    if (EVP_PKEY_keygen_init(pctx) <= 0) {
        EVP_PKEY_CTX_free(pctx);
        return;
    }

    if (EVP_PKEY_keygen(pctx, &pkey) <= 0) {
        EVP_PKEY_CTX_free(pctx);
        return;
    }

    size_t len = KEY_SIZE;
    EVP_PKEY_get_raw_public_key(pkey, pub_key, &len);
    EVP_PKEY_get_raw_private_key(pkey, priv_key, &len);

    EVP_PKEY_free(pkey);
    EVP_PKEY_CTX_free(pctx);
}

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

/**
 * Generates a new X25519 key pair for session rotation.
 * returns 0 on success, negative on failure.
 */
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

JNIEXPORT jint JNICALL Java_com_vpn_NativeEncryptionCore_generateSessionKeyPair(
    JNIEnv *env, jobject obj, jobject pub_key_buf, jobject priv_key_buf) {

    unsigned char *pub_key = get_safe_buffer(env, pub_key_buf, 0, KEY_SIZE, nullptr);
    unsigned char *priv_key = get_safe_buffer(env, priv_key_buf, 0, KEY_SIZE, nullptr);

    if (!pub_key || !priv_key) return -1;

    EVP_PKEY *pkey = nullptr;
    EVP_PKEY_CTX *pctx = EVP_PKEY_CTX_new_id(EVP_PKEY_X25519, nullptr);
    if (!pctx) return -2;

    if (EVP_PKEY_keygen_init(pctx) <= 0) {
        EVP_PKEY_CTX_free(pctx);
        return -3;
    }

    if (EVP_PKEY_keygen(pctx, &pkey) <= 0) {
        EVP_PKEY_CTX_free(pctx);
        return -4;
    }

    size_t len = KEY_SIZE;
    if (EVP_PKEY_get_raw_public_key(pkey, pub_key, &len) <= 0) {
        EVP_PKEY_free(pkey);
        EVP_PKEY_CTX_free(pctx);
        return -5;
    }

    len = KEY_SIZE;
    if (EVP_PKEY_get_raw_private_key(pkey, priv_key, &len) <= 0) {
        EVP_PKEY_free(pkey);
        EVP_PKEY_CTX_free(pctx);
        return -6;
    }

    EVP_PKEY_free(pkey);
    EVP_PKEY_CTX_free(pctx);
    return 0;
}

/**
 * XOR Masking Layer for DPI Stealth
 * Implements a lightweight stream cipher based on a shared seed and counter.
 */
typedef struct {
    uint64_t seed;
    uint64_t counter;
} MaskState;

static uint8_t generate_mask_byte(MaskState *state) {
    // SplitMix64-based PRNG for constant-time byte generation
    uint64_t z = (state->seed + state->counter);
    z = (z ^ (z >> 30)) * 0xbf58476d1ce4e5b9LLU;
    z = (z ^ (z >> 27)) * 0x94d049bb133111ebLLU;
    z = z ^ (z >> 31);
    state->counter++;
    return (uint8_t)(z & 0xFF);
}

static void apply_xor_mask(uint8_t *data, size_t len, MaskState *state) {
    for (size_t i = 0; i < len; i++) {
        data[i] ^= generate_mask_byte(state);
    }
}

/**
 * Constant-time memory comparison to prevent timing attacks.
 */
static int secure_memcmp(const unsigned char *a, const unsigned char *b, size_t len) {
    return CRYPTO_memcmp(a, b, len);
}

/**
 * Helper to safely extract a direct buffer address and validate its capacity.
 */
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
    if ((int)EVP_DecryptInit_ex(ctx, EVP_chacha20_poly1305(), nullptr, key, nonce) != 1) {
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
