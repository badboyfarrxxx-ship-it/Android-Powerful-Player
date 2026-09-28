package com.vpn.data;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

public class VpnPreferences {
    private static final String PREF_FILE = "vpn_secure_prefs";
    private final SharedPreferences sharedPreferences;

    public VpnPreferences(Context context) {
        try {
            String masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC);
            sharedPreferences = EncryptedSharedPreferences.create(
                    PREF_FILE,
                    masterKeyAlias,
                    context,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize EncryptedSharedPreferences", e);
        }
    }

    public void saveServerConfig(String publicKey, String assignedIp) {
        sharedPreferences.edit()
                .putString("server_public_key", publicKey)
                .putString("assigned_ip", assignedIp)
                .apply();
    }

    public String getServerPublicKey() {
        return sharedPreferences.getString("server_public_key", null);
    }

    public String getAssignedIp() {
        return sharedPreferences.getString("assigned_ip", null);
    }

    public void saveOnboardingData(String serverDomain, String deviceName) {
        sharedPreferences.edit()
                .putString("server_domain", serverDomain)
                .putString("device_name", deviceName)
                .apply();
    }

    public String getServerDomain() {
        return sharedPreferences.getString("server_domain", null);
    }

    public String getDeviceName() {
        return sharedPreferences.getString("device_name", null);
    }
}
