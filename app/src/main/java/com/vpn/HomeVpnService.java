package com.vpn;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.net.VpnService.Builder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicLong;

public class HomeVpnService extends VpnService {
    private static final String TAG = "HomeVpnService";
    private static final String CHANNEL_ID = "HomeVPN_Channel";

    private ParcelFileDescriptor vpnInterface;
    private FileChannel tunChannel;
    private DatagramChannel udpChannel;
    private Thread readThread;
    private Thread writeThread;
    private boolean isRunning = false;

    private final NativeEncryptionCore nativeCore = new NativeEncryptionCore();

    // These would be provided by onboarding/settings
    private final String vpnIp = "10.0.0.2";
    private final String dnsIp = "10.0.0.1";
    private final String gatewayIp = "1.2.3.4"; // Public IP of Pi
    private final int gatewayPort = 51820;

    private final ByteBuffer key = ByteBuffer.allocateDirect(32);
    private final ByteBuffer nonce = ByteBuffer.allocateDirect(12);
    private final ByteBuffer sessionPrivKey = ByteBuffer.allocateDirect(32);
    private final ByteBuffer sessionPubKey = ByteBuffer.allocateDirect(32);

    private final AtomicLong bytesUp = new AtomicLong(0);
    private final AtomicLong bytesDown = new AtomicLong(0);

    // XOR Masking State
    private long maskingSeed = 0xDEADBEEFCAFEBABEL;
    private long txCounter = 0;
    private long rxCounter = 0;
    private int packetCount = 0;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForegroundService();
        startVpn();
        return START_STICKY;
    }

    private void startForegroundService() {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Home VPN", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(channel);
        }

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Home VPN Active")
                .setContentText("Routing traffic through Raspberry Pi")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();

        startForeground(1, notification);
    }

    private void startVpn() {
        Builder builder = new Builder();
        builder.setSession("HomeVPN")
               .addAddress(vpnIp, 24)
               .addDnsServer(dnsIp)
               .addRoute("0.0.0.0", 0);

        try {
            vpnInterface = builder.establish();
            tunChannel = java.nio.channels.Channels.newChannel(vpnInterface.getFileDescriptor());

            udpChannel = DatagramChannel.open();
            udpChannel.configureBlocking(true);

            isRunning = true;

            // Start Packet Processing Loops
            readThread = new Thread(this::readLoop, "VPN-Read-Thread");
            writeThread = new Thread(this::writeLoop, "VPN-Write-Thread");

            readThread.start();
            writeThread.start();

            // Schedule daily key rotation
            scheduleKeyRotation();

            Log.i(TAG, "VPN Interface established and loops started");
        } catch (IOException e) {
            Log.e(TAG, "Failed to establish VPN interface", e);
        }
    }

    private void scheduleKeyRotation() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    rotateSessionKeys();
                    handler.postDelayed(this, 24 * 60 * 60 * 1000L);
                }
            }
        }, 24 * 60 * 60 * 1000L);
    }

    private void rotateSessionKeys() {
        Log.i(TAG, "Triggering daily session key rotation...");

        int result = nativeCore.generateSessionKeyPair(sessionPubKey, sessionPrivKey);
        if (result != 0) {
            Log.e(TAG, "Failed to generate session key pair: " + result);
            return;
        }

        try {
            // Control Packet Format: [Type: 1 byte][Length: 1 byte][PubKey: 32 bytes]
            ByteBuffer controlPacket = ByteBuffer.allocateDirect(34);
            controlPacket.put((byte) 0xCF); // Control packet marker
            controlPacket.put((byte) 32);    // Payload length

            sessionPubKey.rewind();
            controlPacket.put(sessionPubKey);
            controlPacket.flip();

            // Encrypt the control packet using current tunnel key
            ByteBuffer encryptedControl = ByteBuffer.allocateDirect(34 + 16);
            int encResult = nativeCore.encryptPacket(
                    controlPacket, 0, 34,
                    key, nonce,
                    encryptedControl, 0
            );

            if (encResult == 0) {
                encryptedControl.flip();
                // Apply XOR masking
                nativeCore.maskPacket(encryptedControl, 0, encryptedControl.remaining(), maskingSeed, txCounter++);

                InetSocketAddress remoteAddress = new InetSocketAddress(gatewayIp, gatewayPort);
                udpChannel.send(encryptedControl, remoteAddress);
                Log.i(TAG, "Session key sync packet sent to gateway");
            } else {
                Log.e(TAG, "Failed to encrypt control packet: " + encResult);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error sending key rotation packet", e);
        }
    }

    private void readLoop() {
        try {
            ByteBuffer packetBuffer = ByteBuffer.allocateDirect(2048);
            ByteBuffer encryptedBuffer = ByteBuffer.allocateDirect(2048 + 16);
            InetSocketAddress remoteAddress = new InetSocketAddress(gatewayIp, gatewayPort);

            while (isRunning) {
                packetBuffer.clear();
                int read = tunChannel.read(packetBuffer);
                if (read == -1) break;
                packetBuffer.flip();

                encryptedBuffer.clear();
                int result = nativeCore.encryptPacket(
                        packetBuffer, 0, read,
                        key, nonce,
                        encryptedBuffer, 0
                );

                if (result == 0) {
                    encryptedBuffer.flip();

                    // Mask packet before UDP send
                    nativeCore.maskPacket(encryptedBuffer, 0, encryptedBuffer.remaining(), maskingSeed, txCounter++);

                    udpChannel.send(encryptedBuffer, remoteAddress);
                    bytesUp.addAndGet(read);

                    // Seed rotation every 100 packets
                    if (++packetCount % 100 == 0) {
                        maskingSeed ^= 0x5555555555555555L;
                        txCounter = 0;
                        rxCounter = 0;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in read loop", e);
        }
    }

    private void writeLoop() {
        try {
            ByteBuffer ciphertextBuffer = ByteBuffer.allocateDirect(2048 + 16);
            ByteBuffer plaintextBuffer = ByteBuffer.allocateDirect(2048);

            while (isRunning) {
                ciphertextBuffer.clear();
                int read = udpChannel.receive(ciphertextBuffer);
                if (read == -1) break;
                ciphertextBuffer.flip();

                // Unmask packet immediately after UDP receive
                nativeCore.unmaskPacket(ciphertextBuffer, 0, ciphertextBuffer.remaining(), maskingSeed, rxCounter++);

                plaintextBuffer.clear();
                int result = nativeCore.decryptPacket(
                        ciphertextBuffer, 0, read,
                        key, nonce,
                        plaintextBuffer, 0
                );

                if (result == 0) {
                    plaintextBuffer.flip();
                    tunChannel.write(plaintextBuffer);
                    bytesDown.addAndGet(read);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in write loop", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;

        if (readThread != null) readThread.interrupt();
        if (writeThread != null) writeThread.interrupt();

        try {
            if (udpChannel != null) udpChannel.close();
            if (tunChannel != null) tunChannel.close();
            if (vpnInterface != null) vpnInterface.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing resources", e);
        }
        Log.i(TAG, "VPN Service destroyed and resources cleaned");
    }

    public long getBytesUp() { return bytesUp.get(); }
    public long getBytesDown() { return bytesDown.get(); }
}
