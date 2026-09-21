package com.simba.snip.npo.productioncampaign.service;

import com.simba.snip.npo.productioncampaign.config.ProductionCampaignProperties;
import com.simba.snip.npo.productioncampaign.domain.AuthenticatedActor;
import com.simba.snip.npo.productioncampaign.domain.CampaignConstants;
import com.simba.snip.npo.productioncampaign.domain.CampaignPermission;
import com.simba.snip.npo.productioncampaign.entity.CampaignCohortEntity;
import com.simba.snip.npo.productioncampaign.entity.CampaignExecutionItemEntity;
import com.simba.snip.npo.productioncampaign.entity.CampaignRevisionEntity;
import com.simba.snip.npo.productioncampaign.entity.ProductionChangeCampaignEntity;
import com.simba.snip.npo.productioncampaign.exception.CampaignException;
import com.simba.snip.npo.productioncampaign.repository.CampaignCohortRepository;
import com.simba.snip.npo.productioncampaign.repository.CampaignExecutionItemRepository;
import com.simba.snip.npo.productioncampaign.repository.CampaignRevisionRepository;
import com.simba.snip.npo.productioncampaign.repository.ProductionChangeCampaignRepository;
import com.simba.snip.npo.productionchange.entity.ProductionNetworkTargetEntity;
import com.simba.snip.npo.productionchange.protocol.CanonicalJson;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionchange.repository.ProductionNetworkTargetRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class ProductionChangeCampaignService {

    public record PlannedItem(
            UUID phase14PlanId,
            String phase14PlanFingerprint,
            UUID phase15ExecutionId,
            String phase15ExecutionFingerprint,
            String cellId,
            BigDecimal expectedValue,
            BigDecimal desiredValue,
            BigDecimal rollbackValue,
            String unit,
            String objectType,
            String parameter
    ) {
    }

    private final ProductionCampaignProperties properties;
    private final ProductionChangeCampaignRepository campaignRepository;
    private final CampaignRevisionRepository revisionRepository;
    private final CampaignCohortRepository cohortRepository;
    private final CampaignExecutionItemRepository itemRepository;
    private final ProductionNetworkTargetRepository targetRepository;
    private final NamedParameterJdbcTemplate jdbc;
    private final CampaignAuditService auditService;
    private final CampaignGovernanceIdempotencyService idempotencyService;
    private final ForwardProgressionClosureService closureService;
    private final CampaignGenerationService generationService;
    private final CampaignCohortReleaseService releaseService;
    private final CampaignSuspensionService suspensionService;
    private final CampaignCompletionService completionService;
    private final CampaignLeaseService leaseService;
    private final CampaignObservationService observationService;

    public ProductionChangeCampaignService(
            ProductionCampaignProperties properties,
            ProductionChangeCampaignRepository campaignRepository,
            CampaignRevisionRepository revisionRepository,
            CampaignCohortRepository cohortRepository,
            CampaignExecutionItemRepository itemRepository,
            ProductionNetworkTargetRepository targetRepository,
            NamedParameterJdbcTemplate jdbc,
            CampaignAuditService auditService,
            CampaignGovernanceIdempotencyService idempotencyService,
            ForwardProgressionClosureService closureService,
            CampaignGenerationService generationService,
            CampaignCohortReleaseService releaseService,
            CampaignSuspensionService suspensionService,
            CampaignCompletionService completionService,
            CampaignLeaseService leaseService,
            CampaignObservationService observationService
    ) {
        this.properties = properties;
        this.campaignRepository = campaignRepository;
        this.revisionRepository = revisionRepository;
        this.cohortRepository = cohortRepository;
        this.itemRepository = itemRepository;
        this.targetRepository = targetRepository;
        this.jdbc = jdbc;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
        this.closureService = closureService;
        this.generationService = generationService;
        this.releaseService = releaseService;
        this.suspensionService = suspensionService;
        this.completionService = completionService;
        this.leaseService = leaseService;
        this.observationService = observationService;
    }

    @Transactional
    public ProductionChangeCampaignEntity create(
            String productionTargetId,
            String objective,
            String changeControlReference,
            List<PlannedItem> items,
            AuthenticatedActor creator,
            String idempotencyKey
    ) {
        properties.validate();
        if (items == null || items.isEmpty()) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_SCOPE_INCOMPLETE, "campaign items are required");
        }
        if (items.size() > properties.effectiveMaximumItems()) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_SIZE_LIMIT, "campaign exceeds effective maximum");
        }
        ProductionNetworkTargetEntity target = targetRepository.findById(productionTargetId)
                .orElseThrow(() -> new CampaignException(
                        ProductionReasonCode.PRODUCTION_TARGET_NOT_FOUND,
                        "production target not found"
                ));
        Set<String> cells = new java.util.HashSet<>();
        for (PlannedItem item : items) {
            TxPowerUnitValidator.requireCanonicalDbm(item.unit());
            TxPowerUnitValidator.requireCellTxPower(item.objectType(), item.parameter());
            String key = target.getTargetId() + "|" + item.cellId() + "|" + item.parameter();
            if (!cells.add(key)) {
                throw new CampaignException(
                        ProductionReasonCode.DUPLICATE_CAMPAIGN_LINEAGE,
                        "duplicate target/cell/parameter in campaign revision"
                );
            }
        }
        PlannedItem canary = items.get(0);
        if (items.stream().limit(1).count() != 1) {
            throw new CampaignException(ProductionReasonCode.CAMPAIGN_SCOPE_INCOMPLETE, "canary must be exactly one cell");
        }
        Instant now = Instant.now();
        UUID campaignId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        UUID canaryCohortId = UUID.randomUUID();

        ProductionChangeCampaignEntity campaign = new ProductionChangeCampaignEntity();
        campaign.setCampaignId(campaignId);
        campaign.setProductionTargetId(target.getTargetId());
        campaign.setVendor(target.getVendor());
        campaign.setPlatform(target.getPlatform());
        campaign.setEnvironment(target.getEnvironment());
        campaign.setNetworkDomain(target.getNetworkDomain());
        campaign.setObjective(objective);
        campaign.setChangeControlReference(changeControlReference);
        campaign.setState("DRAFT");
        campaign.setEnabled(true);
        campaign.setCreatorPrincipalId(creator.actorId());
        campaign.setCurrentRevisionNumber(1);
        campaign.setAbortState("NOT_ABORTED");
        campaign.setCreatedAt(now);
        campaign.setUpdatedAt(now);

        CampaignRevisionEntity revision = new CampaignRevisionEntity();
        revision.setRevisionId(revisionId);
        revision.setCampaignId(campaignId);
        revision.setRevisionNumber(1);
        revision.setAuthorizationGeneration(0);
        revision.setState("DRAFT");
        revision.setPolicyVersions(defaultPolicyVersions());
        revision.setWindows(defaultWindows());
        revision.setExternalTicketCurrentnessRequired(true);
        revision.setCreatedAt(now);
        revision.setUpdatedAt(now);

        List<CampaignExecutionItemEntity> persistedItems = new ArrayList<>();
        CampaignCohortEntity canaryCohort = cohort(canaryCohortId, campaignId, revisionId, 1, "CANARY", now);
        UUID standardCohortId = items.size() > 1 ? UUID.randomUUID() : null;
        CampaignCohortEntity standardCohort = standardCohortId == null
                ? null
                : cohort(standardCohortId, campaignId, revisionId, 2, "STANDARD", now);

        for (int i = 0; i < items.size(); i++) {
            PlannedItem planned = items.get(i);
            CampaignExecutionItemEntity entity = new CampaignExecutionItemEntity();
            entity.setItemId(UUID.randomUUID());
            entity.setCampaignId(campaignId);
            entity.setRevisionId(revisionId);
            entity.setCohortId(i == 0 ? canaryCohortId : standardCohortId);
            entity.setItemSequence(i + 1);
            entity.setPhase14PlanId(planned.phase14PlanId());
            entity.setPhase14PlanFingerprint(planned.phase14PlanFingerprint());
            entity.setPhase15ExecutionId(planned.phase15ExecutionId());
            entity.setPhase15ExecutionFingerprint(planned.phase15ExecutionFingerprint());
            entity.setProductionTargetId(target.getTargetId());
            entity.setObjectType(CampaignConstants.OBJECT_CELL);
            entity.setCellId(planned.cellId());
            entity.setParameter(CampaignConstants.PARAMETER_TX_POWER);
            entity.setExpectedValue(planned.expectedValue());
            entity.setDesiredValue(planned.desiredValue());
            entity.setRollbackValue(planned.rollbackValue());
            entity.setUnit(CampaignConstants.UNIT_DBM);
            entity.setState("PLANNED");
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            entity.setItemFingerprint(itemFingerprint(entity));
            persistedItems.add(entity);
        }
        List<CampaignCohortEntity> persistedCohorts = new ArrayList<>();
        persistedCohorts.add(canaryCohort);
        if (standardCohort != null) {
            persistedCohorts.add(standardCohort);
        }
        revision.setFingerprint(revisionFingerprint(campaign, revision, persistedItems, persistedCohorts));
        campaign.setFingerprint(revision.getFingerprint());

        campaignRepository.saveAndFlush(campaign);
        revisionRepository.saveAndFlush(revision);
        cohortRepository.saveAndFlush(canaryCohort);
        if (standardCohort != null) {
            cohortRepository.saveAndFlush(standardCohort);
        }
        itemRepository.saveAllAndFlush(persistedItems);
        bootstrapControlPlane(campaignId, revisionId, now);
        auditService.append(campaignId, "CAMPAIGN_CREATED", creator.actorId(), Map.of("state", "DRAFT"));
        idempotencyService.record(idempotencyKey, campaignId, "CREATE_CAMPAIGN", revision.getFingerprint());
        return campaign;
    }

    @Transactional
    public ProductionChangeCampaignEntity transition(
            UUID campaignId,
            String trigger,
            AuthenticatedActor actor,
            String idempotencyKey
    ) {
        if ("COMPLETE_CAMPAIGN".equals(trigger)
                || "EVALUATE_READY".equals(trigger)
                || "CANARY_HEALTH_AND_SAFETY_PASS".equals(trigger)
                || "DETERMINED_RECOVERY_REQUIRED".equals(trigger)
                || "UNRESOLVED_MUTATION_OUTCOME".equals(trigger)
                || "MATERIAL_INVALIDATION".equals(trigger)
                || "SAFETY_SUSPEND".equals(trigger)
                || "CANARY_ITEMS_ENTER_OBSERVING".equals(trigger)
                || "COHORT_ITEMS_ENTER_OBSERVING".equals(trigger)
                || "COHORT_HEALTH_AND_SAFETY_PASS".equals(trigger)
                || "CANARY_TERMINALLY_ACCOUNTED".equals(trigger)
                || "MORE_AUTHORIZED_COHORTS_REMAIN".equals(trigger)
                || "LAST_AUTHORIZED_COHORT_ACCOUNTED".equals(trigger)) {
            throw new CampaignException(
                    ProductionReasonCode.SYSTEM_TRANSITION_DENIED,
                    "SYSTEM campaign transitions are not externally invocable: " + trigger
            );
        }
        ProductionChangeCampaignEntity campaign = campaignRepository.lockById(campaignId)
                .orElseThrow(() -> new CampaignException(ProductionReasonCode.CAMPAIGN_NOT_FOUND, "campaign not found"));
        String from = campaign.getState();
        String to = nextState(from, trigger, campaign, actor);
        campaign.setState(to);
        campaign.setUpdatedAt(Instant.now());
        if ("AUTHORIZE_CAMPAIGN".equals(trigger)) {
            campaign.setAuthorizerPrincipalId(actor.actorId());
            CampaignRevisionEntity revision = revisionRepository
                    .findByCampaignIdAndRevisionNumber(campaignId, campaign.getCurrentRevisionNumber())
                    .orElseThrow();
            revision.setAuthorizationGeneration(revision.getAuthorizationGeneration() + 1);
            revision.setState("AUTHORIZED");
            revision.setUpdatedAt(Instant.now());
            revisionRepository.save(revision);
        }
        if ("RELEASE_CANARY".equals(trigger) || "RELEASE_STANDARD_COHORT".equals(trigger)) {
            closureService.assertOpen(campaignId);
            if (campaign.getAuthorizerPrincipalId() != null
                    && campaign.getAuthorizerPrincipalId().equals(actor.actorId())) {
                throw new CampaignException(
                        ProductionReasonCode.PRODUCTION_SOD_VIOLATION,
                        "campaign authorizer cannot release a cohort"
                );
            }
            CampaignRevisionEntity revision = revisionRepository
                    .findByCampaignIdAndRevisionNumber(campaignId, campaign.getCurrentRevisionNumber())
                    .orElseThrow();
            String type = "RELEASE_CANARY".equals(trigger) ? "CANARY" : "STANDARD";
            CampaignCohortEntity cohort = cohortRepository.findByRevisionIdOrderByCohortSequenceAsc(revision.getRevisionId())
                    .stream()
                    .filter(c -> type.equals(c.getCohortType()) && !"RELEASED".equals(c.getState()))
                    .findFirst()
                    .orElseThrow(() -> new CampaignException(
                            ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                            "no unreleased " + type + " cohort"
                    ));
            if ("CANARY".equals(type)) {
                long canaryItems = itemRepository.findByRevisionIdOrderByItemSequenceAsc(revision.getRevisionId())
                        .stream()
                        .filter(i -> cohort.getCohortId().equals(i.getCohortId()))
                        .count();
                if (canaryItems != 1) {
                    throw new CampaignException(
                            ProductionReasonCode.CAMPAIGN_SCOPE_INCOMPLETE,
                            "canary must be exactly one cell"
                    );
                }
            }
            long fence = leaseService.acquire(campaignId, actor.actorId());
            releaseService.release(campaignId, revision.getRevisionId(), cohort.getCohortId(), fence, actor);
        }
        if ("PAUSE_CAMPAIGN".equals(trigger) || "OPERATOR_PAUSE".equals(trigger)) {
            CampaignRevisionEntity revision = revisionRepository
                    .findByCampaignIdAndRevisionNumber(campaignId, campaign.getCurrentRevisionNumber())
                    .orElseThrow();
            suspensionService.operatorPause(campaignId, revision.getRevisionId(), from, actor);
        }
        if ("SAFETY_SUSPEND".equals(trigger)) {
            CampaignRevisionEntity revision = revisionRepository
                    .findByCampaignIdAndRevisionNumber(campaignId, campaign.getCurrentRevisionNumber())
                    .orElseThrow();
            suspensionService.safetySuspend(campaignId, revision.getRevisionId(), "SAFETY_SUSPEND", from);
        }
        if ("ABORT_CAMPAIGN".equals(trigger)) {
            campaign.setAbortState("ABORTED");
            generationService.invalidateControlGeneration(campaignId);
        }
        campaignRepository.save(campaign);
        auditService.append(campaignId, trigger, actor == null ? "SYSTEM" : actor.actorId(), Map.of("from", from, "to", to));
        idempotencyService.record(idempotencyKey, campaignId, trigger, campaign.getFingerprint());
        return campaign;
    }

    /**
     * Internally derived AUTHORIZED → READY. Not HTTP-invocable.
     */
    @Transactional
    public ProductionChangeCampaignEntity evaluateReady(UUID campaignId) {
        ProductionChangeCampaignEntity campaign = campaignRepository.lockById(campaignId)
                .orElseThrow(() -> new CampaignException(ProductionReasonCode.CAMPAIGN_NOT_FOUND, "campaign not found"));
        requireState(campaign.getState(), "AUTHORIZED");
        closureService.assertOpen(campaignId);
        return applyInternal(campaign, "EVALUATE_READY", "READY");
    }

    /**
     * Internally derived from authoritative observation HEALTHY+SAFE. Not HTTP-invocable.
     */
    @Transactional
    public ProductionChangeCampaignEntity applyCanaryHealthAndSafetyPass(UUID campaignId) {
        ProductionChangeCampaignEntity campaign = campaignRepository.lockById(campaignId)
                .orElseThrow(() -> new CampaignException(ProductionReasonCode.CAMPAIGN_NOT_FOUND, "campaign not found"));
        requireState(campaign.getState(), "CANARY_OBSERVING");
        observationService.assertCurrentHealthyAndSafe(campaignId);
        return applyInternal(campaign, "CANARY_HEALTH_AND_SAFETY_PASS", "CANARY_VERIFIED");
    }

    @Transactional
    public ProductionChangeCampaignEntity enterCanaryObserving(UUID campaignId) {
        ProductionChangeCampaignEntity campaign = campaignRepository.lockById(campaignId)
                .orElseThrow(() -> new CampaignException(ProductionReasonCode.CAMPAIGN_NOT_FOUND, "campaign not found"));
        requireState(campaign.getState(), "CANARY_ACTIVE");
        return applyInternal(campaign, "CANARY_ITEMS_ENTER_OBSERVING", "CANARY_OBSERVING");
    }

    @Transactional
    public ProductionChangeCampaignEntity applyMaterialInvalidation(UUID campaignId) {
        ProductionChangeCampaignEntity campaign = campaignRepository.lockById(campaignId)
                .orElseThrow(() -> new CampaignException(ProductionReasonCode.CAMPAIGN_NOT_FOUND, "campaign not found"));
        denyIfTerminal(campaign.getState());
        generationService.invalidateControlGeneration(campaignId);
        return applyInternal(campaign, "MATERIAL_INVALIDATION", "STALE");
    }

    /**
     * Produced only by authoritative send/outcome resolution. Not HTTP-invocable.
     */
    @Transactional
    public ProductionChangeCampaignEntity recordUnresolvedMutationOutcome(UUID campaignId) {
        ProductionChangeCampaignEntity campaign = campaignRepository.lockById(campaignId)
                .orElseThrow(() -> new CampaignException(ProductionReasonCode.CAMPAIGN_NOT_FOUND, "campaign not found"));
        if (!Set.of("CANARY_ACTIVE", "CANARY_OBSERVING", "COHORT_ACTIVE", "COHORT_OBSERVING", "FINAL_OBSERVATION")
                .contains(campaign.getState())) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                    "UNRESOLVED_MUTATION_OUTCOME is illegal from " + campaign.getState()
            );
        }
        return applyInternal(campaign, "UNRESOLVED_MUTATION_OUTCOME", "OUTCOME_UNRESOLVED");
    }

    /**
     * Completion is internally derived after frozen successful-forward evidence. Not HTTP-invocable.
     */
    @Transactional
    public ProductionChangeCampaignEntity completeCampaign(UUID campaignId) {
        ProductionChangeCampaignEntity campaign = campaignRepository.lockById(campaignId)
                .orElseThrow(() -> new CampaignException(ProductionReasonCode.CAMPAIGN_NOT_FOUND, "campaign not found"));
        requireState(campaign.getState(), "FINAL_OBSERVATION");
        completionService.complete(campaignId);
        return applyInternal(campaign, "COMPLETE_CAMPAIGN", "COMPLETED");
    }

    @Transactional
    public ProductionChangeCampaignEntity safetySuspend(UUID campaignId) {
        ProductionChangeCampaignEntity campaign = campaignRepository.lockById(campaignId)
                .orElseThrow(() -> new CampaignException(ProductionReasonCode.CAMPAIGN_NOT_FOUND, "campaign not found"));
        denyIfTerminal(campaign.getState());
        CampaignRevisionEntity revision = revisionRepository
                .findByCampaignIdAndRevisionNumber(campaignId, campaign.getCurrentRevisionNumber())
                .orElseThrow();
        String from = campaign.getState();
        suspensionService.safetySuspend(campaignId, revision.getRevisionId(), "SAFETY_SUSPEND", from);
        return applyInternal(campaign, "SAFETY_SUSPEND", "SUSPENDED");
    }

    private ProductionChangeCampaignEntity applyInternal(
            ProductionChangeCampaignEntity campaign, String trigger, String to
    ) {
        String from = campaign.getState();
        campaign.setState(to);
        campaign.setUpdatedAt(Instant.now());
        campaignRepository.save(campaign);
        auditService.append(campaign.getCampaignId(), trigger, "SYSTEM", Map.of("from", from, "to", to));
        return campaign;
    }

    public void assertProgressionObservation(CampaignObservationService.ObservationEvidence evidence) {
        observationService.assertProgressionAdmissible(evidence);
    }

    @Transactional(readOnly = true)
    public ProductionChangeCampaignEntity require(UUID campaignId) {
        return campaignRepository.findById(campaignId)
                .orElseThrow(() -> new CampaignException(ProductionReasonCode.CAMPAIGN_NOT_FOUND, "campaign not found"));
    }

    private String nextState(String from, String trigger, ProductionChangeCampaignEntity campaign, AuthenticatedActor actor) {
        return switch (trigger) {
            case "SUBMIT_FOR_REVIEW" -> {
                require(from, "DRAFT", "UNDER_REVIEW", actor, CampaignPermission.CAMPAIGN_CREATE);
                if (!campaign.getCreatorPrincipalId().equals(actor.actorId())) {
                    throw new CampaignException(
                            ProductionReasonCode.PRODUCTION_SOD_VIOLATION,
                            "only the campaign creator may submit for review"
                    );
                }
                yield "UNDER_REVIEW";
            }
            case "REVIEW_APPROVE" -> {
                require(from, "UNDER_REVIEW", "APPROVED", actor, CampaignPermission.CAMPAIGN_REVIEW);
                if (campaign.getCreatorPrincipalId().equals(actor.actorId())) {
                    throw new CampaignException(
                            ProductionReasonCode.PRODUCTION_SOD_VIOLATION,
                            "creator cannot review the same campaign"
                    );
                }
                yield "APPROVED";
            }
            case "REVIEW_RETURN" -> require(from, "UNDER_REVIEW", "DRAFT", actor, CampaignPermission.CAMPAIGN_REVIEW);
            case "AUTHORIZE_CAMPAIGN" -> {
                require(from, "APPROVED", "AUTHORIZED", actor, CampaignPermission.CAMPAIGN_AUTHORIZE);
                if (campaign.getCreatorPrincipalId().equals(actor.actorId())) {
                    throw new CampaignException(
                            ProductionReasonCode.PRODUCTION_SOD_VIOLATION,
                            "creator cannot authorize the same campaign"
                    );
                }
                yield "AUTHORIZED";
            }
            case "RELEASE_CANARY" -> {
                require(from, "READY", "CANARY_ACTIVE", actor, CampaignPermission.RELEASE_COHORT);
                closureService.assertOpen(campaign.getCampaignId());
                yield "CANARY_ACTIVE";
            }
            case "RELEASE_STANDARD_COHORT", "RELEASE_COHORT" -> {
                require(from, "PAUSED_FOR_RELEASE", "COHORT_ACTIVE", actor, CampaignPermission.RELEASE_COHORT);
                closureService.assertOpen(campaign.getCampaignId());
                yield "COHORT_ACTIVE";
            }
            case "PAUSE_CAMPAIGN", "OPERATOR_PAUSE" -> {
                denyIfTerminal(from);
                requireHuman(actor, CampaignPermission.CAMPAIGN_PAUSE);
                yield "SUSPENDED";
            }
            case "SAFETY_SUSPEND" -> {
                denyIfTerminal(from);
                yield "SUSPENDED";
            }
            case "ABORT_CAMPAIGN" -> {
                denyIfTerminal(from);
                requireHuman(actor, CampaignPermission.CAMPAIGN_ABORT);
                yield "ABORTED";
            }
            default -> throw new CampaignException(
                    ProductionReasonCode.SYSTEM_TRANSITION_DENIED,
                    "transition not externally permitted: " + from + " / " + trigger
            );
        };
    }

    private static void denyIfTerminal(String from) {
        if ("COMPLETED".equals(from) || "ABORTED".equals(from) || "EXPIRED".equals(from)) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                    "illegal campaign transition from terminal state " + from
            );
        }
    }

    private String require(String from, String expectedFrom, String to, AuthenticatedActor actor, CampaignPermission permission) {
        requireState(from, expectedFrom);
        requireHuman(actor, permission);
        return to;
    }

    private static void requireState(String from, String expectedFrom) {
        if (!expectedFrom.equals(from)) {
            throw new CampaignException(
                    ProductionReasonCode.INVALID_CAMPAIGN_TRANSITION,
                    "illegal campaign transition from " + from
            );
        }
    }

    private static void requireHuman(AuthenticatedActor actor, CampaignPermission permission) {
        if (actor == null || !actor.authenticated() || !actor.authorities().contains(permission)) {
            throw new CampaignException(
                    ProductionReasonCode.UNAUTHENTICATED_HUMAN_ACTOR,
                    "required human campaign authority is absent"
            );
        }
    }

    private void bootstrapControlPlane(UUID campaignId, UUID revisionId, Instant now) {
        Timestamp ts = Timestamp.from(now);
        jdbc.update(
                "INSERT INTO campaign_control_generation (campaign_id, current_generation, updated_at, version) VALUES (:id, 1, :now, 0)",
                new MapSqlParameterSource().addValue("id", campaignId).addValue("now", ts)
        );
        jdbc.update(
                "INSERT INTO campaign_release_generation (campaign_id, current_generation, updated_at, version) VALUES (:id, 0, :now, 0)",
                new MapSqlParameterSource().addValue("id", campaignId).addValue("now", ts)
        );
        jdbc.update(
                "INSERT INTO campaign_lease (campaign_id, fencing_token, status, version) VALUES (:id, 0, 'NONE', 0)",
                new MapSqlParameterSource("id", campaignId)
        );
        jdbc.update(
                "INSERT INTO campaign_forward_progression_closure (campaign_id, revision_id, state, version) VALUES (:id, :revisionId, 'OPEN', 0)",
                new MapSqlParameterSource().addValue("id", campaignId).addValue("revisionId", revisionId)
        );
        jdbc.update(
                """
                INSERT INTO campaign_mutation_slot
                    (campaign_id, holder_type, state, version)
                VALUES (:id, 'NONE', 'EMPTY', 0)
                """,
                new MapSqlParameterSource("id", campaignId)
        );
        jdbc.update(
                """
                INSERT INTO campaign_safety_exposure
                    (campaign_id, revision_id, forward_mutation_exposure, recovery_mutation_exposure,
                     distinct_cells_exposed, unresolved_forward_exposure, unresolved_recovery_exposure,
                     verification_failure_count, consecutive_failure_count, version)
                VALUES (:id, :revisionId, 0, 0, 0, 0, 0, 0, 0, 0)
                """,
                new MapSqlParameterSource().addValue("id", campaignId).addValue("revisionId", revisionId)
        );
        jdbc.update(
                "INSERT INTO campaign_recovery (campaign_id, revision_id, state, version) VALUES (:id, :revisionId, 'NOT_REQUIRED', 0)",
                new MapSqlParameterSource().addValue("id", campaignId).addValue("revisionId", revisionId)
        );
    }

    private static CampaignCohortEntity cohort(
            UUID cohortId, UUID campaignId, UUID revisionId, int sequence, String type, Instant now
    ) {
        CampaignCohortEntity cohort = new CampaignCohortEntity();
        cohort.setCohortId(cohortId);
        cohort.setCampaignId(campaignId);
        cohort.setRevisionId(revisionId);
        cohort.setCohortSequence(sequence);
        cohort.setCohortType(type);
        cohort.setState("PLANNED");
        cohort.setCreatedAt(now);
        cohort.setUpdatedAt(now);
        return cohort;
    }

    private static String itemFingerprint(CampaignExecutionItemEntity item) {
        Map<String, Object> material = CampaignFingerprintFactory.material();
        material.put("cellId", item.getCellId());
        material.put("parameter", item.getParameter());
        material.put("expected", CampaignHandoffIdFactory.canonicalDecimal(item.getExpectedValue()));
        material.put("desired", CampaignHandoffIdFactory.canonicalDecimal(item.getDesiredValue()));
        material.put("rollback", CampaignHandoffIdFactory.canonicalDecimal(item.getRollbackValue()));
        material.put("unit", item.getUnit());
        material.put("objectType", item.getObjectType());
        material.put("phase14PlanId", item.getPhase14PlanId() == null ? null : item.getPhase14PlanId().toString());
        material.put("phase14PlanFingerprint", item.getPhase14PlanFingerprint());
        material.put("phase15ExecutionId", item.getPhase15ExecutionId() == null ? null : item.getPhase15ExecutionId().toString());
        material.put("phase15ExecutionFingerprint", item.getPhase15ExecutionFingerprint());
        return CampaignFingerprintFactory.hash(material);
    }

    static String revisionFingerprint(
            ProductionChangeCampaignEntity campaign,
            CampaignRevisionEntity revision,
            List<CampaignExecutionItemEntity> items,
            List<CampaignCohortEntity> cohorts
    ) {
        Map<String, Object> material = fingerprintMaterial(campaign, revision, items, cohorts);
        return CampaignFingerprintFactory.hash(material);
    }

    static Map<String, Object> fingerprintMaterial(
            ProductionChangeCampaignEntity campaign,
            CampaignRevisionEntity revision,
            List<CampaignExecutionItemEntity> items,
            List<CampaignCohortEntity> cohorts
    ) {
        Map<String, Object> material = CampaignFingerprintFactory.material();
        material.put("campaignId", campaign.getCampaignId() == null ? null : campaign.getCampaignId().toString());
        material.put("revisionNumber", revision.getRevisionNumber());
        material.put("objective", campaign.getObjective());
        material.put("changeControlReference", campaign.getChangeControlReference());
        material.put("productionTargetId", campaign.getProductionTargetId());
        material.put("vendor", campaign.getVendor());
        material.put("platform", campaign.getPlatform());
        material.put("environment", campaign.getEnvironment());
        material.put("networkDomain", campaign.getNetworkDomain());
        material.put("authorizationGeneration", revision.getAuthorizationGeneration());
        material.put("policyVersions", revision.getPolicyVersions());
        material.put("windows", revision.getWindows());
        material.put("externalTicketCurrentnessRequired", revision.isExternalTicketCurrentnessRequired());
        material.put("safetyPolicy", "p18-safety-v1");
        material.put("progressionPolicy", "p18-progression-v1");
        material.put("observationPolicy", "p18-observation-v1");
        material.put("healthPolicy", "p18-health-v1");
        material.put("verificationPolicy", "p18-verification-v1");
        material.put("rollbackPolicy", "p18-rollback-v1");
        material.put("rateLimits", "maxActiveMutations=1");
        material.put("blastLimits", "hardMaxItems=25");
        material.put("p17TransportIdentity", "UNCONFIGURED");
        material.put("p17CertificationCurrentness", "NOT_EXECUTED");
        material.put("p17OnboardingCurrentness", "L0");
        material.put("p16ProductionChangeIdentity", null);
        material.put("p16ProductionFingerprint", null);
        List<Map<String, Object>> orderedItems = items.stream().map(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("itemSequence", item.getItemSequence());
            row.put("cellId", item.getCellId());
            row.put("parameter", item.getParameter());
            row.put("expected", CampaignHandoffIdFactory.canonicalDecimal(item.getExpectedValue()));
            row.put("desired", CampaignHandoffIdFactory.canonicalDecimal(item.getDesiredValue()));
            row.put("rollback", CampaignHandoffIdFactory.canonicalDecimal(item.getRollbackValue()));
            row.put("unit", item.getUnit());
            row.put("phase14PlanId", item.getPhase14PlanId() == null ? null : item.getPhase14PlanId().toString());
            row.put("phase14PlanFingerprint", item.getPhase14PlanFingerprint());
            row.put("phase15ExecutionId", item.getPhase15ExecutionId() == null ? null : item.getPhase15ExecutionId().toString());
            row.put("phase15ExecutionFingerprint", item.getPhase15ExecutionFingerprint());
            row.put("itemFingerprint", item.getItemFingerprint());
            return row;
        }).toList();
        material.put("orderedItems", orderedItems);
        List<Map<String, Object>> orderedCohorts = cohorts.stream()
                .filter(c -> c != null)
                .map(c -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("cohortSequence", c.getCohortSequence());
                    row.put("cohortType", c.getCohortType());
                    return row;
                })
                .toList();
        material.put("orderedCohorts", orderedCohorts);
        return material;
    }

    static String defaultPolicyVersions() {
        Map<String, Object> policies = new LinkedHashMap<>();
        policies.put("safety", "p18-safety-v1");
        policies.put("progression", "p18-progression-v1");
        policies.put("observation", "p18-observation-v1");
        policies.put("health", "p18-health-v1");
        policies.put("verification", "p18-verification-v1");
        policies.put("rollback", "p18-rollback-v1");
        return CanonicalJson.serialize(policies);
    }

    static String defaultWindows() {
        Map<String, Object> windows = new LinkedHashMap<>();
        windows.put("executionWindow", "MANUAL");
        return CanonicalJson.serialize(windows);
    }
}
