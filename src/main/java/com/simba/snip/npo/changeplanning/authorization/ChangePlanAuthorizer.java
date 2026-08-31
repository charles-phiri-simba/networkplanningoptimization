package com.simba.snip.npo.changeplanning.authorization;

import com.simba.snip.npo.changeplanning.ChangePlanException;
import com.simba.snip.npo.changeplanning.model.ChangePlanFailureCode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class ChangePlanAuthorizer {

    public static final String PERMISSION_VIEW = "VIEW_NETWORK_CHANGE_PLAN";
    public static final String PERMISSION_CREATE = "CREATE_NETWORK_CHANGE_PLAN";
    public static final String PERMISSION_REVIEW = "REVIEW_NETWORK_CHANGE_PLAN";
    public static final String PERMISSION_AUTHORIZE = "AUTHORIZE_NETWORK_CHANGE_PLAN";
    public static final String PERMISSION_CANCEL = "CANCEL_NETWORK_CHANGE_PLAN";
    public static final String HEADER = "X-SNIP-CHANGE-PLAN-PERMISSION";
    private static final String ATTR = "snip.changePlanPermission";

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
        require(PERMISSION_VIEW, ChangePlanFailureCode.PLAN_CREATION_FORBIDDEN);
    }

    public void requireCreate() {
        require(PERMISSION_CREATE, ChangePlanFailureCode.PLAN_CREATION_FORBIDDEN);
    }

    public void requireReview() {
        require(PERMISSION_REVIEW, ChangePlanFailureCode.PLAN_REVIEW_FORBIDDEN);
    }

    public void requireViewOrReview() {
        String permission = current();
        if (PERMISSION_VIEW.equals(permission) || PERMISSION_REVIEW.equals(permission)) {
            return;
        }
        throw new ChangePlanException(
                ChangePlanFailureCode.PLAN_REVIEW_FORBIDDEN,
                PERMISSION_VIEW + " or " + PERMISSION_REVIEW + " is required"
        );
    }

    public void requireAuthorize() {
        require(PERMISSION_AUTHORIZE, ChangePlanFailureCode.PLAN_AUTHORIZATION_FORBIDDEN);
    }

    public void requireCancel() {
        require(PERMISSION_CANCEL, ChangePlanFailureCode.PLAN_CANCELLATION_FORBIDDEN);
    }

    public void requireReadiness() {
        require(PERMISSION_AUTHORIZE, ChangePlanFailureCode.PLAN_READINESS_FORBIDDEN);
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

    private void require(String permission, ChangePlanFailureCode failureCode) {
        if (!permission.equals(current())) {
            throw new ChangePlanException(failureCode, permission + " is required");
        }
    }
}
