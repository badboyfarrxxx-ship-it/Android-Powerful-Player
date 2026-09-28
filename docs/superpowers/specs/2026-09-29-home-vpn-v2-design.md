# Home-Server VPN V2.0 Design Specification: The Professional Suite

## 1. Overview
V2.0 transforms the Home-Server VPN from a functional tunnel into a professional-grade private network infrastructure. The focus shifts toward stealth, intelligence, granular control, and operational resilience.

## 2. Security & Stealth Hardening

### 2.1 Traffic Obfuscation (Stealth Mode)
To hide VPN usage from Deep Packet Inspection (DPI) and ISP monitoring.
- **XOR Masking**: Implement a lightweight XOR-masking layer between the WireGuard core and the UDP socket.
- **Rotating Masks**: The XOR mask rotates every 100 packets using a pre-shared seed, preventing static signature detection.
- **TLS Wrapping**: Option to encapsulate UDP traffic within a TLS/HTTPS stream to mimic standard web browsing for high-restriction environments.

### 2.2 MFA Onboarding & Key Management
- **OOB Verification**: Implement a challenge-response flow. The Pi generates a 6-digit code sent via Email/SMS; the client must verify this code to activate the peer.
- **Auto-Key Rotation**: Implement a daily rotation of session keys. Clients generate a new temporary key and sync it to the Pi via the existing tunnel to ensure Perfect Forward Secrecy.

## 3. Network Intelligence & Performance

### 3.1 Dynamic Optimization
- **MTU Auto-Discovery**: Implement a "Path MTU Discovery" (PMTUD) probe during handshake to find the maximum packet size, eliminating fragmentation.
- **Adaptive Keep-Alives**: Implement a heartbeat that scales from 25s to 120s based on connection stability to optimize mobile battery life.
- **Multi-Path Failover**: Configure the Pi Gateway to monitor multiple WAN interfaces (e.g., Ethernet and 4G) and seamlessly switch the exit gateway if the primary link fails.

## 4. The Power-User Suite

### 4.1 Hybrid Split-Tunneling
- **App-Based Routing (Android)**: Utilize `VpnService` to specify apps that must use the VPN vs. those that bypass it.
- **Destination-Based Routing (Global)**: Implement a synced "Route List" (Domains/IPs) that forces specific traffic through the tunnel regardless of the app.
- **Priority Logic**: Destination rules override App-based bypass rules for security.

### 4.2 LAN Discovery & Visibility
- **Resource Scanning**: Implement a background ARP/mDNS scanner on the Pi that returns a list of active home devices to the clients.
- **Traffic Visualizer**: An "Engine Room" dashboard showing real-time throughput graphs and round-trip latency (Ping) to the gateway.

## 5. Infrastructure & Management

### 5.1 Dockerized Gateway
- **Containerization**: Move WireGuard, the Onboarding API, and the DDNS client into a single Docker image for 1-click deployment and backup.
- **Persistent Volumes**: Map `/etc/wireguard` and keys to the host filesystem to ensure persistence across container updates.

### 5.2 Integrated Admin Dashboard
- **Remote Management**: A hidden "Admin Mode" in the Android app providing a secure RPC channel to the Pi.
- **Peer Control**: Ability to view all connected devices and revoke access to specific peers in real-time.
- **Health Monitoring**: Real-time monitoring of Pi CPU, RAM, and tunnel load.

## 6. Success Criteria
- **Invisibility**: VPN traffic is not flagged as "WireGuard" by standard ISP DPI.
- **Zero-Downtime**: Multi-path failover switches links without dropping active sessions.
- **Granularity**: Specific apps can be routed locally while specific domains are routed via VPN.
- **Portability**: Gateway can be migrated to new hardware via a single Docker image.
