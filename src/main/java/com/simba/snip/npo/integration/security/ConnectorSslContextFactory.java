package com.simba.snip.npo.integration.security;

import com.simba.snip.npo.integration.ImportFailureCode;
import org.springframework.stereotype.Component;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ConnectorSslContextFactory {

    public PreparedSsl prepare(ConnectorTrustProfile trust, CredentialHandle clientCredential) {
        try {
            X509TrustManager trustManager = trustManager(trust);
            CapturingTrustManager capturing = new CapturingTrustManager(trustManager);
            KeyManager[] keyManagers = keyManagers(clientCredential);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keyManagers, new TrustManager[]{capturing}, new SecureRandom());
            return new PreparedSsl(context, capturing);
        } catch (ConnectorSecurityException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ConnectorSecurityException(ImportFailureCode.TLS_TRUST_FAILED, "TLS context failed", ex);
        }
    }

    private static X509TrustManager trustManager(ConnectorTrustProfile trust) throws Exception {
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        if (trust.trustMode() == TrustMode.SYSTEM_CA) {
            factory.init((KeyStore) null);
        } else {
            KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
            store.load(null);
            List<X509Certificate> certs = trust.customTrustedCertificates();
            if (certs.isEmpty()) {
                throw new ConnectorSecurityException(ImportFailureCode.TLS_TRUST_FAILED, "custom CA is not configured");
            }
            int index = 0;
            for (X509Certificate cert : certs) {
                store.setCertificateEntry("ca-" + index++, cert);
            }
            factory.init(store);
        }
        for (TrustManager manager : factory.getTrustManagers()) {
            if (manager instanceof X509TrustManager x509) {
                return x509;
            }
        }
        throw new ConnectorSecurityException(ImportFailureCode.TLS_TRUST_FAILED, "trust manager is unavailable");
    }

    private static KeyManager[] keyManagers(CredentialHandle clientCredential) throws Exception {
        if (clientCredential == null || clientCredential.certificateDerCopy() == null) {
            return null;
        }
        byte[] certDer = clientCredential.certificateDerCopy();
        byte[] keyDer = clientCredential.privateKeyPkcs8Copy();
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certDer));
        PrivateKey key;
        try {
            key = KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(keyDer));
        } catch (Exception ignored) {
            key = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyDer));
        }
        KeyStore store = KeyStore.getInstance(KeyStore.getDefaultType());
        store.load(null);
        store.setKeyEntry("client", key, new char[0], new X509Certificate[]{cert});
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(store, new char[0]);
        return kmf.getKeyManagers();
    }

    public record PreparedSsl(SSLContext sslContext, CapturingTrustManager capturingTrustManager) {
    }

    public static final class CapturingTrustManager implements X509TrustManager {
        private final X509TrustManager delegate;
        private final AtomicReference<X509Certificate[]> chain = new AtomicReference<>();

        private CapturingTrustManager(X509TrustManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws java.security.cert.CertificateException {
            delegate.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws java.security.cert.CertificateException {
            try {
                delegate.checkServerTrusted(chain, authType);
                this.chain.set(chain);
            } catch (java.security.cert.CertificateException ex) {
                throw new java.security.cert.CertificateException("TLS trust failed", ex);
            } catch (RuntimeException ex) {
                throw new java.security.cert.CertificateException("TLS trust failed", ex);
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return delegate.getAcceptedIssuers();
        }

        public X509Certificate[] capturedChain() {
            return chain.get();
        }
    }
}
