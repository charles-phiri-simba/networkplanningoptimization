package com.simba.snip.npo.productionchange.security;

import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.domain.ProductionChangePermission;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class ProductionChangeAuthorizer {

    public static final String HEADER = "X-SNIP-PRODUCTION-CHANGE-PERMISSION";
    public static final String ACTOR_HEADER = "X-SNIP-ACTOR-PRINCIPAL-ID";
    private static final String PERMISSION_ATTR = "snip.productionChangePermission";
    private static final String ACTOR_ATTR = "snip.productionChangeActorPrincipalId";

    private final ThreadLocal<String> permissionOverride = new ThreadLocal<>();
    private final ThreadLocal<String> actorOverride = new ThreadLocal<>();

    public void bindRequest(String permission, String actorPrincipalId) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.setAttribute(PERMISSION_ATTR, permission, RequestAttributes.SCOPE_REQUEST);
            attributes.setAttribute(ACTOR_ATTR, actorPrincipalId, RequestAttributes.SCOPE_REQUEST);
        } else {
            permissionOverride.set(permission);
            actorOverride.set(actorPrincipalId);
        }
    }

    public String currentPermission() {
        String local = permissionOverride.get();
        if (local != null) {
            return local;
        }
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object value = attributes.getAttribute(PERMISSION_ATTR, RequestAttributes.SCOPE_REQUEST);
        return value == null ? null : value.toString();
    }

    public ActorPrincipal requireActor() {
        String actorId = currentActorPrincipalId();
        if (actorId == null || actorId.isBlank()) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_UNAUTHORIZED,
                    ACTOR_HEADER + " is required; display names are not authoritative"
            );
        }
        return ActorPrincipal.of(actorId);
    }

    public String currentActorPrincipalId() {
        String local = actorOverride.get();
        if (local != null) {
            return local;
        }
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object value = attributes.getAttribute(ACTOR_ATTR, RequestAttributes.SCOPE_REQUEST);
        return value == null ? null : value.toString();
    }

    public void requireView() {
        require(ProductionChangePermission.VIEW_PRODUCTION_CHANGE);
    }

    public void requireRequest() {
        require(ProductionChangePermission.REQUEST_PRODUCTION_CHANGE);
    }

    public void requireReview() {
        require(ProductionChangePermission.REVIEW_PRODUCTION_CHANGE);
    }

    public void requireAuthorize() {
        require(ProductionChangePermission.AUTHORIZE_PRODUCTION_CHANGE);
    }

    public void requireExecute() {
        require(ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE);
    }

    public void requireAdministerTarget() {
        require(ProductionChangePermission.ADMINISTER_PRODUCTION_TARGET);
    }

    public void requireRollbackRequest() {
        require(ProductionChangePermission.REQUEST_PRODUCTION_ROLLBACK);
    }

    public void requireRollbackReview() {
        require(ProductionChangePermission.REVIEW_PRODUCTION_ROLLBACK);
    }

    public void requireRollbackAuthorize() {
        require(ProductionChangePermission.AUTHORIZE_PRODUCTION_ROLLBACK);
    }

    public void requireRollbackExecute() {
        require(ProductionChangePermission.EXECUTE_PRODUCTION_ROLLBACK);
    }

    public void requireViewOrEvidence() {
        String permission = currentPermission();
        if (ProductionChangePermission.VIEW_PRODUCTION_CHANGE.equals(permission)
                || ProductionChangePermission.EXECUTE_PRODUCTION_CHANGE.equals(permission)
                || ProductionChangePermission.AUTHORIZE_PRODUCTION_CHANGE.equals(permission)
                || ProductionChangePermission.REVIEW_PRODUCTION_CHANGE.equals(permission)
                || ProductionChangePermission.REQUEST_PRODUCTION_CHANGE.equals(permission)
                || ProductionChangePermission.ADMINISTER_PRODUCTION_TARGET.equals(permission)
                || ProductionChangePermission.REQUEST_PRODUCTION_ROLLBACK.equals(permission)
                || ProductionChangePermission.REVIEW_PRODUCTION_ROLLBACK.equals(permission)
                || ProductionChangePermission.AUTHORIZE_PRODUCTION_ROLLBACK.equals(permission)
                || ProductionChangePermission.EXECUTE_PRODUCTION_ROLLBACK.equals(permission)) {
            return;
        }
        throw new ProductionChangeException(
                ProductionReasonCode.PRODUCTION_UNAUTHORIZED,
                ProductionChangePermission.VIEW_PRODUCTION_CHANGE + " is required"
        );
    }

    public void runWith(String permission, String actorPrincipalId, Runnable action) {
        String previousPermission = permissionOverride.get();
        String previousActor = actorOverride.get();
        permissionOverride.set(permission);
        actorOverride.set(actorPrincipalId);
        try {
            action.run();
        } finally {
            restore(permissionOverride, previousPermission);
            restore(actorOverride, previousActor);
        }
    }

    private void require(String permission) {
        if (!permission.equals(currentPermission())) {
            throw new ProductionChangeException(
                    ProductionReasonCode.PRODUCTION_UNAUTHORIZED,
                    permission + " is required"
            );
        }
    }

    private void restore(ThreadLocal<String> slot, String previous) {
        if (previous == null) {
            slot.remove();
        } else {
            slot.set(previous);
        }
    }
}
