package com.simba.snip.npo.productionwritegateway.service;

import com.simba.snip.npo.productionchange.protocol.GrantStatus;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.entity.ProductionExecutionGrantEntity;
import com.simba.snip.npo.productionwritegateway.metrics.ProductionGatewayMetrics;
import com.simba.snip.npo.productionwritegateway.repository.ProductionExecutionGrantRepository;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

/**
 * Atomic ISSUED→CONSUMED. Bindings in CONSUME_SQL are compared to the durable
 * grant row (concurrent grant mutation still fails the WHERE). They are not a
 * substitute for live currentness: {@link ProductionGatewayPreflightService}
 * re-reads production_network_change fingerprint and authorization generation,
 * target enabled/ACTIVE state and security/capability/credential profiles,
 * change-control VALID/unexpired, change window, lease fencing, kill switch,
 * and rate-limit counters after consume and before send eligibility.
 */
@Service
public class ProductionGrantConsumeService {

    public static final String CONSUME_SQL = """
            UPDATE production_execution_grant
               SET status = 'CONSUMED', consumed_at = :now, version = version + 1
             WHERE grant_id = :grantId
               AND status = 'ISSUED'
               AND expires_at > :now
               AND production_change_id = :productionChangeId
               AND phase15_execution_id = :phase15ExecutionId
               AND target_id = :targetId
               AND production_fingerprint = :productionFingerprint
               AND authorization_generation = :authorizationGeneration
               AND fencing_token = :fencingToken
               AND operation_binding_hash = :operationBindingHash
               AND grant_type = :grantType
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ProductionExecutionGrantRepository grantRepository;
    private final ProductionGatewayMetrics metrics;

    public ProductionGrantConsumeService(
            NamedParameterJdbcTemplate jdbcTemplate,
            ProductionExecutionGrantRepository grantRepository,
            ProductionGatewayMetrics metrics
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.grantRepository = grantRepository;
        this.metrics = metrics;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ConsumeResult consume(ConsumeCommand command) {
        Instant now = Instant.now();
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("now", Timestamp.from(now))
                .addValue("grantId", command.grantId())
                .addValue("productionChangeId", command.productionChangeId())
                .addValue("phase15ExecutionId", command.phase15ExecutionId())
                .addValue("targetId", command.targetId())
                .addValue("productionFingerprint", command.productionFingerprint())
                .addValue("authorizationGeneration", command.authorizationGeneration())
                .addValue("fencingToken", command.fencingToken())
                .addValue("operationBindingHash", command.operationBindingHash())
                .addValue("grantType", command.grantType().name());
        int rows = jdbcTemplate.update(CONSUME_SQL, params);
        if (rows == 1) {
            return ConsumeResult.success();
        }
        metrics.incrementGrantConsumeConflicts();
        return ConsumeResult.denied(classifyDenial(command, now));
    }

    private ProductionReasonCode classifyDenial(ConsumeCommand command, Instant now) {
        Optional<ProductionExecutionGrantEntity> loaded = grantRepository.findById(command.grantId());
        if (loaded.isEmpty()) {
            return ProductionReasonCode.PRODUCTION_GRANT_NOT_FOUND;
        }
        ProductionExecutionGrantEntity grant = loaded.get();
        if (GrantStatus.CONSUMED.name().equals(grant.getStatus())) {
            return ProductionReasonCode.PRODUCTION_GRANT_ALREADY_CONSUMED;
        }
        if (GrantStatus.REVOKED.name().equals(grant.getStatus())) {
            return ProductionReasonCode.PRODUCTION_GRANT_REVOKED;
        }
        if (GrantStatus.EXPIRED.name().equals(grant.getStatus())
                || (grant.getExpiresAt() != null && !grant.getExpiresAt().isAfter(now))) {
            return ProductionReasonCode.PRODUCTION_GRANT_EXPIRED;
        }
        if (!command.grantType().name().equals(grant.getGrantType())
                || !command.productionChangeId().equals(grant.getProductionChangeId())
                || !command.phase15ExecutionId().equals(grant.getPhase15ExecutionId())
                || !command.targetId().equals(grant.getTargetId())
                || !command.productionFingerprint().equals(grant.getProductionFingerprint())
                || command.authorizationGeneration() != grant.getAuthorizationGeneration()
                || command.fencingToken() != grant.getFencingToken()
                || !command.operationBindingHash().equals(grant.getOperationBindingHash())) {
            if (command.fencingToken() != grant.getFencingToken()) {
                return ProductionReasonCode.PRODUCTION_FENCING_MISMATCH;
            }
            return ProductionReasonCode.PRODUCTION_GRANT_BINDING_MISMATCH;
        }
        return ProductionReasonCode.PRODUCTION_GRANT_CONSUME_CONFLICT;
    }
}
