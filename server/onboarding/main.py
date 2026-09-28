from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import subprocess
import os

app = FastAPI(title="VPN Onboarding API")

# Configuration
WG_CONF_PATH = "/etc/wireguard/wg0.conf"
SERVER_PUB_KEY_PATH = "/etc/wireguard/keys/publickey"
VPN_SUBNET = "10.0.0"
START_IP = 2
MAX_IP = 254

class RegistrationRequest(BaseModel):
    device_name: str
    public_key: str

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
        client_ip = get_next_free_ip()
        
        # Construct the peer configuration block
        peer_block = f"\n[Peer]\nPublicKey = {req.public_key}\nAllowedIPs = {client_ip}/32\n"
        
        # Append to wg0.conf
        with open(WG_CONF_PATH, "a") as f:
            f.write(peer_block)
            
        # Apply configuration without dropping existing tunnels
        # wg syncconf wg0 <(wg-quick strip wg0)
        # Since we are in Python, we use a shell to handle the process substitution
        cmd = f"sudo wg syncconf wg0 <(sudo wg-quick strip wg0)"
        subprocess.run(cmd, shell=True, check=True)
        
        with open(SERVER_PUB_KEY_PATH, "r") as f:
            server_pub_key = f.read().strip()
            
        return {
            "status": "success",
            "assigned_ip": client_ip,
            "server_public_key": server_pub_key,
            "message": f"Device {req.device_name} registered successfully."
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
