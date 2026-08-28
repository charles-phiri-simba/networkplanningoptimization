package com.simba.snip.npo.integration.enm;

import com.simba.snip.npo.integration.ImportFailureCode;
import com.simba.snip.npo.integration.security.ConnectorSecurityException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class VendorImportAuthorizer {

    public static final String PERMISSION = "TRIGGER_VENDOR_IMPORT";
    public static final String HEADER = "X-SNIP-VENDOR-IMPORT-PERMISSION";
    private static final String ATTR = "snip.vendorImportPermission";

    private final ThreadLocal<String> override = new ThreadLocal<>();

    public void runWith(String permission, Runnable action) {
        String previous = override.get();
        override.set(permission);
        try {
            action.run();
        } finally {
            if (previous == null) {
                override.remove();
            } else {
                override.set(previous);
            }
        }
    }

    public <T> T callWith(String permission, java.util.concurrent.Callable<T> action) {
        String previous = override.get();
        override.set(permission);
        try {
            return action.call();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        } finally {
            if (previous == null) {
                override.remove();
            } else {
                override.set(previous);
            }
        }
    }

    public void requireTrigger() {
        if (!PERMISSION.equals(current())) {
            throw new ConnectorSecurityException(
                    ImportFailureCode.CONNECTOR_AUTHORIZATION_DENIED,
                    "TRIGGER_VENDOR_IMPORT is required"
            );
        }
    }

    public String current() {
        String local = override.get();
        if (local != null) {
            return local;
        }
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object value = attributes.getAttribute(ATTR, RequestAttributes.SCOPE_REQUEST);
        return value == null ? null : value.toString();
    }

    public void bindRequestPermission(String permission) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.setAttribute(ATTR, permission, RequestAttributes.SCOPE_REQUEST);
        } else {
            override.set(permission);
        }
    }
}
