package com.simba.snip.npo.productioncampaign.security;

import com.simba.snip.npo.productioncampaign.config.ProductionCampaignProperties;
import com.simba.snip.npo.productioncampaign.domain.ActorType;
import com.simba.snip.npo.productioncampaign.domain.AuthenticatedActor;
import com.simba.snip.npo.productioncampaign.domain.CampaignPermission;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * Header adapter is valid only when trusted ingress stripping/replacement is
 * explicitly configured. Raw caller-controlled headers never independently
 * establish human authority.
 */
@Component
public class HeaderAuthenticatedActorProvider implements AuthenticatedActorProvider {

    public static final String PERMISSION_HEADER = "X-SNIP-CAMPAIGN-PERMISSION";
    public static final String ACTOR_HEADER = "X-SNIP-CAMPAIGN-ACTOR-ID";
    public static final String SOURCE_HEADER = "X-SNIP-CAMPAIGN-AUTH-SOURCE";
    public static final String ACTOR_TYPE_HEADER = "X-SNIP-CAMPAIGN-ACTOR-TYPE";

    private static final String PERMISSION_ATTR = "snip.campaignPermission";
    private static final String ACTOR_ATTR = "snip.campaignActorId";
    private static final String SOURCE_ATTR = "snip.campaignAuthSource";
    private static final String TYPE_ATTR = "snip.campaignActorType";

    private final ProductionCampaignProperties properties;
    private final ThreadLocal<BoundHeaders> override = new ThreadLocal<>();

    public HeaderAuthenticatedActorProvider(ProductionCampaignProperties properties) {
        this.properties = properties;
    }

    public void bindRequest(String permission, String actorId, String source, String actorType) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.setAttribute(PERMISSION_ATTR, permission, RequestAttributes.SCOPE_REQUEST);
            attributes.setAttribute(ACTOR_ATTR, actorId, RequestAttributes.SCOPE_REQUEST);
            attributes.setAttribute(SOURCE_ATTR, source, RequestAttributes.SCOPE_REQUEST);
            attributes.setAttribute(TYPE_ATTR, actorType, RequestAttributes.SCOPE_REQUEST);
        } else {
            override.set(new BoundHeaders(permission, actorId, source, actorType));
        }
    }

    public void clear() {
        override.remove();
    }

    @Override
    public AuthenticatedActor requireHuman(CampaignPermission required) {
        if (!properties.isTrustedIngressGuarantee()) {
            throw new CampaignException(
                    ProductionReasonCode.UNTRUSTED_ACTOR_SOURCE,
                    "trusted ingress guarantee is not configured"
            );
        }
        String source = header(SOURCE_ATTR, BoundHeaders::source);
        if (source == null || source.isBlank()
                || properties.getTrustedAuthenticationSources().stream()
                .noneMatch(allowed -> allowed.equals(source))) {
            throw new CampaignException(
                    ProductionReasonCode.UNTRUSTED_ACTOR_SOURCE,
                    "authentication source is absent, unknown, or unconfigured"
            );
        }
        String actorId = header(ACTOR_ATTR, BoundHeaders::actorId);
        if (actorId == null || actorId.isBlank()) {
            throw new CampaignException(
                    ProductionReasonCode.UNAUTHENTICATED_HUMAN_ACTOR,
                    "human actor identity is required"
            );
        }
        ActorType actorType = parseType(header(TYPE_ATTR, BoundHeaders::actorType));
        if (actorType != ActorType.HUMAN) {
            throw new CampaignException(
                    ProductionReasonCode.UNAUTHENTICATED_HUMAN_ACTOR,
                    "Agent/MCP/service/scheduler/event identities cannot satisfy human campaign authority"
            );
        }
        CampaignPermission permission = parsePermission(header(PERMISSION_ATTR, BoundHeaders::permission));
        if (permission != required) {
            throw new CampaignException(
                    ProductionReasonCode.PRODUCTION_UNAUTHORIZED,
                    "required campaign authority is not present"
            );
        }
        Set<CampaignPermission> authorities = EnumSet.of(permission);
        return new AuthenticatedActor(actorId, ActorType.HUMAN, authorities, source, true);
    }

    private String header(String attr, java.util.function.Function<BoundHeaders, String> getter) {
        BoundHeaders local = override.get();
        if (local != null) {
            return getter.apply(local);
        }
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object value = attributes.getAttribute(attr, RequestAttributes.SCOPE_REQUEST);
        return value == null ? null : value.toString();
    }

    private static ActorType parseType(String raw) {
        if (raw == null || raw.isBlank()) {
            return ActorType.UNKNOWN;
        }
        try {
            return ActorType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return ActorType.UNKNOWN;
        }
    }

    private static CampaignPermission parsePermission(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new CampaignException(
                    ProductionReasonCode.PRODUCTION_UNAUTHORIZED,
                    "campaign permission is required"
            );
        }
        try {
            return CampaignPermission.valueOf(raw.trim());
        } catch (IllegalArgumentException ex) {
            throw new CampaignException(
                    ProductionReasonCode.PRODUCTION_UNAUTHORIZED,
                    "unknown campaign permission"
            );
        }
    }

    private record BoundHeaders(String permission, String actorId, String source, String actorType) {
    }
}
