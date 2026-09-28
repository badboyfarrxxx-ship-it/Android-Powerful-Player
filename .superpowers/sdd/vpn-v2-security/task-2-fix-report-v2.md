# Task 2: MFA Onboarding Flow - Security Fix Report v2

## Overview
This report documents the critical security fixes implemented to address the rejected "patch" implementation of the MFA Onboarding Flow. The focus was on activating server-side rate limiting, enforcing the client-side state machine, and replacing hard-coded responses with actual JSON parsing.

## Fixes Implemented

### 1. Server-Side Rate Limiter Activation
**File**: `server/onboarding/main.py`
**Issue**: Rate limiting constants were defined, but the logic was never invoked in the `/verify` endpoint, allowing brute-force attacks on the 6-digit MFA code.
**Implementation**:
Inserted logic at the start of the `verify_device` handler to track attempts per `peer_id`.
- **Logic**: Checks `verification_attempts` dictionary. Filters timestamps to the `RATE_LIMIT_WINDOW` (10 minutes).
- **Enforcement**: If attempts $\ge 5$, returns `429 Too Many Requests`.
- **Code Change**:
```python
        # Rate Limiting
        now = time.time()
        attempts = verification_attempts.get(req.peer_id, [])
        attempts = [t for t in attempts if now - t < RATE_LIMIT_WINDOW]
        
        if len(attempts) >= RATE_LIMIT_ATTEMPTS:
            raise HTTPException(status_code=429, detail="Too many verification attempts. Please try again in 10 minutes.")
        
        attempts.append(now)
        verification_attempts[req.peer_id] = attempts
```

### 2. Windows Client State Machine Enforcement
**File**: `windows/service/VpnService.cpp`
**Issue**: `VpnService::Start()` lacked checks for the current `VpnState`, allowing the driver to be activated regardless of whether the device was verified.
**Implementation**:
Added an explicit state check at the entry point of `Start()`.
- **Enforcement**: The service now checks `if (currentState_ != VpnState::ACTIVE)`.
- **Result**: Returns `false` and logs a security error if the state is not `ACTIVE`.
- **Code Change**:
```cpp
    // SECURITY FIX: Enforce State Machine
    if (currentState_ != VpnState::ACTIVE) {
        std::cerr << "[SECURITY ERROR] Attempted to start VPN while state is not ACTIVE (Current State: " 
                  << static_cast<int>(currentState_.load()) << ")" << std::endl;
        return false;
    }
```

### 3. Dynamic JSON Parsing in Windows Client
**File**: `windows/service/VpnService.cpp`
**Issue**: The client used `_popen` with `curl` but ignored the output, using hard-coded values for IP and Public Keys.
**Implementation**:
Replaced hard-coded values with a proper response capture loop and `nlohmann/json` parsing.
- **Logic**: Captures the full output of the `curl` command and parses it as JSON.
- **Dynamic State Update**: 
    - If status is `pending`, sets state to `PENDING_VERIFICATION`.
    - If status is `active`, sets state to `ACTIVE` and extracts `server_public_key` and `assigned_ip`.
- **Security**: Explicitly ensures that the verification code is NOT read from the response (per OOB contract).
- **Code Change**:
```cpp
    auto j = json::parse(response_text);
    if (j.contains("status") && j["status"] == "pending") {
        currentState_ = VpnState::PENDING_VERIFICATION;
        return true;
    } else if (j.contains("status") && j["status"] == "active") {
        currentState_ = VpnState::ACTIVE;
        serverPublicKey_ = j.value("server_public_key", "");
        assignedIp_ = j.value("assigned_ip", "");
    }
```

## Final Security Status
- **Rate Limiting**: ACTIVE (5 attempts / 10 mins).
- **State Enforcement**: ACTIVE (Start() blocked unless ACTIVE).
- **Dynamic Parsing**: ACTIVE (Removed hard-coded responses).
- **OOB Integrity**: MAINTAINED (Codes remain out of JSON responses).

**Status**: DONE
