#!/bin/bash
set -e

echo "Installing WireGuard and prerequisites..."
sudo apt-get update && sudo apt-get install -y wireguard iptables

# Key Generation
echo "Generating server keys..."
mkdir -p /etc/wireguard/keys
cd /etc/wireguard/keys
umask 077
wg genkey | tee privatekey | wg pubkey > publickey

PRIV_KEY=$(cat privatekey)
PUB_KEY=$(cat publickey)

# Base Configuration
echo "Creating wg0.conf..."
cat << WG_CONF > /etc/wireguard/wg0.conf
[Interface]
PrivateKey = $PRIV_KEY
Address = 10.0.0.1/24
ListenPort = 51820
SaveConfig = true

# Firewall rules will be handled by separate setup script
WG_CONF

# IP Forwarding
echo "Enabling IP forwarding..."
echo "net.ipv4.ip_forward=1" | sudo tee /etc/sysctl.d/99-vpn-forwarding.conf
sudo sysctl -p /etc/sysctl.d/99-vpn-forwarding.conf

# Service Setup
echo "Starting WireGuard service..."
sudo systemctl enable wg-quick@wg0
sudo systemctl start wg-quick@wg0

echo "WireGuard base installation complete."
echo "Server Public Key: $PUB_KEY"
