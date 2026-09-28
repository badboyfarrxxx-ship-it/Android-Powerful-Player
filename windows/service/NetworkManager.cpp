#include "NetworkManager.h"
#include <iostream>
#include <sstream>
#include <array>
#include <memory>

NetworkManager::NetworkManager() {}
NetworkManager::~NetworkManager() {}

bool NetworkManager::executeCommand(const std::string& cmd) {
    // Helper to execute shell commands and return success
    int result = std::system(cmd.c_str());
    return result == 0;
}

std::string NetworkManager::GetInterfaceName() {
    // In a real implementation, we would query the wireguard-nt driver
    // For this implementation, we assume the default interface name
    return "WireGuard";
}

bool NetworkManager::loadWireGuardDriver() {
    // Use 'sc start' or 'net start' to ensure the driver is active
    return executeCommand("sc start wireguard-nt");
}

bool NetworkManager::configureInterface(const std::string& ip, const std::string& publicKey) {
    // Using the wireguard-nt command line tool (wg.exe) for configuration
    std::stringstream ss;
    ss << "wg set " << GetInterfaceName() << " ip=" << ip << "/24";
    if (!executeCommand(ss.str())) return false;

    ss.str("");
    ss << "wg set " << GetInterfaceName() << " peer " << publicKey << " allowed-ips=0.0.0.0/0";
    return executeCommand(ss.str());
}

bool NetworkManager::setInterfaceState(bool up) {
    std::string state = up ? "up" : "down";
    std::stringstream ss;
    ss << "netsh interface set interface \"" << GetInterfaceName() << "\" " << state;
    return executeCommand(ss.str());
}

bool NetworkManager::setDefaultGateway(const std::string& gatewayIp) {
    // 1. Remove existing default gateway
    executeCommand("route delete 0.0.0.0");
    
    // 2. Add VPN as default gateway
    std::stringstream ss;
    ss << "route add 0.0.0.0 mask 0.0.0.0 " << gatewayIp << " metric 1";
    return executeCommand(ss.str());
}

bool NetworkManager::addRoute(const RouteConfig& route) {
    std::stringstream ss;
    ss << "route add " << route.destination << " mask " << route.mask << " " << route.gateway << " metric " << route.metric;
    return executeCommand(ss.str());
}

bool NetworkManager::clearVpnRoutes() {
    return executeCommand("route delete 0.0.0.0");
}

bool NetworkManager::setSystemDns(const std::string& dnsIp) {
    // Use netsh to set DNS for the WireGuard interface
    std::stringstream ss;
    ss << "netsh interface ip set dns name=\"" << GetInterfaceName() << "\" static " << dnsIp;
    return executeCommand(ss.str());
}

bool NetworkManager::sendPacket(const std::vector<uint8_t>& packet) {
    // In a real implementation, this would send the packet over the UDP socket
    // using the WireGuard-NT driver or a raw socket.
    // For this implementation, we simulate a successful send to the gateway.
    std::cout << "[NetworkManager] Sending stealth-masked packet (" << packet.size() << " bytes) to gateway..." << std::endl;
    return true;
}
