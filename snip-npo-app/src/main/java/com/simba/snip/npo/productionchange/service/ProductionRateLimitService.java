package com.simba.snip.npo.productionchange.service;

import com.simba.snip.npo.productionchange.config.ProductionChangeProperties;
import com.simba.snip.npo.productionchange.exception.ProductionChangeException;
import com.simba.snip.npo.productionchange.metrics.ProductionChangeMetrics;
import com.simba.snip.npo.productionchange.protocol.ProductionRateLimitCounters;
import com.simba.snip.npo.productionchange.protocol.ProductionReasonCode;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
public class ProductionRateLimitService {

    /**
     * Atomic consume: first insert and subsequent increment share the same predicate.
     * {@code maximum <= 0} never inserts. Concurrent first-writers serialize on PK.
     */
    static final String CONSUME_SQL = """
            INSERT INTO production_rate_limit_state
                (counter_id, scope_type, scope_key, window_start, count, updated_at)
            SELECT :counterId, :scopeType, :scopeKey, :windowStart, 1, :now
             WHERE :maximum > 0
            ON CONFLICT (counter_id) DO UPDATE
               SET count = CASE
                     WHEN production_rate_limit_state.window_start < EXCLUDED.window_start THEN 1
                     ELSE production_rate_limit_state.count + 1
                   END,
                   window_start = CASE
                     WHEN production_rate_limit_state.window_start < EXCLUDED.window_start THEN EXCLUDED.window_start
                     ELSE production_rate_limit_state.window_start
                   END,
                   updated_at = EXCLUDED.updated_at
             WHERE :maximum > 0
               AND (
                    production_rate_limit_state.window_start < EXCLUDED.window_start
                    OR production_rate_limit_state.count < :maximum
               )
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ProductionChangeProperties properties;
    private final ProductionChangeMetrics metrics;
    private final Clock clock;

    public ProductionRateLimitService(
            NamedParameterJdbcTemplate jdbcTemplate,
            ProductionChangeProperties properties,
            ProductionChangeMetrics metrics,
            Clock clock
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void consumeOrDeny(String scopeType, String scopeKey, Duration window, int maximum) {
        if (scopeType == null || scopeType.isBlank() || scopeKey == null || scopeKey.isBlank() || window == null) {
            deny("rate-limit policy state unknown; deny");
        }
        try {
            Instant now = clock.instant();
            Instant windowStart = ProductionRateLimitCounters.align(now, window);
            String counterId = ProductionRateLimitCounters.counterId(scopeType, scopeKey, windowStart);
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("counterId", counterId)
                    .addValue("scopeType", scopeType)
                    .addValue("scopeKey", scopeKey)
                    .addValue("windowStart", Timestamp.from(windowStart))
                    .addValue("now", Timestamp.from(now))
                    .addValue("maximum", maximum);
            int rows = jdbcTemplate.update(CONSUME_SQL, params);
            if (rows != 1) {
                deny("durable rate limit exceeded or disabled for " + scopeType);
            }
        } catch (ProductionChangeException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            deny("rate-limit state unknown; deny", ex);
        }
    }

    private void deny(String message) {
        deny(message, null);
    }

    private void deny(String message, RuntimeException cause) {
        metrics.incrementRateLimitDenials();
        if (cause == null) {
            throw new ProductionChangeException(ProductionReasonCode.PRODUCTION_RATE_LIMIT_EXCEEDED, message);
        }
        throw new ProductionChangeException(ProductionReasonCode.PRODUCTION_RATE_LIMIT_EXCEEDED, message, cause);
    }

    public void consumeTargetHour(String targetId) {
        consumeOrDeny(ProductionRateLimitCounters.TARGET_HOUR, targetId, Duration.ofHours(1), properties.getMaximumChangesPerTargetPerHour());
    }

    public void consumeCellDay(String cellId) {
        consumeOrDeny(ProductionRateLimitCounters.CELL_DAY, cellId, Duration.ofDays(1), properties.getMaximumChangesPerCellPerDay());
    }

    public void consumeActorHour(String actorPrincipalId) {
        consumeOrDeny(ProductionRateLimitCounters.ACTOR_HOUR, actorPrincipalId, Duration.ofHours(1), properties.getMaximumGrantsPerActorPerHour());
    }
}
