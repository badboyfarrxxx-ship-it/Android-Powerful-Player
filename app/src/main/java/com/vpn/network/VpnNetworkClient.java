package com.vpn.network;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.json.JSONObject;
import org.json.JSONException;

public class VpnNetworkClient {
    public static class RegisterResponse {
        public final String publicKey;
        public final String assignedIp;

        public RegisterResponse(String publicKey, String assignedIp) {
            this.publicKey = publicKey;
            this.assignedIp = assignedIp;
        }
    }

    public RegisterResponse registerDevice(String serverDomain, String deviceName) throws IOException, JSONException {
        String urlString = "https://" + serverDomain + "/register";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        JSONObject payload = new JSONObject();
        payload.put("device_name", deviceName);

        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        if (conn.getResponseCode() != 200) {
            throw new IOException("Registration failed: HTTP " + conn.getResponseCode());
        }

        try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
            String response = scanner.useDelimiter("\\A").next();
            JSONObject json = new JSONObject(response);
            return new RegisterResponse(
                    json.getString("public_key"),
                    json.getString("assigned_ip")
            );
        }
    }
}
