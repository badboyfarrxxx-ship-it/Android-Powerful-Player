package com.vpn;

import org.conscrypt.OpenSSLProvider;
import java.security.*;
import java.security.spec.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class NativeEncryptionCore {
    static {
        // Initialize Conscrypt provider for high-performance Android encryption
        Security.insertProviderAt(new OpenSSLProvider(), 1);
    }

    private static final String ALGORITHM = "ChaCha20-Poly1305";
    private static final int KEY_SIZE = 32;
    private static final int NONCE_SIZE = 12;
    private static final int TAG_SIZE = 16;

    public int generateSessionKeyPair(ByteBuffer pubKeyBuf, ByteBuffer privKeyBuf) {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("X25519");
            KeyPair kp = kpg.generateKeyPair();

            byte[] pub = kp.getPublic().getEncoded();
            byte[] priv = kp.getPrivate().getEncoded();

            // X25519 encoded keys in Java often include headers; we need the raw bytes
            // Raw public key is the last 32 bytes of the SubjectPublicKeyInfo
            byte[] rawPub = new byte[KEY_SIZE];
            System.arraycopy(pub, pub.length - KEY_SIZE, rawPub, 0, KEY_SIZE);

            // Raw private key is the last 32 bytes of the PKCS#8 format
            byte[] rawPriv = new byte[KEY_SIZE];
            System.arraycopy(priv, priv.length - KEY_SIZE, rawPriv, 0, KEY_SIZE);

            pubKeyBuf.clear();
            pubKeyBuf.put(rawPub);
            privKeyBuf.clear();
            privKeyBuf.put(rawPriv);

            return 0;
        } catch (Exception e) {
            return -1;
        }
    }

    public int encryptPacket(
            ByteBuffer plaintext, int plaintextPos, int plaintextLen,
            ByteBuffer key, ByteBuffer nonce,
            ByteBuffer ciphertext, int ciphertextPos) {
        try {
            byte[] keyBytes = new byte[KEY_SIZE];
            key.position(0);
            key.get(keyBytes);

            byte[] nonceBytes = new byte[NONCE_SIZE];
            nonce.position(0);
            nonce.get(nonceBytes);

            byte[] plainBytes = new byte[plaintextLen];
            plaintext.position(plaintextPos);
            plaintext.get(plainBytes);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "ChaCha20");
            IvParameterSpec ivSpec = new IvParameterSpec(nonceBytes);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(plainBytes);

            ciphertext.position(ciphertextPos);
            ciphertext.put(encrypted);

            return 0;
        } catch (Exception e) {
            return -1;
        }
    }

    public int decryptPacket(
            ByteBuffer ciphertext, int ciphertextPos, int ciphertextLen,
            ByteBuffer key, ByteBuffer nonce,
            ByteBuffer plaintext, int plaintextPos) {
        try {
            byte[] keyBytes = new byte[KEY_SIZE];
            key.position(0);
            key.get(keyBytes);

            byte[] nonceBytes = new byte[NONCE_SIZE];
            nonce.position(0);
            nonce.get(nonceBytes);

            byte[] cipherBytes = new byte[ciphertextLen];
            ciphertext.position(ciphertextPos);
            ciphertext.get(cipherBytes);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "ChaCha20");
            IvParameterSpec ivSpec = new IvParameterSpec(nonceBytes);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decrypted = cipher.doFinal(cipherBytes);

            plaintext.position(plaintextPos);
            plaintext.put(decrypted);

            return 0;
        } catch (Exception e) {
            return -1;
        }
    }

    public void maskPacket(ByteBuffer packet, int pos, int len, long seed, long counter) {
        // Implementing the same SplitMix64-based XOR masking in Java
        packet.position(pos);
        for (int i = 0; i < len; i++) {
            long z = (seed + counter);
            z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
            z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
            z = z ^ (z >>> 31);

            byte mask = (byte) (z & 0xFF);
            byte original = packet.get();
            packet.put(pos + i, (byte) (original ^ mask));
            counter++;
        }
    }

    public void unmaskPacket(ByteBuffer packet, int pos, int len, long seed, long counter) {
        maskPacket(packet, pos, len, seed, counter);
    }
}
