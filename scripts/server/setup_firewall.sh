#!/bin/bash
set -e

# Automatically detect the primary network interface
INTERFACE=$(ip route get 8.8.8.8 | grep -Po 'dev \K\S+')
echo "Detected primary interface: $INTERFACE"

echo "Configuring Firewall and NAT..."

# 1. Enable Masquerading for the WireGuard subnet
# This allows clients on 10.0.0.0/24 to access the internet via the Pi
sudo iptables -t nat -A POSTROUTING -s 10.0.0.0/24 -o $INTERFACE -j MASQUERADE

# 2. Ensure forwarding is allowed for the WireGuard interface
sudo iptables -A FORWARD -i wg0 -j ACCEPT
sudo iptables -A FORWARD -o wg0 -j ACCEPT

# 3. LAN Access: Allow traffic to local network
# We assume the local network is the subnet of the detected interface
LOCAL_NET=$(ip -o -f inet addr show $INTERFACE | awk '{print $4}')
echo "Allowing access to local network: $LOCAL_NET"
sudo iptables -A FORWARD -s 10.0.0.0/24 -d $LOCAL_NET -j ACCEPT

# 4. Make rules persistent
if command -v iptables-save >/dev/null; then
    sudo apt-get install -y iptables-persistent
    sudo netfilter-persistent save
fi

echo "Firewall configuration complete."
