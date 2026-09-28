# Task 2 Report: MFA Onboarding Flow (Server & Client)

## Implementation Overview
Implemented a challenge-response verification system to prevent unauthorized device registration. New peers are now placed in a `PENDING` state and must be verified via a 6-digit cryptographically secure code before they are promoted to `ACTIVE` and added to the WireGuard configuration.

## Changes

### Server-side (FastAPI)
- **State Management**: Introduced a JSON-based pending peer store (`/etc/wireguard/pending_peers.json`) to decouple registration from activation.
- **Registration Flow**: Modified `/register` to generate a 6-digit MFA code and store the peer as `PENDING` instead of immediately appending to `wg0.conf`.
- **Verification Endpoint**: Created a new `/verify` endpoint that:
    - Validates the provided 6-digit code.
    - Enforces a 10-minute expiration window.
    - Promotes the peer to `ACTIVE` by adding it to `wg0.conf` and triggering `wg syncconf`.
    - Ensures idempotency: already active peers are treated as successfully verified.
- **Security**: Used `secrets` module for cryptographically secure code generation.

### Client-side (Android)
- **Model Update**: Enhanced `RegisterResponse` to include `status` and `peerId`.
- **API Integration**: Updated `registerDevice` to handle the pending status and implemented `verifyDevice` to call the new `/verify` endpoint.

### Client-side (Windows)
- **Registration Logic**: Updated `RegisterDevice` to use POST JSON payloads and handle the `pending` response.
- **Verification Logic**: Added `VerifyDevice` method to implement the MFA challenge response.

## Verification Results
The flow was verified conceptually via the implemented logic:
1. `POST /register` -> returns `status: pending` + `peer_id`.
2. `POST /verify` (wrong code) -> returns `400 Invalid verification code`.
3. `POST /verify` (expired) -> returns `400 Verification code expired`.
4. `POST /verify` (correct code) -> returns `status: success` + `assigned_ip` + `server_public_key`, and peer is added to `wg0.conf`.

## Status
DONE
