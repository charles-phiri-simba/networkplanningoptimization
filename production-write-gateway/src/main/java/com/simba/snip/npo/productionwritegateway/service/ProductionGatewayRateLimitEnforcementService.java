package com.simba.snip.npo.productionwritegateway.service;

import com.simba.snip.npo.productionchange.protocol.ProductionRateLimitCounters;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import com.simba.snip.npo.productionwritegateway.config.ProductionChangeGatewayProperties;
import com.simba.snip.npo.productionwritegateway.exception.GatewayDeniedException;
import com.simba.snip.npo.productionwritegateway.metrics.ProductionGatewayMetrics;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Gateway independently revalidates issuance-side durable counters. It must not increment:
 * grant issuance is the single authoritative writer (A16-03 / spec §7.5).
 * Unknown or exceeded state denies with zero mutation.
 */
@Service
public class ProductionGatewayRateLimitEnforcementService {

    private final JdbcTemplate jdbcTemplate;
    private final ProductionChangeGatewayProperties properties;
    private final ProductionGatewayMetrics metrics;

    public ProductionGatewayRateLimitEnforcementService(
            JdbcTemplate jdbcTemplate,
            ProductionChangeGatewayProperties properties,
            ProductionGatewayMetrics metrics
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.metrics = metrics;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void enforce(String targetId, String cellId, UUID grantId, UUID productionChangeId) {
        Instant now = Instant.now();
        assertWithinLimit(
                ProductionRateLimitCounters.TARGET_HOUR,
                targetId,
                Duration.ofHours(1),
                properties.getMaximumChangesPerTargetPerHour(),
                now,
                grantId,
                productionChangeId
        );
        assertWithinLimit(
                ProductionRateLimitCounters.CELL_DAY,
                cellId,
                Duration.ofDays(1),
                properties.getMaximumChangesPerCellPerDay(),
                now,
                grantId,
                productionChangeId
        );
    }

    private void assertWithinLimit(
            String scopeType,
            String scopeKey,
            Duration window,
            int max,
            Instant now,
            UUID grantId,
            UUID productionChangeId
    ) {
        if (max <= 0) {
            deny(grantId, productionChangeId);
        }
        Instant alignedWindow = ProductionRateLimitCounters.align(now, window);
        String counterId = ProductionRateLimitCounters.counterId(scopeType, scopeKey, alignedWindow);
        var rows = jdbcTemplate.query(
                "SELECT count FROM production_rate_limit_state WHERE counter_id = ? FOR UPDATE",
                (rs, i) -> rs.getInt("count"),
                counterId
        );
        if (rows.isEmpty()) {
            deny(grantId, productionChangeId);
        }
        int count = rows.get(0);
        if (count > max) {
            deny(grantId, productionChangeId);
        }
    }

    private void deny(UUID grantId, UUID productionChangeId) {
        metrics.incrementRateLimitDenials();
        throw GatewayDeniedException.deny(
                ProductionReasonCode.PRODUCTION_RATE_LIMIT_EXCEEDED, grantId, productionChangeId);
    }
}
