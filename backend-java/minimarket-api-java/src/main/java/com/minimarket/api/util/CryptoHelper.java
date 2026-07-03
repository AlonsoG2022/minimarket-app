package com.minimarket.api.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

// Cifrado simetrico (AES-256-CBC) para guardar secretos en la BD (ej. la clave de la IA).
// Compatible con el equivalente en .NET (misma passphrase y esquema IV+cipher en base64).
public final class CryptoHelper {

    private static final String PASSPHRASE = "Minimarket-Api-Secret-2026";

    private CryptoHelper() {
    }

    private static SecretKeySpec key() throws Exception {
        byte[] k = MessageDigest.getInstance("SHA-256").digest(PASSPHRASE.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(k, "AES");
    }

    public static String encrypt(String plainText) {
        try {
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new IvParameterSpec(iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(encrypted, 0, out, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo cifrar el valor.", ex);
        }
    }

    public static String decrypt(String cipherTextBase64) {
        try {
            byte[] all = Base64.getDecoder().decode(cipherTextBase64);
            byte[] iv = Arrays.copyOfRange(all, 0, 16);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new IvParameterSpec(iv));
            byte[] plain = cipher.doFinal(all, 16, all.length - 16);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new RuntimeException("No se pudo descifrar el valor.", ex);
        }
    }
}
