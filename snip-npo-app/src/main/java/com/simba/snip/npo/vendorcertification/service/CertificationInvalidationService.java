package com.simba.snip.npo.vendorcertification.service;

import com.simba.snip.npo.productionchange.domain.ActorPrincipal;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkChangeEntity;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.protocol.Sha256Hex;
import com.simba.snip.npo.productionchange.repository.ProductionNetworkChangeRepository;
import com.simba.snip.npo.productionchange.service.ProductionAuthorizationService;
import com.simba.snip.npo.productionchange.service.ProductionExecutionGrantService;
import com.simba.snip.npo.vendorcertification.audit.Phase17CertificationAuditService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CertificationInvalidationService {

    public enum TriggerType {
        INTERFACE_REVOKED,
        INTERFACE_SUPERSEDED,
        DOCUMENTATION_WITHDRAWN,
        DOCUMENTATION_SUPERSEDED,
        APPROVAL_REVOKED,
        TRANSPORT_IMPLEMENTATION_CHANGED,
        ARTIFACT_DIGEST_CHANGED,
        ENDPOINT_PROFILE_CHANGED,
        NETWORK_PROFILE_CHANGED,
        TLS_PROFILE_CHANGED,
        SECURITY_PROFILE_CHANGED,
        CREDENTIAL_PROFILE_CHANGED,
        CAPABILITY_PROFILE_CHANGED,
        VENDOR_VERSION_MISMATCH,
        TARGET_ONBOARDING_CHANGED,
        TARGET_SUSPENDED,
        CERTIFICATION_EXPIRED,
        CERTIFICATION_REVOKED,
        PHASE16_L4_AUTHORIZATION_REVOKED,
        KILL_SWITCH_DISABLED
    }

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(ZoneOffset.UTC);

    private final NamedParameterJdbcTemplate jdbc;
    private final ProductionAuthorizationService authorizationService;
    private final ProductionExecutionGrantService grantService;
    private final ProductionNetworkChangeRepository changeRepository;
    private final Phase17CertificationAuditService auditService;
    private final ObjectProvider<InvalidationTransactionHook> hookProvider;
    private final Clock clock;

    public CertificationInvalidationService(
            NamedParameterJdbcTemplate jdbc,
            ProductionAuthorizationService authorizationService,
            ProductionExecutionGrantService grantService,
            ProductionNetworkChangeRepository changeRepository,
            Phase17CertificationAuditService auditService,
            ObjectProvider<InvalidationTransactionHook> hookProvider,
            Clock clock
    ) {
        this.jdbc = jdbc;
        this.authorizationService = authorizationService;
        this.grantService = grantService;
        this.changeRepository = changeRepository;
        this.auditService = auditService;
        this.hookProvider = hookProvider;
        this.clock = clock;
    }

    @Transactional
    public InvalidationResult invalidateExpired(UUID certificationId, String productionTargetId, Instant expiry) {
        return invalidate(new InvalidationCommand(
                TriggerType.CERTIFICATION_EXPIRED,
                "transport_certification",
                certificationId.toString(),
                certificationId,
                "EXPIRED",
                expiry,
                productionTargetId,
                ActorPrincipal.of(Phase17CertificationExpiryScheduler.SYSTEM_EXPIRY_ACTOR)
        ));
    }

    @Transactional
    public InvalidationResult invalidate(InvalidationCommand command) {
        ActorPrincipal actor = command.actor();
        Instant effectiveAt = command.effectiveAt() == null ? clock.instant() : command.effectiveAt();
        String key = idempotencyKey(
                command.triggerType().name(),
                command.sourceLogicalId(),
                command.sourceVersionId(),
                command.newStatus(),
                effectiveAt
        );
        UUID eventId = UUID.randomUUID();
        try {
            jdbc.update(
                    "INSERT INTO phase17_invalidation_event ("
                            + "invalidation_event_id, idempotency_key, trigger_type, source_table, source_logical_id, "
                            + "source_version_id, new_status, effective_at, processed_at, actor_principal_id) "
                            + "VALUES (:id,:key,:trigger,:table,:logical,:version,:status,:effective,:processed,:actor)",
                    new MapSqlParameterSource()
                            .addValue("id", eventId)
                            .addValue("key", key)
                            .addValue("trigger", command.triggerType().name())
                            .addValue("table", command.sourceTable())
                            .addValue("logical", command.sourceLogicalId())
                            .addValue("version", command.sourceVersionId())
                            .addValue("status", command.newStatus())
                            .addValue("effective", Timestamp.from(effectiveAt))
                            .addValue("processed", Timestamp.from(clock.instant()))
                            .addValue("actor", actor.actorPrincipalId())
            );
        } catch (DataIntegrityViolationException duplicate) {
            return InvalidationResult.replay();
        }

        AffectedGraph graph = resolveAffected(command);
        lockAffected(command, graph);
        InvalidationTransactionHook hook = hookProvider.getIfAvailable();
        if (hook != null) {
            hook.afterLocks();
        }

        if (command.triggerType() != TriggerType.KILL_SWITCH_DISABLED
                && command.triggerType() != TriggerType.PHASE16_L4_AUTHORIZATION_REVOKED) {
            applyPhase17Writes(command, graph);
        }

        boolean phase16Effects = command.triggerType() != TriggerType.KILL_SWITCH_DISABLED
                && command.productionTargetId() != null
                && (graph.currentTargetCertificationAffected()
                || command.triggerType() == TriggerType.PHASE16_L4_AUTHORIZATION_REVOKED
                || command.triggerType() == TriggerType.TARGET_SUSPENDED
                || command.triggerType() == TriggerType.CERTIFICATION_REVOKED
                || command.triggerType() == TriggerType.CERTIFICATION_EXPIRED
                || command.triggerType() == TriggerType.TARGET_ONBOARDING_CHANGED);
        if (phase16Effects) {
            for (ProductionNetworkChangeEntity change :
                    changeRepository.findByProductionTargetIdOrderByCreatedAtDesc(command.productionTargetId())) {
                authorizationService.markStale(change, actor, ProductionReasonCode.PRODUCTION_AUTHORIZATION_STALE);
            }
        }

        appendAudit(command, graph, actor);
        if (hook != null) {
            hook.afterRequiredWrites();
        }

        jdbc.update(
                "INSERT INTO phase17_invalidation_outbox (outbox_id, invalidation_event_id, payload_canonical, created_at) "
                        + "VALUES (:id,:event,:payload,:created)",
                new MapSqlParameterSource()
                        .addValue("id", UUID.randomUUID())
                        .addValue("event", eventId)
                        .addValue("payload", "{\"cache\":\"invalidate\"}")
                        .addValue("created", Timestamp.from(clock.instant()))
        );
        return InvalidationResult.success();
    }

    private AffectedGraph resolveAffected(InvalidationCommand command) {
        String targetId = command.productionTargetId();
        TriggerType trigger = command.triggerType();
        List<UUID> interfaceVersionIds = List.of();
        List<UUID> approvalIds = List.of();
        List<UUID> profileVersionIds = List.of();
        List<UUID> certIds = List.of();
        List<UUID> bundleIds = List.of();
        List<UUID> onboardingIds = List.of();
        List<UUID> targetCertIds = List.of();

        if (trigger == TriggerType.KILL_SWITCH_DISABLED) {
            return AffectedGraph.empty();
        }

        UUID logicalUuid = parseUuid(command.sourceLogicalId());
        UUID versionId = command.sourceVersionId();

        if (trigger == TriggerType.INTERFACE_REVOKED
                || trigger == TriggerType.INTERFACE_SUPERSEDED
                || trigger == TriggerType.DOCUMENTATION_WITHDRAWN
                || trigger == TriggerType.DOCUMENTATION_SUPERSEDED) {
            if (logicalUuid != null) {
                interfaceVersionIds = queryUuids(
                        "SELECT interface_definition_version_id FROM vendor_interface_definition "
                                + "WHERE interface_definition_id = :logical "
                                + "ORDER BY interface_definition_version_id",
                        new MapSqlParameterSource("logical", logicalUuid));
                bundleIds = queryUuids(
                        "SELECT tcb.bundle_version_id FROM transport_certification_bundle tcb "
                                + "WHERE tcb.interface_definition_version_id IN (:ids) "
                                + targetBundleFilter(targetId)
                                + " ORDER BY tcb.bundle_version_id",
                        idsParams(interfaceVersionIds, targetId));
            }
        } else if (trigger == TriggerType.APPROVAL_REVOKED && logicalUuid != null) {
            approvalIds = List.of(logicalUuid);
            bundleIds = queryUuids(
                    "SELECT tcb.bundle_version_id FROM transport_certification_bundle tcb "
                            + "WHERE tcb.interface_approval_id = :logical "
                            + targetBundleFilter(targetId)
                            + " ORDER BY tcb.bundle_version_id",
                    new MapSqlParameterSource("logical", logicalUuid).addValue("target", targetId));
        } else if (isProfileTrigger(trigger)) {
            UUID profileVersion = versionId != null ? versionId : logicalUuid;
            if (profileVersion != null) {
                profileVersionIds = List.of(profileVersion);
                bundleIds = queryBundlesForProfile(trigger, profileVersion, targetId);
            }
        } else if (trigger == TriggerType.CERTIFICATION_REVOKED || trigger == TriggerType.CERTIFICATION_EXPIRED) {
            if (logicalUuid != null) {
                certIds = queryUuids(
                        "SELECT transport_certification_id FROM transport_certification "
                                + "WHERE transport_certification_id = :id "
                                + "ORDER BY transport_certification_id",
                        new MapSqlParameterSource("id", logicalUuid));
                bundleIds = queryUuids(
                        "SELECT tcb.bundle_version_id FROM transport_certification_bundle tcb "
                                + "JOIN transport_certification trc "
                                + "  ON trc.transport_profile_version_id = tcb.transport_profile_version_id "
                                + "WHERE trc.transport_certification_id = :id "
                                + targetBundleFilter(targetId)
                                + " ORDER BY tcb.bundle_version_id",
                        new MapSqlParameterSource("id", logicalUuid).addValue("target", targetId));
            }
            if (bundleIds.isEmpty() && targetId != null) {
                bundleIds = queryUuids(
                        "SELECT bundle_version_id FROM production_target_certification "
                                + "WHERE production_target_id = :target ORDER BY bundle_version_id",
                        new MapSqlParameterSource("target", targetId));
            }
        } else if (trigger == TriggerType.TARGET_SUSPENDED
                || trigger == TriggerType.TARGET_ONBOARDING_CHANGED
                || trigger == TriggerType.VENDOR_VERSION_MISMATCH) {
            if (targetId != null) {
                bundleIds = queryUuids(
                        "SELECT bundle_version_id FROM production_target_certification "
                                + "WHERE production_target_id = :target ORDER BY bundle_version_id",
                        new MapSqlParameterSource("target", targetId));
            }
        } else if (trigger == TriggerType.PHASE16_L4_AUTHORIZATION_REVOKED) {
            return new AffectedGraph(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    targetId != null);
        }

        if (!bundleIds.isEmpty()) {
            certIds = merge(certIds, queryUuids(
                    "SELECT trc.transport_certification_id FROM transport_certification trc "
                            + "WHERE trc.transport_profile_version_id IN ("
                            + "SELECT transport_profile_version_id FROM transport_certification_bundle "
                            + "WHERE bundle_version_id IN (:ids)) "
                            + "ORDER BY trc.transport_certification_id",
                    new MapSqlParameterSource("ids", bundleIds)));
            profileVersionIds = merge(profileVersionIds, queryUuids(
                    "SELECT DISTINCT transport_profile_version_id FROM transport_certification_bundle "
                            + "WHERE bundle_version_id IN (:ids) ORDER BY transport_profile_version_id",
                    new MapSqlParameterSource("ids", bundleIds)));
            if (targetId != null) {
                targetCertIds = queryUuids(
                        "SELECT target_certification_id FROM production_target_certification "
                                + "WHERE production_target_id = :target AND bundle_version_id IN (:ids) "
                                + "ORDER BY target_certification_id",
                        new MapSqlParameterSource("target", targetId).addValue("ids", bundleIds));
                onboardingIds = queryUuids(
                        "SELECT DISTINCT pto.onboarding_id FROM production_target_onboarding pto "
                                + "LEFT JOIN production_target_onboarding_version ptov "
                                + "  ON ptov.onboarding_version_id = pto.onboarding_version_id "
                                + "WHERE pto.production_target_id = :target "
                                + "AND (ptov.bundle_version_id IN (:ids) OR pto.onboarding_version_id IS NULL) "
                                + "ORDER BY pto.onboarding_id",
                        new MapSqlParameterSource("target", targetId).addValue("ids", bundleIds));
            }
        } else if (targetId != null && (trigger == TriggerType.TARGET_SUSPENDED
                || trigger == TriggerType.TARGET_ONBOARDING_CHANGED)) {
            targetCertIds = queryUuids(
                    "SELECT target_certification_id FROM production_target_certification "
                            + "WHERE production_target_id = :target ORDER BY target_certification_id",
                    new MapSqlParameterSource("target", targetId));
            onboardingIds = queryUuids(
                    "SELECT onboarding_id FROM production_target_onboarding "
                            + "WHERE production_target_id = :target ORDER BY onboarding_id",
                    new MapSqlParameterSource("target", targetId));
        }

        boolean currentAffected = !targetCertIds.isEmpty() && targetId != null && !queryUuids(
                "SELECT target_certification_id FROM production_target_certification "
                        + "WHERE target_certification_id IN (:ids) AND status = 'CURRENT' "
                        + "ORDER BY target_certification_id",
                new MapSqlParameterSource("ids", targetCertIds)
        ).isEmpty();
        if (trigger == TriggerType.TARGET_SUSPENDED || trigger == TriggerType.CERTIFICATION_EXPIRED) {
            currentAffected = currentAffected || targetId != null;
        }
        return new AffectedGraph(
                interfaceVersionIds, approvalIds, profileVersionIds, certIds, bundleIds,
                onboardingIds, targetCertIds, currentAffected);
    }

    private List<UUID> queryBundlesForProfile(TriggerType trigger, UUID profileVersion, String targetId) {
        String column = switch (trigger) {
            case TRANSPORT_IMPLEMENTATION_CHANGED, ARTIFACT_DIGEST_CHANGED -> "transport_profile_version_id";
            case ENDPOINT_PROFILE_CHANGED -> "endpoint_profile_version_id";
            case NETWORK_PROFILE_CHANGED -> "network_policy_profile_version_id";
            case TLS_PROFILE_CHANGED -> "tls_profile_version_id";
            case SECURITY_PROFILE_CHANGED -> "security_cert_version_id";
            case CREDENTIAL_PROFILE_CHANGED -> "credential_profile_version_id";
            case CAPABILITY_PROFILE_CHANGED -> "capability_cert_version_id";
            default -> "transport_profile_version_id";
        };
        return queryUuids(
                "SELECT tcb.bundle_version_id FROM transport_certification_bundle tcb "
                        + "WHERE tcb." + column + " = :profile "
                        + targetBundleFilter(targetId)
                        + " ORDER BY tcb.bundle_version_id",
                new MapSqlParameterSource("profile", profileVersion).addValue("target", targetId));
    }

    private static String targetBundleFilter(String targetId) {
        if (targetId == null) {
            return "";
        }
        return " AND tcb.bundle_version_id IN ("
                + "SELECT bundle_version_id FROM production_target_certification WHERE production_target_id = :target "
                + "UNION SELECT bundle_version_id FROM production_target_onboarding_version WHERE production_target_id = :target)";
    }

    private static boolean isProfileTrigger(TriggerType trigger) {
        return trigger == TriggerType.TRANSPORT_IMPLEMENTATION_CHANGED
                || trigger == TriggerType.ARTIFACT_DIGEST_CHANGED
                || trigger == TriggerType.ENDPOINT_PROFILE_CHANGED
                || trigger == TriggerType.NETWORK_PROFILE_CHANGED
                || trigger == TriggerType.TLS_PROFILE_CHANGED
                || trigger == TriggerType.SECURITY_PROFILE_CHANGED
                || trigger == TriggerType.CREDENTIAL_PROFILE_CHANGED
                || trigger == TriggerType.CAPABILITY_PROFILE_CHANGED;
    }

    private void lockAffected(InvalidationCommand command, AffectedGraph graph) {
        lockUuids("SELECT interface_definition_version_id FROM vendor_interface_definition "
                + "WHERE interface_definition_version_id IN (:ids) "
                + "ORDER BY interface_definition_version_id FOR UPDATE", graph.interfaceVersionIds());
        lockUuids("SELECT approval_id FROM vendor_interface_approval WHERE approval_id IN (:ids) "
                + "ORDER BY approval_id FOR UPDATE", graph.approvalIds());
        lockUuids("SELECT transport_certification_id FROM transport_certification "
                + "WHERE transport_certification_id IN (:ids) ORDER BY transport_certification_id FOR UPDATE",
                graph.certIds());
        lockUuids("SELECT bundle_version_id FROM transport_certification_bundle "
                + "WHERE bundle_version_id IN (:ids) ORDER BY bundle_version_id FOR UPDATE", graph.bundleIds());
        lockUuids("SELECT onboarding_id FROM production_target_onboarding "
                + "WHERE onboarding_id IN (:ids) ORDER BY onboarding_id FOR UPDATE", graph.onboardingIds());
        lockUuids("SELECT target_certification_id FROM production_target_certification "
                + "WHERE target_certification_id IN (:ids) ORDER BY target_certification_id FOR UPDATE",
                graph.targetCertIds());

        String targetId = command.productionTargetId();
        if (targetId != null) {
            jdbc.query("SELECT production_change_id FROM production_network_change "
                            + "WHERE production_target_id = :target ORDER BY production_change_id FOR UPDATE",
                    new MapSqlParameterSource("target", targetId), rs -> null);
            jdbc.query("SELECT a.authorization_id FROM production_change_authorization a "
                            + "JOIN production_network_change c ON c.production_change_id = a.production_change_id "
                            + "WHERE c.production_target_id = :target ORDER BY a.authorization_id FOR UPDATE",
                    new MapSqlParameterSource("target", targetId), rs -> null);
            jdbc.query("SELECT grant_id FROM production_execution_grant "
                            + "WHERE target_id = :target AND status = 'ISSUED' ORDER BY grant_id FOR UPDATE",
                    new MapSqlParameterSource("target", targetId), rs -> null);
            jdbc.query("SELECT health_id FROM vendor_transport_health "
                            + "WHERE production_target_id = :target ORDER BY health_id FOR UPDATE",
                    new MapSqlParameterSource("target", targetId), rs -> null);
        }
        jdbc.query("SELECT invalidation_event_id FROM phase17_invalidation_event "
                        + "WHERE idempotency_key = :key ORDER BY invalidation_event_id FOR UPDATE",
                new MapSqlParameterSource("key", idempotencyKey(
                        command.triggerType().name(),
                        command.sourceLogicalId(),
                        command.sourceVersionId(),
                        command.newStatus(),
                        command.effectiveAt() == null ? clock.instant() : command.effectiveAt())),
                rs -> null);
    }

    private void applyPhase17Writes(InvalidationCommand command, AffectedGraph graph) {
        TriggerType trigger = command.triggerType();
        boolean rewriteCert = trigger != TriggerType.TARGET_SUSPENDED
                && trigger != TriggerType.TARGET_ONBOARDING_CHANGED;
        boolean rewriteBundle = trigger != TriggerType.TARGET_SUSPENDED;

        if (trigger == TriggerType.INTERFACE_REVOKED) {
            updateByLogicalUuid("UPDATE vendor_interface_definition SET status = 'REVOKED', updated_at = :now "
                    + "WHERE interface_definition_id = :logical AND status <> 'REVOKED'", command);
        } else if (trigger == TriggerType.INTERFACE_SUPERSEDED) {
            updateByLogicalUuid("UPDATE vendor_interface_definition SET status = 'SUPERSEDED', updated_at = :now "
                    + "WHERE interface_definition_id = :logical AND status NOT IN ('SUPERSEDED','REVOKED')", command);
        } else if (trigger == TriggerType.DOCUMENTATION_WITHDRAWN) {
            updateByLogicalUuid("UPDATE vendor_interface_definition SET documentation_status = 'WITHDRAWN', updated_at = :now "
                    + "WHERE interface_definition_id = :logical", command);
        } else if (trigger == TriggerType.DOCUMENTATION_SUPERSEDED) {
            updateByLogicalUuid("UPDATE vendor_interface_definition SET documentation_status = 'SUPERSEDED', updated_at = :now "
                    + "WHERE interface_definition_id = :logical", command);
        } else if (trigger == TriggerType.APPROVAL_REVOKED) {
            updateByLogicalUuid("UPDATE vendor_interface_approval SET approval_status = 'REVOKED', revoked_at = :now "
                    + "WHERE approval_id = :logical AND approval_status = 'APPROVED'", command);
        } else if (isProfileTrigger(trigger)) {
            UUID profile = command.sourceVersionId() != null ? command.sourceVersionId() : parseUuid(command.sourceLogicalId());
            if (profile != null) {
                String table = profileTable(trigger);
                jdbc.update("UPDATE " + table + " SET status = 'SUPERSEDED' WHERE "
                                + profilePk(trigger) + " = :id AND status = 'ACTIVE'",
                        new MapSqlParameterSource("id", profile));
            }
        } else if (trigger == TriggerType.VENDOR_VERSION_MISMATCH && command.sourceVersionId() != null) {
            jdbc.update("UPDATE vendor_version_compatibility SET status = 'SUSPENDED' "
                            + "WHERE compatibility_id = :id AND status = 'ACTIVE'",
                    new MapSqlParameterSource("id", command.sourceVersionId()));
        }

        String certState = switch (trigger) {
            case VENDOR_VERSION_MISMATCH -> "SUSPENDED";
            case CERTIFICATION_EXPIRED -> "EXPIRED";
            default -> "REVOKED";
        };
        if (rewriteCert && !graph.certIds().isEmpty()) {
            jdbc.update("UPDATE transport_certification SET state = :state, updated_at = :now "
                            + "WHERE transport_certification_id IN (:ids)",
                    new MapSqlParameterSource()
                            .addValue("state", certState)
                            .addValue("now", Timestamp.from(clock.instant()))
                            .addValue("ids", graph.certIds()));
        }

        String bundleStatus = switch (trigger) {
            case INTERFACE_SUPERSEDED, DOCUMENTATION_SUPERSEDED, TRANSPORT_IMPLEMENTATION_CHANGED,
                    ARTIFACT_DIGEST_CHANGED, ENDPOINT_PROFILE_CHANGED, NETWORK_PROFILE_CHANGED,
                    TLS_PROFILE_CHANGED, SECURITY_PROFILE_CHANGED, CREDENTIAL_PROFILE_CHANGED,
                    CAPABILITY_PROFILE_CHANGED, VENDOR_VERSION_MISMATCH, TARGET_ONBOARDING_CHANGED -> "INVALID";
            case CERTIFICATION_EXPIRED -> "EXPIRED";
            default -> "REVOKED";
        };
        if (rewriteBundle && !graph.bundleIds().isEmpty()) {
            jdbc.update("UPDATE transport_certification_bundle SET status = :status "
                            + "WHERE bundle_version_id IN (:ids)",
                    new MapSqlParameterSource()
                            .addValue("status", bundleStatus)
                            .addValue("ids", graph.bundleIds()));
        }

        if (command.productionTargetId() != null) {
            String targetStatus = switch (trigger) {
                case TARGET_SUSPENDED, VENDOR_VERSION_MISMATCH -> "SUSPENDED";
                case CERTIFICATION_EXPIRED -> "EXPIRED";
                case INTERFACE_SUPERSEDED, DOCUMENTATION_SUPERSEDED, TARGET_ONBOARDING_CHANGED,
                        TRANSPORT_IMPLEMENTATION_CHANGED, ARTIFACT_DIGEST_CHANGED,
                        ENDPOINT_PROFILE_CHANGED, NETWORK_PROFILE_CHANGED, TLS_PROFILE_CHANGED,
                        SECURITY_PROFILE_CHANGED, CREDENTIAL_PROFILE_CHANGED, CAPABILITY_PROFILE_CHANGED -> "INVALID";
                default -> "REVOKED";
            };
            if (!graph.targetCertIds().isEmpty()) {
                jdbc.update("UPDATE production_target_certification SET status = :status "
                                + "WHERE target_certification_id IN (:ids)",
                        new MapSqlParameterSource()
                                .addValue("status", targetStatus)
                                .addValue("ids", graph.targetCertIds()));
            }
            if (!graph.onboardingIds().isEmpty()
                    && (trigger == TriggerType.TARGET_SUSPENDED
                    || trigger == TriggerType.INTERFACE_REVOKED
                    || trigger == TriggerType.INTERFACE_SUPERSEDED
                    || trigger == TriggerType.CERTIFICATION_REVOKED
                    || trigger == TriggerType.APPROVAL_REVOKED
                    || trigger == TriggerType.DOCUMENTATION_WITHDRAWN
                    || trigger == TriggerType.DOCUMENTATION_SUPERSEDED
                    || trigger == TriggerType.TARGET_ONBOARDING_CHANGED)) {
                String onboardingStatus = switch (trigger) {
                    case TARGET_SUSPENDED -> "SUSPENDED";
                    case INTERFACE_SUPERSEDED, DOCUMENTATION_SUPERSEDED, TARGET_ONBOARDING_CHANGED -> "INVALID";
                    default -> "REVOKED";
                };
                jdbc.update("UPDATE production_target_onboarding SET status = :status, updated_at = :now "
                                + "WHERE onboarding_id IN (:ids)",
                        new MapSqlParameterSource()
                                .addValue("status", onboardingStatus)
                                .addValue("now", Timestamp.from(clock.instant()))
                                .addValue("ids", graph.onboardingIds()));
            }
            if (trigger != TriggerType.TARGET_ONBOARDING_CHANGED && !graph.profileVersionIds().isEmpty()) {
                String health = switch (trigger) {
                    case TLS_PROFILE_CHANGED, SECURITY_PROFILE_CHANGED, CREDENTIAL_PROFILE_CHANGED,
                            APPROVAL_REVOKED, INTERFACE_REVOKED, CERTIFICATION_REVOKED -> "SECURITY_FAILURE";
                    case VENDOR_VERSION_MISMATCH -> "VERSION_MISMATCH";
                    case CAPABILITY_PROFILE_CHANGED, ARTIFACT_DIGEST_CHANGED,
                            TRANSPORT_IMPLEMENTATION_CHANGED, INTERFACE_SUPERSEDED -> "CAPABILITY_MISMATCH";
                    case TARGET_SUSPENDED -> "SUSPENDED";
                    default -> "UNAVAILABLE";
                };
                jdbc.update("UPDATE vendor_transport_health SET health_state = :state, observed_at = :now, "
                                + "requires_human_reactivation = TRUE, source = 'POLICY' "
                                + "WHERE production_target_id = :target "
                                + "AND transport_profile_version_id IN (:ids)",
                        new MapSqlParameterSource()
                                .addValue("state", health)
                                .addValue("now", Timestamp.from(clock.instant()))
                                .addValue("target", command.productionTargetId())
                                .addValue("ids", graph.profileVersionIds()));
            } else if (trigger == TriggerType.TARGET_SUSPENDED && command.productionTargetId() != null) {
                jdbc.update("UPDATE vendor_transport_health SET health_state = 'SUSPENDED', observed_at = :now, "
                                + "requires_human_reactivation = TRUE, source = 'POLICY' "
                                + "WHERE production_target_id = :target",
                        new MapSqlParameterSource()
                                .addValue("now", Timestamp.from(clock.instant()))
                                .addValue("target", command.productionTargetId()));
            }
        }
    }

    private void appendAudit(InvalidationCommand command, AffectedGraph graph, ActorPrincipal actor) {
        String eventType = switch (command.triggerType()) {
            case INTERFACE_REVOKED, APPROVAL_REVOKED -> "INTERFACE_REVOKED";
            case INTERFACE_SUPERSEDED, DOCUMENTATION_SUPERSEDED -> "INTERFACE_SUPERSEDED";
            case DOCUMENTATION_WITHDRAWN -> "DOCUMENTATION_APPROVAL_REVOKED";
            case TRANSPORT_IMPLEMENTATION_CHANGED -> "PROFILE_VERSIONED";
            case ARTIFACT_DIGEST_CHANGED -> "ARTIFACT_MISMATCH";
            case ENDPOINT_PROFILE_CHANGED -> "ENDPOINT_CHANGED";
            case NETWORK_PROFILE_CHANGED -> "NETWORK_PROFILE_CHANGED";
            case TLS_PROFILE_CHANGED -> "TLS_PROFILE_CHANGED";
            case CREDENTIAL_PROFILE_CHANGED -> "CREDENTIAL_PROFILE_CHANGED";
            case SECURITY_PROFILE_CHANGED, CAPABILITY_PROFILE_CHANGED, CERTIFICATION_REVOKED -> "CERT_REVOKED";
            case VENDOR_VERSION_MISMATCH -> "VENDOR_VERSION_MISMATCH";
            case TARGET_ONBOARDING_CHANGED -> "ONBOARD_CREATED";
            case TARGET_SUSPENDED -> "TARGET_SUSPENDED";
            case CERTIFICATION_EXPIRED -> "CERT_EXPIRED";
            case PHASE16_L4_AUTHORIZATION_REVOKED, KILL_SWITCH_DISABLED -> "HEALTH_TRANSITION";
        };
        String subjectId = command.productionTargetId() != null
                ? command.productionTargetId()
                : command.sourceLogicalId();
        String version = command.sourceVersionId() == null ? "" : command.sourceVersionId().toString();
        auditService.append(
                "INVALIDATION",
                subjectId,
                version,
                eventType,
                actor.actorPrincipalId(),
                "{\"trigger\":\"" + command.triggerType().name() + "\",\"certs\":" + graph.certIds().size()
                        + ",\"bundles\":" + graph.bundleIds().size() + "}"
        );
    }

    private void updateByLogicalUuid(String sql, InvalidationCommand command) {
        UUID logical = parseUuid(command.sourceLogicalId());
        if (logical == null) {
            return;
        }
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("logical", logical)
                .addValue("now", Timestamp.from(clock.instant())));
    }

    private static String profileTable(TriggerType trigger) {
        return switch (trigger) {
            case TRANSPORT_IMPLEMENTATION_CHANGED, ARTIFACT_DIGEST_CHANGED -> "vendor_write_transport_profile";
            case ENDPOINT_PROFILE_CHANGED -> "production_endpoint_profile";
            case NETWORK_PROFILE_CHANGED -> "production_network_policy_profile";
            case TLS_PROFILE_CHANGED -> "production_tls_profile";
            case SECURITY_PROFILE_CHANGED -> "vendor_security_certification";
            case CREDENTIAL_PROFILE_CHANGED -> "production_credential_profile";
            case CAPABILITY_PROFILE_CHANGED -> "vendor_capability_certification";
            default -> "vendor_write_transport_profile";
        };
    }

    private static String profilePk(TriggerType trigger) {
        return switch (trigger) {
            case TRANSPORT_IMPLEMENTATION_CHANGED, ARTIFACT_DIGEST_CHANGED -> "transport_profile_version_id";
            case ENDPOINT_PROFILE_CHANGED -> "endpoint_profile_version_id";
            case NETWORK_PROFILE_CHANGED -> "network_policy_profile_version_id";
            case TLS_PROFILE_CHANGED -> "tls_profile_version_id";
            case SECURITY_PROFILE_CHANGED -> "security_cert_version_id";
            case CREDENTIAL_PROFILE_CHANGED -> "credential_profile_version_id";
            case CAPABILITY_PROFILE_CHANGED -> "capability_cert_version_id";
            default -> "transport_profile_version_id";
        };
    }

    private void lockUuids(String sql, List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        jdbc.query(sql, new MapSqlParameterSource("ids", ids), rs -> null);
    }

    private List<UUID> queryUuids(String sql, MapSqlParameterSource params) {
        if (sql.contains("IN (:ids)") && emptyIds(params)) {
            return List.of();
        }
        return jdbc.query(sql, params, (rs, row) -> (UUID) rs.getObject(1));
    }

    private static boolean emptyIds(MapSqlParameterSource params) {
        Object ids = params.getValue("ids");
        return ids instanceof List<?> list && list.isEmpty();
    }

    private MapSqlParameterSource idsParams(List<UUID> ids, String targetId) {
        return new MapSqlParameterSource("ids", ids).addValue("target", targetId);
    }

    private static List<UUID> merge(List<UUID> left, List<UUID> right) {
        Set<UUID> ordered = new LinkedHashSet<>(left);
        ordered.addAll(right);
        return new ArrayList<>(ordered);
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public static String idempotencyKey(
            String triggerType,
            String sourceLogicalId,
            UUID sourceVersionId,
            String newStatus,
            Instant effectiveAt
    ) {
        String version = sourceVersionId == null ? "" : sourceVersionId.toString();
        String canonical = triggerType + "|" + sourceLogicalId + "|" + version + "|" + newStatus + "|"
                + ISO.format(effectiveAt);
        return Sha256Hex.hashBytes(canonical.getBytes(StandardCharsets.UTF_8));
    }

    public record InvalidationCommand(
            TriggerType triggerType,
            String sourceTable,
            String sourceLogicalId,
            UUID sourceVersionId,
            String newStatus,
            Instant effectiveAt,
            String productionTargetId,
            ActorPrincipal actor
    ) {
    }

    public record InvalidationResult(boolean applied, boolean idempotentReplay) {
        public static InvalidationResult success() {
            return new InvalidationResult(true, false);
        }

        public static InvalidationResult replay() {
            return new InvalidationResult(false, true);
        }
    }

    private record AffectedGraph(
            List<UUID> interfaceVersionIds,
            List<UUID> approvalIds,
            List<UUID> profileVersionIds,
            List<UUID> certIds,
            List<UUID> bundleIds,
            List<UUID> onboardingIds,
            List<UUID> targetCertIds,
            boolean currentTargetCertificationAffected
    ) {
        static AffectedGraph empty() {
            return new AffectedGraph(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), false);
        }
    }
}
