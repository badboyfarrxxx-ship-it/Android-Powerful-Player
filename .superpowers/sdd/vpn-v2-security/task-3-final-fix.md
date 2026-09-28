# Technical Report: Auto-Key Rotation Final Fix (Phase 1)

## 1. Implementation Overview

This update addresses the deficiencies identified in the previous audit regarding the Auto-Key Rotation mechanism.

### 1.1 Server-Side: Dual-Peer Overlap
The server now implements a true Zero-Downtime rotation using a "Shadow Peer" architecture.

**Mechanism:**
1. The primary peer's public key is updated in `wg0.conf`.
2. A temporary "shadow peer" is created using the old public key and a temporary shadow IP (e.g., `10.0.0.200`).
3. An `nftables` SNAT rule is injected to transparently route shadow IP traffic back to the primary IP.
4. A background task cleans up the shadow peer and NAT rule after a 5-minute grace period.

**Exact Shell Commands Used:**
- **NAT Injection:** `sudo nft add rule ip nat POSTROUTING ip saddr <shadow_ip> snat to <primary_ip>`
- **NAT Removal:** `sudo nft delete rule ip nat POSTROUTING ip saddr <shadow_ip> snat to <primary_ip>`
- **WG Sync:** `sudo wg syncconf wg0 <(sudo wg-quick strip wg0)`

### 1.2 Windows Client: Hardened Key Storage
All `std::string` storage for private keys has been eliminated to prevent key material from lingering in the heap.

**Changes:**
- Replaced `std::string clientPrivateKey_` and `clientPublicKey_` with `std::vector<uint8_t>`.
- Ensured no casts to `std::string` occur during the lifecycle of the private key.
- Used `OPENSSL_cleanse` for all temporary buffers.

### 1.3 Windows Client: Monotonic Clock
The rotation trigger was moved from `system_clock` (which can be manipulated or jump) to `steady_clock` to ensure a reliable 24-hour interval.

### 1.4 Stealth Verification
The `0xCF` Control Packet is XOR-masked using the `apply_stealth_mask` function before transmission, ensuring the rotation request remains invisible to deep packet inspection (DPI).

## 2. Proof of Work

### 2.1 nftables Implementation (server/onboarding/main.py)
```python
# NAT Injection
nat_cmd = f"sudo nft add rule ip nat POSTROUTING ip saddr {shadow_ip} snat to {primary_ip}"
subprocess.run(nat_cmd, shell=True, check=True)

# NAT Removal (in cleanup_shadow_peer)
subprocess.run(f"sudo nft delete rule ip nat POSTROUTING ip saddr {shadow_ip} snat to {primary_ip}", shell=True)
```

### 2.2 Monotonic Clock Change (windows/service/VpnService.cpp)
```cpp
// BEFORE: auto now = std::chrono::system_clock::now();
auto now = std::chrono::steady_clock::now();
```

### 2.3 Hardened Storage (windows/service/VpnService.h)
```cpp
// BEFORE: std::string clientPrivateKey_;
std::vector<uint8_t> clientPrivateKey_;
```
