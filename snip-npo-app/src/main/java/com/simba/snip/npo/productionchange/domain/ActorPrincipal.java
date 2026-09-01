package com.simba.snip.npo.productionchange.domain;

/**
 * Authoritative SoD identity. Display names are never compared.
 */
public record ActorPrincipal(String actorPrincipalId, String displayName) {

    public ActorPrincipal {
        if (actorPrincipalId == null || actorPrincipalId.isBlank()) {
            throw new IllegalArgumentException("actorPrincipalId is required");
        }
        actorPrincipalId = actorPrincipalId.strip();
        if (displayName != null && displayName.isBlank()) {
            displayName = null;
        }
    }

    public static ActorPrincipal of(String actorPrincipalId) {
        return new ActorPrincipal(actorPrincipalId, null);
    }
}
