package com.simba.snip.npo.productionchange.audit;

/**
 * App-side facade for the shared protocol SHA-256 helper.
 */
public final class Sha256Hex {

    private Sha256Hex() {
    }

    public static String hash(String utf8) {
        return com.simba.snip.npo.productionchange.protocol.Sha256Hex.hash(utf8);
    }

    public static String genesisHash() {
        return com.simba.snip.npo.productionchange.protocol.Sha256Hex.genesisHash();
    }
}
