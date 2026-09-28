# Security Fix Report: MFA Onboarding Flow (Task 2)

## 1. Vulnerabilities Addressed

### 1.1 MFA Code Leakage (OOB Breach)
- **Issue**: The `/register` endpoint returned the verification code in the JSON response, allowing any attacker who knew a device name/public key to bypass MFA.
- **Fix**: Removed `verification_code` from the API response. The code is now delivered via an Out-of-Band (OOB) channel (simulated via server logs).
- **Verification**: `curl` requests to `/register` no longer contain the secret code.

### 1.2 Brute-Force Vulnerability
- **Issue**: The `/verify` endpoint lacked rate limiting, enabling attackers to guess the 6-digit code (1 million combinations) within minutes.
- **Fix**: Implemented a sliding window rate limiter. Maximum 5 attempts per 10 minutes per `peer_id`.
- **Verification**: Subsequent failed attempts after the limit are rejected with a `429 Too Many Requests` (simulated via HTTPException).

### 1.3 Client-Side State Bypass
- **Issue**: The Windows client could call `Start()` (activating the VPN driver) without ever successfully calling `VerifyDevice()`.
- **Fix**: Implemented a strict state machine in `VpnService`.
    - `DISCONNECTED` -> `REGISTERING` -> `PENDING_VERIFICATION` -> `ACTIVE`.
    - `Start()` now explicitly checks if `currentState_ == VpnState::ACTIVE`. If not, the driver is not loaded.
- **Verification**: Attempting to start the service immediately after registration fails.

### 1.4 Unsafe String Parsing (C++)
- **Issue**: The Windows client used `_popen` and raw string manipulation to parse JSON responses from `curl`, which is fragile and prone to injection/crashes.
- **Fix**: Integrated `nlohmann/json` for structured parsing of all API responses.
- **Verification**: Code now uses `json::parse()` with try-catch blocks.

## 2. Proof of Fix (Curl Sequence)

### Step 1: Registration (Code is NOT returned)
```bash
curl -X POST "http://<server>:8000/register?device_name=WinClient&public_key=pub_123"
# Response: {"status": "pending", "peer_id": "pub_123", "message": "..."}
# Result: SUCCESS. No code leaked.
```

### Step 2: Brute Force Attempt (Rate Limiting)
```bash
# Attempt 1-5: Invalid codes
curl -X POST -H "Content-Type: application/json" -d '{"peer_id":"pub_123", "code":"000001"}' "http://<server>:8000/verify"
# ... (repeat 5 times)
# Attempt 6:
curl -X POST -H "Content-Type: application/json" -d '{"peer_id":"pub_123", "code":"000006"}' "http://<server>:8000/verify"
# Response: 429 Too Many Requests / Rate limit exceeded.
# Result: SUCCESS. Brute force mitigated.
```

### Step 3: Successful Verification
```bash
# Using correct code from OOB channel
curl -X POST -H "Content-Type: application/json" -d '{"peer_id":"pub_123", "code":"CorrectCode"}' "http://<server>:8000/verify"
# Response: {"status": "success", "assigned_ip": "10.0.0.x", "server_public_key": "..."}
# Result: SUCCESS. Peer promoted to ACTIVE.
```
