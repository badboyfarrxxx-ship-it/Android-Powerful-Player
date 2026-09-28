#ifndef OPENSSL_PEM_H
#define OPENSSL_PEM_H

#include <openssl/crypto.h>
#include <stddef.h>

// Stub for PEM related functions to satisfy the compiler
// In a real build, these would be provided by the OpenSSL library
typedef struct bio_s BIO;

BIO* BIO_new(const char* type);
void BIO_free(BIO* b);
int PEM_read_bio_PrivateKey(BIO* bp, void** key, const char* password, void* cb);
int PEM_write_bio_PrivateKey(BIO* bp, void* key, const char* password, void* cb);

#endif // OPENSSL_PEM_H
