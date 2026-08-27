package com.simba.snip.npo.integration.security;

public record ResolvedVaultSecret(String name, String version, String value, boolean enabled) {
}
