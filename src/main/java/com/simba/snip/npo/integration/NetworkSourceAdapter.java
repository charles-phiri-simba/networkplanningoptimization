package com.simba.snip.npo.integration;

public interface NetworkSourceAdapter {

    Vendor vendor();

    String sourceSystem();

    String schemaVersion();

    SourceSnapshot readSnapshot(FixtureKind kind);
}
