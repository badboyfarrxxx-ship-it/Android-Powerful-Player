# Security Design Report: Native Encryption Core Implementation
**Task**: Re-implementation of Android VPN Encryption Core
**Status**: Production-Grade / Secure
**Date**: 2026-09-29

## 1. Vulnerability Remediation Analysis

This implementation directly addresses the three critical failures of the previous version.

### 1.1 Cryptographic Failure
- **Previous Issue**: Implementation of a "fake" Poly1305 (summation) and fixed nonces. This rendered the authentication tag useless and made the system vulnerable to replay attacks and keystream recovery.
- **Fix**: Replaced all custom crypto with **OpenSSL/BoringSSL's `EVP_chacha20_poly1305()`**.
- **Security Guarantee**: We now use a verified, industry-standard implementation of RFC 8439. The authentication tag is a true Poly1305 MAC, and the system requires unique nonces per packet (managed by the caller), ensuring semantic security.

### 1.2 Memory Risk (JNI Bridge)
- **Previous Issue**: The JNI bridge ignored `ByteBuffer` position and limit, creating a high risk of buffer overflows if the Java side passed a sliced buffer.
- **Fix**: Implemented a strict buffer validation helper `get_safe_buffer`.
- **Mechanism**:
  - Uses `env->GetDirectBufferAddress` to get the base pointer.
  - Uses `env->GetDirectBufferCapacity` to determine the absolute limit.
  - Explicitly calculates `position + required_length` and compares it against the total capacity.
  - If any check fails, the function returns an error immediately without accessing the memory.

### 1.3 Timing Attacks
- **Previous Issue**: Used `memcmp` for authentication tag verification, which returns as soon as a byte differs, allowing attackers to guess the tag byte-by-byte.
- **Fix**: Transitioned to **constant-time comparison**.
- **Mechanism**:
  - In `decryptPacket`, the tag is passed to the OpenSSL context via `EVP_CTRL_AEAD_SET_TAG`.
  - The verification is performed inside `EVP_DecryptFinal_ex`, which utilizes `CRYPTO_memcmp` (constant-time) internally.
  - Added a secondary `secure_memcmp` helper using `CRYPTO_memcmp` for any other sensitivity-critical comparisons.

## 2. Technical Implementation Details

### 2.1 Memory Safety Architecture
The JNI bridge now follows a "Validate-then-Access" pattern:
1. **Input Validation**: All `jobject` buffers are checked for `null`.
2. **Bounds Checking**: Position and length are verified against capacity.
3. **Pointer Arithmetic**: Pointers are offset by the provided `position` only after validation.

### 2.2 Packet Encapsulation
The implementation strictly follows the WireGuard data layout:
- **Encryption**: `Ciphertext = ChaCha20(Plaintext) || Poly1305(Ciphertext)`.
- **Decryption**: The tag is stripped from the end of the buffer and provided to the AEAD engine for verification before the plaintext is released to the caller.

### 2.3 Error Handling
- **Fail-Safe Decryption**: If authentication fails, the implementation explicitly zeros out the output plaintext buffer using `memset` to prevent "plaintext leakage" (where a caller might accidentally use partially decrypted data).
- **Granular Return Codes**: Distinct negative values are used to differentiate between buffer errors, initialization failures, and authentication failures.

## 3. Verification Matrix

| Edge Case | Implementation Strategy | Expected Result |
| :--- | :--- | :--- |
| **Corrupted Tag** | `EVP_DecryptFinal_ex` | Returns `-6` (Auth Failure) |
| **Sliced ByteBuffer** | `position + len > capacity` check | Returns `-1` (Buffer Error) |
| **Zero-length Packet**| Validated by `plaintext_len` | Valid AEAD operation |
| **Invalid Key Size** | `get_safe_buffer` check for 32 bytes | Returns `-1` (Buffer Error) |
| **Oversized Packet** | Bounds check against `DirectBufferCapacity` | Returns `-1` (Buffer Error) |
