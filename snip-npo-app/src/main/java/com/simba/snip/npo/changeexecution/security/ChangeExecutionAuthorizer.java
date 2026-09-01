package com.simba.snip.npo.changeexecution.security;

import com.simba.snip.npo.changeexecution.domain.ExecutionFailureCode;
import com.simba.snip.npo.changeexecution.exception.ChangeExecutionException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class ChangeExecutionAuthorizer {

    public static final String PERMISSION_VIEW = "VIEW_NETWORK_CHANGE_EXECUTION";
    public static final String PERMISSION_REQUEST = "REQUEST_NETWORK_CHANGE_EXECUTION";
    public static final String PERMISSION_REVIEW = "REVIEW_NETWORK_CHANGE_EXECUTION";
    public static final String PERMISSION_AUTHORIZE = "AUTHORIZE_NETWORK_CHANGE_EXECUTION";
    public static final String PERMISSION_CANCEL = "CANCEL_NETWORK_CHANGE_EXECUTION";
    public static final String PERMISSION_VIEW_EVIDENCE = "VIEW_NETWORK_CHANGE_EXECUTION_EVIDENCE";
    public static final String PERMISSION_ROLLBACK_REQUEST = "REQUEST_NETWORK_CHANGE_ROLLBACK";
    public static final String PERMISSION_ROLLBACK_REVIEW = "REVIEW_NETWORK_CHANGE_ROLLBACK";
    public static final String PERMISSION_ROLLBACK_AUTHORIZE = "AUTHORIZE_NETWORK_CHANGE_ROLLBACK";
    public static final String HEADER = "X-SNIP-CHANGE-EXECUTION-PERMISSION";
    private static final String ATTR = "snip.changeExecutionPermission";

    private final ThreadLocal<String> override = new ThreadLocal<>();

    public void bindRequestPermission(String permission) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.setAttribute(ATTR, permission, RequestAttributes.SCOPE_REQUEST);
        } else {
            override.set(permission);
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

    public void requireView() {
        require(PERMISSION_VIEW, ExecutionFailureCode.EXECUTION_VIEW_FORBIDDEN);
    }

    public void requireRequest() {
        require(PERMISSION_REQUEST, ExecutionFailureCode.EXECUTION_REVIEW_FORBIDDEN);
    }

    public void requireReview() {
        require(PERMISSION_REVIEW, ExecutionFailureCode.EXECUTION_REVIEW_FORBIDDEN);
    }

    public void requireAuthorize() {
        require(PERMISSION_AUTHORIZE, ExecutionFailureCode.EXECUTION_AUTHORIZATION_FORBIDDEN);
    }

    public void requireCancel() {
        require(PERMISSION_CANCEL, ExecutionFailureCode.EXECUTION_CANCEL_FORBIDDEN);
    }

    public void requireViewOrEvidence() {
        String permission = current();
        if (PERMISSION_VIEW.equals(permission) || PERMISSION_VIEW_EVIDENCE.equals(permission)) {
            return;
        }
        throw new ChangeExecutionException(
                ExecutionFailureCode.EXECUTION_VIEW_FORBIDDEN,
                PERMISSION_VIEW + " or " + PERMISSION_VIEW_EVIDENCE + " is required"
        );
    }

    public void requireRollbackRequest() {
        require(PERMISSION_ROLLBACK_REQUEST, ExecutionFailureCode.EXECUTION_REVIEW_FORBIDDEN);
    }

    public void requireRollbackReview() {
        require(PERMISSION_ROLLBACK_REVIEW, ExecutionFailureCode.EXECUTION_REVIEW_FORBIDDEN);
    }

    public void requireRollbackAuthorize() {
        require(PERMISSION_ROLLBACK_AUTHORIZE, ExecutionFailureCode.EXECUTION_AUTHORIZATION_FORBIDDEN);
    }

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

    private void require(String permission, ExecutionFailureCode failureCode) {
        if (!permission.equals(current())) {
            throw new ChangeExecutionException(failureCode, permission + " is required");
        }
    }
}
