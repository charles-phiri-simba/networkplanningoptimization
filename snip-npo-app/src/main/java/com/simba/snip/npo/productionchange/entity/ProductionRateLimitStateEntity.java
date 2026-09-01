package com.simba.snip.npo.productionchange.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "production_rate_limit_state")
public class ProductionRateLimitStateEntity {

    @Id
    @Column(name = "counter_id", length = 256)
    private String counterId;

    @Column(name = "scope_type", nullable = false, length = 32)
    private String scopeType;

    @Column(name = "scope_key", nullable = false, length = 256)
    private String scopeKey;

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Column(nullable = false)
    private int count;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static ProductionRateLimitStateEntity create(
            String counterId,
            String scopeType,
            String scopeKey,
            Instant windowStart,
            int count,
            Instant updatedAt
    ) {
        ProductionRateLimitStateEntity entity = new ProductionRateLimitStateEntity();
        entity.counterId = counterId;
        entity.scopeType = scopeType;
        entity.scopeKey = scopeKey;
        entity.windowStart = windowStart;
        entity.count = count;
        entity.updatedAt = updatedAt;
        return entity;
    }

    public String getCounterId() { return counterId; }
    public String getScopeType() { return scopeType; }
    public String getScopeKey() { return scopeKey; }
    public Instant getWindowStart() { return windowStart; }
    public int getCount() { return count; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setWindowStart(Instant windowStart) { this.windowStart = windowStart; }
    public void setCount(int count) { this.count = count; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
