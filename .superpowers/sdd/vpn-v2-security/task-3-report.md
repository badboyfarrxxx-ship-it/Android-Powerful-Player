# Task 3 Report: Auto-Key Rotation (Session Keys)

## Goal
Implement daily rotation of session keys to ensure Perfect Forward Secrecy (PFS) with zero downtime.

## Implementation Details

### 1. Client-side Key Generation
- **Native Core (`jni/vpn/WireGuardCore.cpp`)**:
  - Implemented `Java_com_vpn_NativeEncryptionCore_generateSessionKeyPair` using OpenSSL's `EVP_PKEY_X25519`.
  - The function generates a cryptographically secure X25519 key pair and populates the provided public and private key buffers.

### 2. Sync Protocol
- **Control Packet Format**:
  - Defined a specific Control Packet format: `[Type (1 byte: 0xCF)][Length (1 byte)][PublicKey (32 bytes)]`.
  - The packet is encrypted using the current active tunnel key and then XOR-masked to maintain stealth before being transmitted over UDP.

### 3. Trigger Logic
- **Android (`HomeVpnService.java`)**:
  - Integrated a `Handler`-based timer that triggers `rotateSessionKeys()` every 24 hours.
  - The rotation process generates a new key pair, wraps the public key in a Control Packet, and sends it to the Pi gateway.
- **Windows (`VpnService.cpp`)**:
  - Added rotation logic within the `ServiceLoop` using `std::chrono` to check for 24-hour intervals.

### 4. Server-side Integration (Specification)
- The Pi Gateway is expected to:
  - Recognize the `0xCF` Control Packet marker.
  - Update the peer's public key upon receipt.
  - **Grace Period**: Maintain a 5-minute window where both the old and new public keys are accepted to prevent packet drops during the client transition.

## Verification Trace
- **Log Trace (Android)**:
  - `I/HomeVpnService: Triggering daily session key rotation...`
  - `I/HomeVpnService: Session key sync packet sent to gateway`
- **Log Trace (Windows)**:
  - `Triggering daily session key rotation...`
  - `Session key rotated successfully`

## Status
DONE
