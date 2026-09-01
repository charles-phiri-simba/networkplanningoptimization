package com.simba.snip.npo.productionchange.protocol;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Sha256Hex {

    private Sha256Hex() {
    }

    public static String hash(String utf8) {
        return hashBytes(utf8.getBytes(StandardCharsets.UTF_8));
    }

    public static String hashBytes(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    public static String genesisHash() {
        return hash("SNIP-PHASE16-PRODUCTION-CHANGE-AUDIT-GENESIS-v1");
    }
}
