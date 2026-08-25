package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.integration.ImportFailureCode;

import java.net.InetAddress;
import java.net.URI;
import java.util.Locale;
import java.util.Set;

public final class NetworkPolicyEnforcer {

    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "169.254.169.254",
            "metadata.google.internal",
            "metadata.azure.com"
    );

    private NetworkPolicyEnforcer() {
    }

    public static URI validate(URI uri, ConnectorNetworkPolicy policy) {
        if (uri == null || policy == null) {
            throw deny("network destination is not configured");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (policy.httpsOnly() && !"https".equals(scheme)) {
            throw deny("scheme is not https");
        }
        if ("file".equals(scheme) || "ftp".equals(scheme) || "http".equals(scheme)) {
            throw deny("scheme is not allowed");
        }
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw deny("hostname is missing");
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        if (BLOCKED_HOSTS.contains(normalized)) {
            throw deny("destination is not allowed");
        }
        if (isLinkLocal(normalized)) {
            throw deny("destination is not allowed");
        }
        boolean hostAllowed = policy.allowedHostnames().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals(normalized));
        if (!hostAllowed) {
            throw deny("hostname is not allow-listed");
        }
        int port = uri.getPort() <= 0 ? 443 : uri.getPort();
        if (!policy.allowedPorts().contains(port)) {
            throw deny("port is not allow-listed");
        }
        if (policy.allowRedirects()) {
            throw deny("redirects are not permitted");
        }
        return uri;
    }

    private static boolean isLinkLocal(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isLinkLocalAddress() || address.isAnyLocalAddress();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static ConnectorSecurityException deny(String reason) {
        return new ConnectorSecurityException(ImportFailureCode.NETWORK_POLICY_DENIED, reason);
    }
}
