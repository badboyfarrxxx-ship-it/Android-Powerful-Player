#pragma once
#include <string>
#include <windows.h>
#include <wincrypt.h>

class ConfigManager {
public:
    static bool SaveSecureString(const std::string& key, const std::string& value);
    static std::string LoadSecureString(const std::string& key);
    static void ClearConfig();

private:
    static std::string GetConfigPath();
};
