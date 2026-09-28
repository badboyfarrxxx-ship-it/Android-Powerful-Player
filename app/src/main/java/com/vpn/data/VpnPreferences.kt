package com.vpn.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class VpnPreferences(context: Context) {
    private val prefFile = "vpn_secure_prefs"
    private val sharedPreferences: SharedPreferences = try {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            prefFile,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        throw RuntimeException("Failed to initialize EncryptedSharedPreferences", e)
    }

    fun saveServerConfig(publicKey: String, assignedIp: String) {
        sharedPreferences.edit()
            .putString("server_public_key", publicKey)
            .putString("assigned_ip", assignedIp)
            .apply()
    }

    fun getServerPublicKey(): String? = sharedPreferences.getString("server_public_key", null)

    fun getAssignedIp(): String? = sharedPreferences.getString("assigned_ip", null)

    fun saveOnboardingData(serverDomain: String, deviceName: String) {
        sharedPreferences.edit()
            .putString("server_domain", serverDomain)
            .putString("device_name", deviceName)
            .apply()
    }

    fun getServerDomain(): String? = sharedPreferences.getString("server_domain", null)

    fun getDeviceName(): String? = sharedPreferences.getString("device_name", null)
}
