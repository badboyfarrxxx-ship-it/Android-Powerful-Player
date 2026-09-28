#pragma once
#include <string>
#include <vector>
#include <windows.h>

struct RouteConfig {
    std::string destination;
    std::string mask;
    std::string gateway;
    int metric;
};

class NetworkManager {
public:
    NetworkManager();
    ~NetworkManager();

    // Driver Management
    bool loadWireGuardDriver();
    bool configureInterface(const std::string& ip, const std::string& publicKey);
    bool setInterfaceState(bool up);

    // Routing Table Management
    bool setDefaultGateway(const std::string& gatewayIp);
    bool addRoute(const RouteConfig& route);
    bool clearVpnRoutes();

    // DNS Configuration
    bool setSystemDns(const std::string& dnsIp);
    bool sendPacket(const std::vector<uint8_t>& packet);

private:
    bool executeCommand(const std::string& cmd);
    std::string GetInterfaceName();
};
