#include "VpnService.h"
#include "NetworkManager.h"
#include "../../jni/vpn/WireGuardCore.h"
#include <iostream>
#include <sstream>
#include <wininet.h>
#include <nlohmann/json.hpp>
#include <vector>
#include <memory>
#include <chrono>
#include <thread>
#include <openssl/crypto.h>

using json = nlohmann::json;

#pragma comment(lib, "wininet.lib")
#pragma comment(lib, "libcrypto")

VpnService::VpnService() : isRunning_(false) {}
VpnService::~VpnService() { Stop(); }

bool VpnService::RegisterDevice(const std::string& domain, const std::string& deviceName) {
    serverDomain_ = domain;

    // 1. Generate local keys using secure X25519
    uint8_t pub[32], priv[32];
    generate_x25519_keypair(pub, priv);

    clientPublicKey_ = std::string((char*)pub, 32); // Simplified for example
    clientPrivateKey_ = std::string((char*)priv, 32);

    // 2. Call Pi Gateway /register API
    std::stringstream url;
    url << "http://" << domain << ":8000/register?device_name=" << deviceName
        << "&public_key=" << clientPublicKey_;

    std::string cmd = "curl -s \"" + url.str() + "\"";

    // Proper response capture
    std::string response_text;
    FILE* pipe = _popen(cmd.c_str(), "r");
    if (!pipe) return false;

    char buffer[128];
    while (fgets(buffer, sizeof(buffer), pipe) != NULL) {
        response_text += buffer;
    }
    _pclose(pipe);

    try {
        auto j = json::parse(response_text);
        if (j.contains("status") && j["status"] == "pending") {
            currentState_ = VpnState::PENDING_VERIFICATION;
            return true;
        } else if (j.contains("status") && j["status"] == "active") {
            currentState_ = VpnState::ACTIVE;
            serverPublicKey_ = j.value("server_public_key", "");
            assignedIp_ = j.value("assigned_ip", "");
        } else {
            currentState_ = VpnState::ERROR;
            return false;
        }
    } catch (const json::parse_error& e) {
        std::cerr << "JSON Parse Error: " << e.what() << std::endl;
        currentState_ = VpnState::ERROR;
        return false;
    }

    ConfigManager::SaveSecureString("server_pub", serverPublicKey_);
    ConfigManager::SaveSecureString("assigned_ip", assignedIp_);

    return (currentState_ == VpnState::ACTIVE || currentState_ == VpnState::PENDING_VERIFICATION);
}

bool VpnService::Start() {
    if (isRunning_) return true;

    if (currentState_ != VpnState::ACTIVE) {
        std::cerr << "[SECURITY ERROR] Attempted to start VPN while state is not ACTIVE" << std::endl;
        return false;
    }

    serverPublicKey_ = ConfigManager::LoadSecureString("server_pub");
    assignedIp_ = ConfigManager::LoadSecureString("assigned_ip");

    if (serverPublicKey_.empty()) return false;

    if (!networkManager_.loadWireGuardDriver()) return false;
    if (!networkManager_.configureInterface(assignedIp_, serverPublicKey_)) return false;
    if (!networkManager_.setInterfaceState(true)) return false;
    if (!networkManager_.setDefaultGateway("10.0.0.1")) return false;
    if (!networkManager_.setSystemDns("10.0.0.1")) return false;

    isRunning_ = true;
    workerThread_ = std::thread(&VpnService::ServiceLoop, this);

    return true;
}

void VpnService::ServiceLoop() {
    while (isRunning_) {
        auto now = std::chrono::system_clock::now();
        if (std::chrono::duration_cast<std::chrono::hours>(now - lastRotation_).count() >= 24) {
            Log("Triggering secure session key rotation...");

            uint8_t newPub[32], newPriv[32];
            generate_x25519_keypair(newPub, newPriv);

            // Construct Control Packet: [0xCF][Length=32][PublicKey]
            std::vector<uint8_t> packet;
            packet.push_back(0xCF);
            packet.push_back(32);
            packet.insert(packet.end(), newPub, newPub + 32);

            // XOR Masking for stealth
            uint64_t seed = 0xDEADBEEF; // Should be pre-shared seed
            uint64_t counter = 0;
            apply_stealth_mask(packet.data(), packet.size(), seed, counter);

            if (networkManager_.sendPacket(packet)) {
                Log("Rotation packet sent successfully. Updating local keys...");
                clientPublicKey_ = std::string((char*)newPub, 32);
                clientPrivateKey_ = std::string((char*)newPriv, 32);
                lastRotation_ = now;
            } else {
                Log("Rotation packet failed to send.");
            }

            // Securely zero memory
            OPENSSL_cleanse(newPriv, 32);
            OPENSSL_cleanse(newPub, 32);
        }

        std::this_thread::sleep_for(std::chrono::seconds(10));
    }
}

bool VpnService::Stop() {
    isRunning_ = false;
    if (workerThread_.joinable()) workerThread_.join();

    networkManager_.clearVpnRoutes();
    networkManager_.setInterfaceState(false);
    return true;
}
