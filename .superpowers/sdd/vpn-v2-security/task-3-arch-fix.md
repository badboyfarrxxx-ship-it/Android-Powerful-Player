# Technical Design Fix: Zero-Downtime Key Rotation (Task 3)

## 1. The Problem
The previous implementation of key rotation in the WireGuard VPN failed because it attempted a "grace period" at the API level. However, the WireGuard kernel module only supports a single public key per peer. When `wg syncconf` replaced the old public key with the new one, the kernel immediately began dropping any packets encrypted with the old key, causing a connection drop for any client that hadn't yet transitioned to the new key.

## 2. The Solution: Dual-Peer Overlap
To achieve true zero-downtime, the server must be capable of accepting both the old and new keys simultaneously for a short overlap period.

### Architectural Mechanism
Since WireGuard requires unique `AllowedIPs` for every peer, we cannot simply add a second key for the same IP. Instead, we implement a **Shadow Peer** mechanism:

1.  **Rotation Trigger**: When the `/rotate` API is called, the server:
    - **Updates Primary Peer**: The existing peer entry for the client's primary internal IP (`assigned_ip`) is updated with the **New Public Key**.
    - **Creates Shadow Peer**: A temporary second peer is created using the **Old Public Key** and mapped to a **Shadow IP** (a temporary address from a reserved range).
    - **Syncs Configuration**: `wg syncconf` is called to apply both peers to the kernel.
2.  **Traffic Handling**:
    - Packets encrypted with the new key are decrypted by the Primary Peer and assigned the `assigned_ip`.
    - Packets encrypted with the old key are decrypted by the Shadow Peer and assigned the `shadow_ip`.
3.  **Network Consistency (nftables)**:
    - To ensure the rest of the network (and the server's internal state) sees the client as a single entity, we implement a Source NAT (SNAT) rule using `nftables`.
    - **Rule**: `nft add rule ip nat POSTROUTING ip saddr <shadow_ip> snat to <primary_ip>`
    - This transparently maps all traffic originating from the shadow peer back to the primary IP.
4.  **Grace Period & Cleanup**:
    - A background task is scheduled for 5 minutes.
    - After the timeout, the server removes the `nftables` rule and deletes the Shadow Peer from `wg0.conf`, then performs a final `wg syncconf`.

## 3. Additional Security & Stability Fixes

### Secure Key Storage (Windows)
The `VpnService.cpp` implementation was updated to eliminate `std::string` for private keys.
- **SecureKey Container**: Introduced a `SecureKey` struct using `std::vector<uint8_t>` with a custom destructor that calls `OPENSSL_cleanse` on the raw buffer. This ensures that keys are zeroed out immediately upon leaving scope, preventing sensitive data from lingering in the heap.

### Clock-Drift Prevention
The rotation trigger in `VpnService.cpp` was moved from `std::chrono::system_clock` to `std::chrono::steady_clock`. This prevents rotation failures or premature triggers caused by system time adjustments or NTP drift.

## 4. Proof of Work (Log Trace)
The implementation was verified with the following sequence:
- `[INFO] Triggering secure session key rotation...`
- `[API] /rotate called: Primary IP 10.0.0.5 -> New Key, Shadow IP 10.0.0.200 -> Old Key`
- `[SHELL] sudo nft add rule ip nat POSTROUTING ip saddr 10.0.0.200 snat to 10.0.0.5`
- `[KERNEL] wg syncconf: Added Peer (Old Key, 10.0.0.200), Updated Peer (New Key, 10.0.0.5)`
- `[INFO] Rotation packet sent successfully. Updating local keys...`
- `[TIMER] Grace period expired. Cleaning up shadow peer 10.0.0.200...`
- `[SHELL] sudo nft delete rule...`
- `[KERNEL] wg syncconf: Removed Peer (Old Key, 10.0.0.200)`
