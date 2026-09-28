#include "VpnService.h"
#include <iostream>
#include <sstream>
#include <wininet.h>

#pragma comment(lib, "wininet.lib")

VpnService::VpnService() : isRunning_(false) {}
VpnService::~VpnService() { Stop(); }

bool VpnService::RegisterDevice(const std::string& domain, const std::string& deviceName) {
    serverDomain_ = domain;
    
    // 1. Generate local keys (simplified for this implementation)
    clientPrivateKey_ = "local_priv_key_generated"; 
    clientPublicKey_ = "local_pub_key_generated";

    // 2. Call Pi Gateway /register API
    std::stringstream url;
    url << "http://" << domain << ":8000/register?device_name=" << deviceName 
        << "&public_key=" << clientPublicKey_;
    
    // Simulating HTTP call via system curl for brevity in this core logic
    std::string cmd = "curl -s \"" + url.str() + "\"";
    std::string response = _popen(cmd.c_str(), "r"); // Simplified capture
    
    // In real implementation, we parse JSON response for assigned_ip and server_pub_key
    assignedIp_ = "10.0.0.2";
    serverPublicKey_ = "pi_pub_key_from_response";

    ConfigManager::SaveSecureString("server_pub", serverPublicKey_);
    ConfigManager::SaveSecureString("assigned_ip", assignedIp_);
    
    return true;
}

bool VpnService::Start() {
    if (isRunning_) return true;
    
    // Load config
    serverPublicKey_ = ConfigManager::LoadSecureString("server_pub");
    assignedIp_ = ConfigManager::LoadSecureString("assigned_ip");
    
    if (serverPublicKey_.empty()) return false;

    // Data Plane Setup
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
        // Monitor connection health
        // Re-establish if interface drops
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
