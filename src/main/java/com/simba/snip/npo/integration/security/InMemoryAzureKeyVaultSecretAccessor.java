package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.integration.ImportFailureCode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-process Key Vault stand-in for default CI. Never used as a production fallback.
 */
public final class InMemoryAzureKeyVaultSecretAccessor implements AzureKeyVaultSecretAccessor {

    public static final class StoredSecret {
        private final String version;
        private final String value;
        private final boolean enabled;

        public StoredSecret(String version, String value, boolean enabled) {
            this.version = version;
            this.value = value;
            this.enabled = enabled;
        }
    }

    private final Map<String, List<StoredSecret>> secrets = new ConcurrentHashMap<>();
    private final AtomicInteger gets = new AtomicInteger();
    private volatile ImportFailureCode forcedFailure;

    public void put(String secretName, String version, String value, boolean enabled) {
        secrets.compute(secretName, (key, existing) -> {
            List<StoredSecret> versions = existing == null ? new ArrayList<>() : new ArrayList<>(existing);
            versions.removeIf(item -> item.version.equals(version));
            versions.add(new StoredSecret(version, value, enabled));
            return versions;
        });
    }

    public void disableLatest(String secretName) {
        List<StoredSecret> versions = secrets.get(secretName);
        if (versions == null || versions.isEmpty()) {
            return;
        }
        StoredSecret latest = versions.get(versions.size() - 1);
        versions.set(versions.size() - 1, new StoredSecret(latest.version, latest.value, false));
    }

    public void forceFailure(ImportFailureCode code) {
        this.forcedFailure = code;
    }

    public int gets() {
        return gets.get();
    }

    public void resetGets() {
        gets.set(0);
    }

    @Override
    public ResolvedVaultSecret get(AzureVaultCredentialReference reference) {
        gets.incrementAndGet();
        if (forcedFailure != null) {
            throw new ConnectorSecurityException(forcedFailure, "forced vault failure");
        }
        List<StoredSecret> versions = secrets.get(reference.secretName());
        if (versions == null || versions.isEmpty()) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.VAULT_SECRET_NOT_FOUND, "vault secret was not found");
        }
        StoredSecret selected;
        if (reference.pinnedVersion() != null) {
            selected = versions.stream()
                    .filter(item -> item.version.equals(reference.pinnedVersion()))
                    .findFirst()
                    .orElseThrow(() -> new ConnectorSecurityException(
                            ImportFailureCode.VAULT_SECRET_NOT_FOUND, "vault secret was not found"));
        } else {
            selected = versions.get(versions.size() - 1);
        }
        if (!selected.enabled) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.VAULT_SECRET_DISABLED, "vault secret is disabled");
        }
        versions.stream()
                .filter(item -> !item.version.equals(selected.version))
                .max(Comparator.comparing(item -> item.version))
                .ifPresent(ignored -> {
                    // Older versions exist but must not be selected when latest is used.
                });
        return new ResolvedVaultSecret(reference.secretName(), selected.version, selected.value, true);
    }
}
