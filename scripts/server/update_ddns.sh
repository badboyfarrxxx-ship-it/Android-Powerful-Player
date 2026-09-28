#!/bin/bash
set -e

# Configuration - User should edit these or set as env vars
DDNS_PROVIDER="duckdns" # Example provider
DOMAIN="your-domain"    # User's DuckDNS domain
TOKEN="your-token"      # User's DuckDNS token
CACHE_FILE="/tmp/current_public_ip.txt"

# 1. Get current public IP
CURRENT_IP=$(curl -s https://ifconfig.me)

if [ -z "$CURRENT_IP" ]; then
    echo "Error: Could not fetch public IP"
    exit 1
fi

# 2. Check if IP has changed
if [ -f "$CACHE_FILE" ]; then
    LAST_IP=$(cat "$CACHE_FILE")
    if [ "$CURRENT_IP" == "$LAST_IP" ]; then
        echo "IP has not changed ($CURRENT_IP). Skipping update."
        exit 0
    fi
fi

echo "IP change detected: $LAST_IP -> $CURRENT_IP"

# 3. Update the DDNS provider
case $DDNS_PROVIDER in
    "duckdns")
        # DuckDNS update URL
        URL="https://www.duckdns.org/update?domains=$DOMAIN&token=$TOKEN&ip=$CURRENT_IP"
        RESPONSE=$(curl -s "$URL")
        if [ "$RESPONSE" == "OK" ]; then
            echo "Successfully updated DuckDNS."
        else
            echo "DuckDNS update failed: $RESPONSE"
            exit 1
        fi
        ;;
    *)
        echo "Unsupported DDNS provider: $DDNS_PROVIDER"
        exit 1
        ;;
esac

# 4. Cache the new IP
echo "$CURRENT_IP" > "$CACHE_FILE"
