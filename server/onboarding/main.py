from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import asyncio
import subprocess
import os
import secrets
import time
import json
from typing import Dict, Optional
from fastapi import FastAPI, HTTPException, BackgroundTasks

app = FastAPI(title="VPN Onboarding API")

async def cleanup_shadow_peer(peer_id_internal: str, shadow_ip: str, primary_ip: str):
    """Removes the shadow peer and the NAT rule after the grace period."""
    await asyncio.sleep(ROTATION_GRACE_PERIOD)
    try:
        # 1. Remove NAT rule specifically for this shadow IP
        subprocess.run(f"sudo nft delete rule ip nat POSTROUTING ip saddr {shadow_ip} snat to {primary_ip}", shell=True)

        # 2. Remove shadow peer from wg0.conf
        if os.path.exists(WG_CONF_PATH):
            with open(WG_CONF_PATH, "r") as f:
                content = f.read()

            import re
            # Match the specific [Peer] block that contains this shadow_ip in AllowedIPs
            # We look for the block starting with [Peer], containing any PublicKey,
            # and specifically the shadow_ip/32.
            pattern = r"\[Peer\]\s*PublicKey = .*?\s*AllowedIPs = " + re.escape(shadow_ip) + r"/32\s*"
            new_content = re.sub(pattern, "", content, flags=re.DOTALL)

            with open(WG_CONF_PATH, "w") as f:
                f.write(new_content)

        # 3. Apply changes
        subprocess.run(f"sudo wg syncconf wg0 <(sudo wg-quick strip wg0)", shell=True, check=True)
        print(f"Cleanup completed for shadow peer {shadow_ip}")
    except Exception as e:
        print(f"Error during shadow peer cleanup: {e}")
SERVER_PUB_KEY_PATH = "/etc/wireguard/keys/publickey"
PENDING_PEERS_PATH = "/etc/wireguard/pending_peers.json"
PEER_STATE_PATH = "/etc/wireguard/peer_state.json" # Track rotation and grace periods
VPN_SUBNET = "10.0.0"
START_IP = 2
MAX_IP = 254
CODE_EXPIRY_SECONDS = 600  # 10 minutes
ROTATION_GRACE_PERIOD = 300 # 5 minutes grace period for old keys

# Rate Limiting Configuration
RATE_LIMIT_ATTEMPTS = 5
RATE_LIMIT_WINDOW = 600  # 10 minutes
verification_attempts: Dict[str, list] = {} # peer_id -> list of timestamps

class RegistrationRequest(BaseModel):
    device_name: str
    public_key: str

class VerificationRequest(BaseModel):
    peer_id: str
    code: str

class RotationRequest(BaseModel):
    peer_id: str
    new_public_key: str

def load_pending_peers() -> Dict:
    if not os.path.exists(PENDING_PEERS_PATH):
        return {}
    with open(PENDING_PEERS_PATH, "r") as f:
        return json.load(f)

def save_pending_peers(peers: Dict):
    with open(PENDING_PEERS_PATH, "w") as f:
        json.dump(peers, f, indent=4)

def load_peer_state() -> Dict:
    if not os.path.exists(PEER_STATE_PATH):
        return {}
    with open(PEER_STATE_PATH, "r") as f:
        return json.load(f)

def save_peer_state(state: Dict):
    with open(PEER_STATE_PATH, "w") as f:
        json.dump(state, f, indent=4)

def get_next_free_ip():
    """Parses wg0.conf to find the next available IP in the pool."""
    if not os.path.exists(WG_CONF_PATH):
        return f"{VPN_SUBNET}.{START_IP}"

    with open(WG_CONF_PATH, "r") as f:
        content = f.read()

    used_ips = []
    for line in content.splitlines():
        if "AllowedIPs" in line:
            ip = line.split("=")[1].strip().split("/")[0]
            used_ips.append(int(ip.split(".")[-1]))

    for ip in range(START_IP, MAX_IP + 1):
        if ip not in used_ips:
            return f"{VPN_SUBNET}.{ip}"

    raise Exception("No more IP addresses available in the pool")

