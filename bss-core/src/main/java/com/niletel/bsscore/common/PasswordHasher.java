package com.niletel.bsscore.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class PasswordHasher {

    private PasswordHasher() {
    }

    public static String sha256Hex(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password must not be null");
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean isEqualHex(String expectedHashHex, String actualHashHex) {
        if (expectedHashHex == null || actualHashHex == null) {
            return false;
        }

        try {
            return MessageDigest.isEqual(
                    HexFormat.of().parseHex(expectedHashHex),
                    HexFormat.of().parseHex(actualHashHex)
            );
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
