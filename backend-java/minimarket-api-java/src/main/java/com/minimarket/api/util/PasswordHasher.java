package com.minimarket.api.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class PasswordHasher {

    public String hash(String value) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            var builder = new StringBuilder(hash.length * 2);

            for (byte current : hash) {
                builder.append(String.format("%02x", current));
            }

            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("No se pudo generar el hash de la contrasena.", ex);
        }
    }

    public boolean verify(String plainText, String hash) {
        return hash(plainText).equals(hash);
    }
}
