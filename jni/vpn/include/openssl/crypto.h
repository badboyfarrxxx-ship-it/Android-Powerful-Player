#ifndef OPENSSL_CRYPTO_H
#define OPENSSL_CRYPTO_H

#include <stddef.h>
#include <stdint.h>

// Basic OpenSSL types and macros to satisfy the compiler
typedef unsigned char BYTE;
typedef uint32_t DWORD;

#define OPENSSL_NO_ASM
#define OPENSSL_THREADS

// Stub for basic crypto operations to avoid linker errors during initial build
static inline int OPENSSL_init_crypto(int flags, const char *config) { return 1; }

#endif // OPENSSL_CRYPTO_H
