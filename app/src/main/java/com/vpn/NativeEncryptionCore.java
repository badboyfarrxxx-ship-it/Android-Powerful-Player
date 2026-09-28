package com.vpn;

import java.nio.ByteBuffer;

public class NativeEncryptionCore {
    static {
        System.loadLibrary("wireguard_core");
    }

    public native int encryptPacket(
        ByteBuffer plaintext, int plaintextPos, int plaintextLen,
        ByteBuffer key, ByteBuffer nonce,
        ByteBuffer ciphertext, int ciphertextPos
    );

    public native int decryptPacket(
        ByteBuffer ciphertext, int ciphertextPos, int ciphertextLen,
        ByteBuffer key, ByteBuffer nonce,
        ByteBuffer plaintext, int plaintextPos
    );
}
