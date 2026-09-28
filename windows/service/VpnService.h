#pragma once
#include <string>
#include <atomic>
#include <thread>
#include "NetworkManager.h"
#include "../common/ConfigManager.h"

class VpnService {
public:
    VpnService();
    ~VpnService();

    bool Start();
    bool Stop();
    bool IsRunning() const { return isRunning_; }

    // Onboarding
    bool RegisterDevice(const std::string& domain, const std::string& deviceName);

private:
    void ServiceLoop();
    bool PerformHandshake();

    std::atomic<bool> isRunning_;
    std::thread workerThread_;
    NetworkManager networkManager_;
    
    std::string serverDomain_;
    std::string serverPublicKey_;
    std::string clientPrivateKey_;
    std::string clientPublicKey_;
    std::string assignedIp_;
};
