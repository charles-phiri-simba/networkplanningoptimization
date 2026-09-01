package com.simba.snip.npo.integration.security;

public interface ReadOnlyVendorClient extends AutoCloseable {

    byte[] readInventory();

    String serverCertificateFingerprint();

    @Override
    void close();
}
