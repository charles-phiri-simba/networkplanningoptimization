package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.integration.ImportFailureCode;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

public final class JavaHttpReadOnlyVendorClient implements ReadOnlyVendorClient {

    private final URI inventoryUri;
    private final HttpClient httpClient;
    private final ConnectorSslContextFactory.CapturingTrustManager capturingTrustManager;
    private final String authorizationHeader;
    private volatile String fingerprint;

    public JavaHttpReadOnlyVendorClient(
            URI inventoryUri,
            SSLContext sslContext,
            ConnectorSslContextFactory.CapturingTrustManager capturingTrustManager,
            String username,
            char[] password
    ) {
        this.inventoryUri = inventoryUri;
        this.capturingTrustManager = capturingTrustManager;
        SSLParameters parameters = new SSLParameters();
        parameters.setEndpointIdentificationAlgorithm("HTTPS");
        parameters.setProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
        this.httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .sslParameters(parameters)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        if (username != null && password != null) {
            String token = username + ":" + new String(password);
            this.authorizationHeader = "Basic " + Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
        } else {
            this.authorizationHeader = null;
        }
    }

    @Override
    public byte[] readInventory() {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(inventoryUri)
                    .timeout(Duration.ofSeconds(10))
                    .GET();
            if (authorizationHeader != null) {
                builder.header("Authorization", authorizationHeader);
            }
            HttpResponse<byte[]> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
            captureFingerprint();
            if (response.statusCode() == 401 || response.statusCode() == 403) {
                throw new ConnectorSecurityException(
                        ImportFailureCode.CONNECTOR_AUTHENTICATION_FAILED, "connector authentication failed");
            }
            if (response.statusCode() != 200) {
                throw new ConnectorSecurityException(
                        ImportFailureCode.SNAPSHOT_READ_FAILED, "connector inventory read failed");
            }
            return response.body() == null ? new byte[0] : response.body();
        } catch (ConnectorSecurityException ex) {
            throw ex;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ConnectorSecurityException(ImportFailureCode.SNAPSHOT_READ_FAILED, "connector inventory read interrupted", ex);
        } catch (Exception ex) {
            Throwable cursor = ex;
            while (cursor != null) {
                if (cursor instanceof ConnectorSecurityException security) {
                    throw security;
                }
                cursor = cursor.getCause();
            }
            String name = ex.getClass().getSimpleName();
            if (name.contains("SSL") || name.contains("Certificate") || name.contains("Handshake")) {
                throw new ConnectorSecurityException(ImportFailureCode.TLS_TRUST_FAILED, "TLS trust failed", ex);
            }
            throw new ConnectorSecurityException(ImportFailureCode.SNAPSHOT_READ_FAILED, "connector inventory read failed", ex);
        }
    }

    @Override
    public String serverCertificateFingerprint() {
        return fingerprint;
    }

    @Override
    public void close() {
        // HttpClient does not require explicit shutdown for Phase 9 short-lived sessions.
    }

    private void captureFingerprint() {
        X509Certificate[] chain = capturingTrustManager.capturedChain();
        if (chain == null || chain.length == 0) {
            return;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(chain[0].getEncoded());
            fingerprint = HexFormat.of().formatHex(digest);
        } catch (Exception ignored) {
            fingerprint = null;
        }
    }
}
