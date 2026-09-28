#pragma once
#include <string>
#include <atomic>
#include <thread>
#include "NetworkManager.h"
#include "../common/ConfigManager.h"
#include <vector>

enum class VpnState {
    DISCONNECTED,
    REGISTERING,
    PENDING_VERIFICATION,
    ACTIVE,
    ERROR
};

class VpnService {
public:
    VpnService();
    ~VpnService();

    bool Start();
    bool Stop();
    bool IsRunning() const { return isRunning_; }

    // Onboarding
    bool RegisterDevice(const std::string& domain, const std::string& deviceName);
    bool VerifyDevice(const std::string& code);

private:
    void ServiceLoop();
    bool PerformHandshake();

    std::atomic<bool> isRunning_;
    std::atomic<VpnState> currentState_;
    std::thread workerThread_;
    NetworkManager networkManager_;

    std::string serverDomain_;
    std::string serverPublicKey_;
    SecureKey clientPrivateKey_;
    SecureKey clientPublicKey_;
    std::string assignedIp_;
};
