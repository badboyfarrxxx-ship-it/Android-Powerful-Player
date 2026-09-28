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
        public final String status;
        public final String peerId;
        public final String publicKey;
        public final String assignedIp;

        public RegisterResponse(String status, String peerId, String publicKey, String assignedIp) {
            this.status = status;
            this.peerId = peerId;
            this.publicKey = publicKey;
            this.assignedIp = assignedIp;
        }
    }

    public RegisterResponse registerDevice(String serverDomain, String deviceName, String publicKey) throws IOException, JSONException {
        String urlString = "https://" + serverDomain + "/register";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        JSONObject payload = new JSONObject();
        payload.put("device_name", deviceName);
        payload.put("public_key", publicKey);

        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        if (conn.getResponseCode() != 200) {
            throw new IOException("Registration failed: HTTP " + conn.getResponseCode());
        }

        try (Scanner scanner = new Scanner(conn.getInputStream(), StandardCharsets.UTF_8.name())) {
            String response = scanner.useDelimiter("\\A").next();
            JSONObject json = new JSONObject(response);

            String status = json.getString("status");
            String peerId = json.optString("peer_id", "");
            String serverPubKey = json.optString("server_public_key", "");
            String assignedIp = json.optString("assigned_ip", "");

            return new RegisterResponse(status, peerId, serverPubKey, assignedIp);
        }
    }

    public boolean verifyDevice(String serverDomain, String peerId, String code) throws IOException, JSONException {
        String urlString = "https://" + serverDomain + "/verify";
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json");

        JSONObject payload = new JSONObject();
        payload.put("peer_id", peerId);
        payload.put("code", code);

        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
        }

        return conn.getResponseCode() == 200;
    }
}