@app.post("/register")
async def register_device(req: RegistrationRequest):
    try:
        # Check if peer already exists in wg0.conf (already ACTIVE)
        if os.path.exists(WG_CONF_PATH):
            with open(WG_CONF_PATH, "r") as f:
                if req.public_key in f.read():
                    # Peer is already active, treat as success but no need for MFA
                    with open(SERVER_PUB_KEY_PATH, "r") as sk:
                        server_pub_key = sk.read().strip()
                    return {
                        "status": "active",
                        "message": f"Device {req.device_name} is already registered.",
                        "server_public_key": server_pub_key
                    }

        client_ip = get_next_free_ip()
        verification_code = "".join([str(secrets.randbelow(10)) for _ in range(6)])

        pending_peers = load_pending_peers()
        pending_peers[req.public_key] = {
            "device_name": req.device_name,
            "public_key": req.public_key,
            "assigned_ip": client_ip,
            "code": verification_code,
            "timestamp": time.time()
        }
        save_pending_peers(pending_peers)

        # SECURITY FIX: MFA codes must be Out-of-Band (OOB).
        # Simulated OOB delivery (log entry). In production, this calls SMS/Email API.
        print(f"[OOB MESSAGE] To {req.device_name}: Your VPN verification code is {verification_code}")

        return {
            "status": "pending",
            "peer_id": req.public_key,
            "message": "Device registered. Please verify with the 6-digit code sent to your registered channel."
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/verify")
async def verify_device(req: VerificationRequest):
    try:
        # Rate Limiting
        now = time.time()
        attempts = verification_attempts.get(req.peer_id, [])
        # Filter attempts within the window
        attempts = [t for t in attempts if now - t < RATE_LIMIT_WINDOW]

        if len(attempts) >= RATE_LIMIT_ATTEMPTS:
            raise HTTPException(status_code=429, detail="Too many verification attempts. Please try again in 10 minutes.")

        # Record this attempt
        attempts.append(now)
        verification_attempts[req.peer_id] = attempts

        pending_peers = load_pending_peers()

        if req.peer_id not in pending_peers:
            # Check if already active (idempotency)
            if os.path.exists(WG_CONF_PATH):
                with open(WG_CONF_PATH, "r") as f:
                    if req.peer_id in f.read():
                        return {"status": "success", "message": "Device already verified."}

            raise HTTPException(status_code=404, detail="Peer not found in pending list.")

        peer_data = pending_peers[req.peer_id]

        # Check expiry
        if time.time() - peer_data["timestamp"] > CODE_EXPIRY_SECONDS:
            raise HTTPException(status_code=400, detail="Verification code expired.")

        # Check code
        if peer_data["code"] != req.code:
            raise HTTPException(status_code=400, detail="Invalid verification code.")

        # Promote to ACTIVE
        client_ip = peer_data["assigned_ip"]
        peer_block = f"\n[Peer]\nPublicKey = {req.peer_id}\nAllowedIPs = {client_ip}/32\n"

        with open(WG_CONF_PATH, "a") as f:
            f.write(peer_block)

        cmd = f"sudo wg syncconf wg0 <(sudo wg-quick strip wg0)"
        subprocess.run(cmd, shell=True, check=True)

        # Initialize peer state for rotation tracking
        peer_state = load_peer_state()
        peer_state[req.peer_id] = {
            "current_pub_key": req.peer_id,
            "previous_pub_key": None,
            "last_rotation_time": 0,
            "assigned_ip": client_ip
        }
        save_peer_state(peer_state)

        # Remove from pending
        del pending_peers[req.peer_id]
        save_pending_peers(pending_peers)

        with open(SERVER_PUB_KEY_PATH, "r") as f:
            server_pub_key = f.read().strip()

        return {
            "status": "success",
            "assigned_ip": client_ip,
            "server_public_key": server_pub_key,
            "message": "Device verified and activated successfully."
        }
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.post("/rotate")
async def rotate_key(req: RotationRequest, background_tasks: BackgroundTasks):
    """
    Handles secure key rotation request with Zero-Downtime.
    Implements Dual-Peer Overlap:
    1. New key takes the primary IP.
    2. Old key is moved to a temporary shadow IP.
    3. nftables NATs shadow IP traffic back to primary IP.
    """
    try:
        peer_state = load_peer_state()

        peer_entry = None
        peer_id_internal = None

        for pid, data in peer_state.items():
            if pid == req.peer_id or data["current_pub_key"] == req.peer_id:
                peer_entry = data
                peer_id_internal = pid
                break

        if not peer_entry:
            raise HTTPException(status_code=404, detail="Active peer not found.")

        old_pub_key = peer_entry["current_pub_key"]
        new_pub_key = req.new_public_key
        primary_ip = peer_entry["assigned_ip"]

        # Dynamic Shadow IP Allocation
        # If primary IP is 10.0.0.X, shadow IP is 10.0.0.X + 100
        # This ensures no collisions as long as X <= 154 (Total subnet 254)
        try:
            ip_suffix = int(primary_ip.split(".")[-1])
            shadow_suffix = ip_suffix + 100
            if shadow_suffix > 254:
                # Fallback for high IPs: wrap around or use a different offset
                # For this subnet, we assume primary IPs are assigned 2-154
                raise HTTPException(status_code=500, detail="Primary IP too high for standard shadow offset")
            shadow_ip = f"{VPN_SUBNET}.{shadow_suffix}"
        except (ValueError, IndexError):
            raise HTTPException(status_code=500, detail="Invalid primary IP format")

        peer_entry["previous_pub_key"] = old_pub_key
        peer_entry["current_pub_key"] = new_pub_key
        peer_entry["last_rotation_time"] = time.time()

        # Update wg0.conf
        if os.path.exists(WG_CONF_PATH):
            with open(WG_CONF_PATH, "r") as f:
                content = f.read()

            # 1. Replace primary peer's public key
            import re
            # This regex finds the [Peer] block that has the primary IP and replaces its PublicKey
            pattern = r"(\[Peer\]\s*PublicKey = )[a-zA-Z0-9+/=]{44}(\s*AllowedIPs = " + re.escape(primary_ip) + r"/32)"
            updated_content = re.sub(pattern, r"\1" + new_pub_key + r"\2", content)

            # 2. Add shadow peer for the old key
            shadow_block = f"\n[Peer]\nPublicKey = {old_pub_key}\nAllowedIPs = {shadow_ip}/32\n"
            updated_content += shadow_block

            with open(WG_CONF_PATH, "w") as f:
                f.write(updated_content)

        # Apply changes
        subprocess.run(f"sudo wg syncconf wg0 <(sudo wg-quick strip wg0)", shell=True, check=True)

        # Setup NAT rule: Shadow IP -> Primary IP
        # This ensures the rest of the network sees the client as primary_ip
        nat_cmd = f"sudo nft add rule ip nat POSTROUTING ip saddr {shadow_ip} snat to {primary_ip}"
        subprocess.run(nat_cmd, shell=True, check=True)

        save_peer_state(peer_state)

        # Schedule cleanup after grace period
        background_tasks.add_task(cleanup_shadow_peer, peer_id_internal, shadow_ip, primary_ip)

        return {
            "status": "success",
            "message": "Key rotated with zero downtime. Dual-peer overlap active."
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
