#ifndef OPENSSL_EVP_H
#define OPENSSL_EVP_H
#include <stdint.h>
#include <stddef.h>

typedef struct evp_cipher_ctx_st EVP_CIPHER_CTX;
typedef struct evp_pkey_ctx_st EVP_PKEY_CTX;
typedef struct evp_pkey_st EVP_PKEY;

#define EVP_PKEY_X25519 1
#define EVP_CTRL_AEAD_GET_TAG 0
#define EVP_CTRL_AEAD_SET_TAG 1

extern "C" {
    EVP_CIPHER_CTX* EVP_CIPHER_CTX_new();
    void EVP_CIPHER_CTX_free(EVP_CIPHER_CTX* ctx);
    int EVP_EncryptInit_ex(EVP_CIPHER_CTX* ctx, const void* cipher, const void* engine, const unsigned char* key, const unsigned char* iv);
    int EVP_EncryptUpdate(EVP_CIPHER_CTX* ctx, unsigned char* out, int* outl, const unsigned char* in, int inl);
    int EVP_EncryptFinal_ex(EVP_CIPHER_CTX* ctx, unsigned char* out, int* outl);
    int EVP_CIPHER_CTX_ctrl(EVP_CIPHER_CTX* ctx, int cmd, int arg, void* p);
    
    EVP_CIPHER_CTX* EVP_DecryptInit_ex(EVP_CIPHER_CTX* ctx, const void* cipher, const void* engine, const unsigned char* key, const unsigned char* iv);
    int EVP_DecryptUpdate(EVP_CIPHER_CTX* ctx, unsigned char* out, int* outl, const unsigned char* in, int inl);
    int EVP_DecryptFinal_ex(EVP_CIPHER_CTX* ctx, unsigned char* out, int* outl);
    
    EVP_PKEY_CTX* EVP_PKEY_CTX_new_id(int id, void* engine);
    void EVP_PKEY_CTX_free(EVP_PKEY_CTX* ctx);
    int EVP_PKEY_keygen_init(EVP_PKEY_CTX* ctx);
    int EVP_PKEY_keygen(EVP_PKEY_CTX* ctx, EVP_PKEY** pkey);
    int EVP_PKEY_get_raw_public_key(EVP_PKEY* pkey, unsigned char* pub, size_t* len);
    int EVP_PKEY_get_raw_private_key(EVP_PKEY* pkey, unsigned char* priv, size_t* len);
    void EVP_PKEY_free(EVP_PKEY* pkey);
    
    void* EVP_chacha20_poly1305();
    int CRYPTO_memcmp(const void* a, const void* b, size_t len);
}
#endif
