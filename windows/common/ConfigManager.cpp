#include "ConfigManager.h"
#include <fstream>
#include <iostream>

std::string ConfigManager::GetConfigPath() {
    return "vpn_config.dat";
}

bool ConfigManager::SaveSecureString(const std::string& key, const std::string& value) {
    DATA_BLOB dataIn;
    dataIn.pbData = (BYTE*)value.c_str();
    dataIn.cbData = (DWORD)value.length();

    DATA_BLOB dataOut;
    if (CryptProtectData(&dataIn, 0, &dataOut)) {
        std::ofstream outFile(GetConfigPath(), std::ios::binary | std::ios::app);
        outFile << key << "=" << dataOut.cbData << " ";
        outFile.write((char*)dataOut.pbData, dataOut.cbData);
        outFile << "\n";
        LocalFree(dataOut.pbData);
        return true;
    }
    return false;
}

std::string ConfigManager::LoadSecureString(const std::string& key) {
    std::ifstream inFile(GetConfigPath(), std::ios::binary);
    std::string line;
    while (std::getline(inFile, line)) {
        if (line.find(key + "=") == 0) {
            size_t pos = line.find('=');
            int len = std::stoi(line.substr(pos + 1));
            
            // Read the binary blob
            std::vector<BYTE> blob(len);
            inFile.read((char*)blob.data(), len);
            
            DATA_BLOB dataIn;
            dataIn.pbData = blob.data();
            dataIn.cbData = (DWORD)len;
            
            DATA_BLOB dataOut;
            if (CryptUnprotectData(&dataIn, nullptr, nullptr, nullptr, nullptr, 0, &dataOut)) {
                std::string result((char*)dataOut.pbData, dataOut.cbData);
                LocalFree(dataOut.pbData);
                return result;
            }
        }
    }
    return "";
}

void ConfigManager::ClearConfig() {
    std::remove(GetConfigPath().c_str());
}
