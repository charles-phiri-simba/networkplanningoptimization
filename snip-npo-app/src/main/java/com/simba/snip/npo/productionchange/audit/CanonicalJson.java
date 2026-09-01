package com.simba.snip.npo.productionchange.audit;

/**
 * App-side facade for the shared protocol canonical JSON serializer.
 */
public final class CanonicalJson {

    private CanonicalJson() {
    }

    public static String serialize(Object value) {
        return com.simba.snip.npo.productionchange.protocol.CanonicalJson.serialize(value);
    }
}
