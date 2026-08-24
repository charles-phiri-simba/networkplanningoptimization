package com.simba.snip.npo.telemetry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simba.snip.npo.config.SnipProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "snip.kafka-enabled", havingValue = "true")
public class TelemetryEventListener {

    private static final Logger log = LoggerFactory.getLogger(TelemetryEventListener.class);

    private final ObjectMapper objectMapper;
    private final TelemetryProjectionService projectionService;
    private final TelemetryMetrics metrics;
    private final SnipProperties properties;

    public TelemetryEventListener(
            ObjectMapper objectMapper,
            TelemetryProjectionService projectionService,
            TelemetryMetrics metrics,
            SnipProperties properties
    ) {
        this.objectMapper = objectMapper;
        this.projectionService = projectionService;
        this.metrics = metrics;
        this.properties = properties;
    }

    @KafkaListener(topics = "${snip.telemetry-topic}", groupId = "snip-npo-telemetry")
    public void onMessage(String payload, @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        metrics.incrementConsumed();
        log.info(
                "telemetryEventsConsumed=1 topic={} keyPresent={} payloadBytes={}",
                properties.getTelemetryTopic(),
                key != null,
                payload == null ? 0 : payload.length()
        );
        TelemetryEvent event;
        try {
            event = objectMapper.readValue(payload, TelemetryEvent.class);
        } catch (JsonProcessingException ex) {
            throw new UnrecoverableTelemetryException("invalid telemetry JSON", ex);
        }
        if (key != null && event.cellId() != null && !key.equals(event.cellId())) {
            log.warn("kafka key {} does not match event cellId {}; projecting by cellId", key, event.cellId());
        }
        projectionService.project(event);
    }
}
