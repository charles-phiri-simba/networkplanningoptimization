package com.simba.snip.npo.telemetry;

import com.simba.snip.npo.AbstractPostgresIT;
import com.simba.snip.npo.NpoApplication;
import com.simba.snip.npo.persist.CellRepository;
import com.simba.snip.npo.persist.KpiObservationRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(
        classes = NpoApplication.class,
        properties = {
                "snip.kafka-enabled=true",
                "spring.kafka.listener.auto-startup=true",
                "spring.kafka.consumer.group-id=snip-npo-telemetry-it"
        }
)
class TelemetryKafkaTest extends AbstractPostgresIT {

    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    static {
        KAFKA.start();
    }

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
        registry.add("snip.kafka-enabled", () -> "true");
    }

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private KpiObservationRepository kpiObservationRepository;

    @Autowired
    private CellRepository cellRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TelemetryMetrics metrics;

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("DELETE FROM kpi_observation WHERE event_id LIKE 'p2-kafka-%'");
    }

    @Test
    void keyedEventIsProjectedAndInvalidEventGoesToDlq() throws Exception {
        String eventId = "p2-kafka-" + UUID.randomUUID();
        String payload = """
                {
                  "eventId": "%s",
                  "eventType": "CELL_KPI_OBSERVED",
                  "schemaVersion": "1.0",
                  "source": "SNIP_SIMULATOR",
                  "cellId": "CELL-003",
                  "metric": "BLER_DL",
                  "value": 0.07,
                  "unit": "ratio",
                  "eventTime": "2026-08-24T12:00:00Z",
                  "synthetic": true
                }
                """.formatted(eventId);
        kafkaTemplate.send("snip.telemetry.cell-kpi.v1", "CELL-003", payload).get();
        waitUntil(() -> kpiObservationRepository.findByEventId(eventId).isPresent());
        assertEquals(Instant.parse("2026-08-24T12:00:00Z"),
                kpiObservationRepository.findByEventId(eventId).orElseThrow().getEventTime());
        assertEquals("CELL-003", kpiObservationRepository.findByEventId(eventId).orElseThrow().getCell().getCellId());

        long cells = cellRepository.count();
        kafkaTemplate.send("snip.telemetry.cell-kpi.v1", "CELL-MISSING", """
                {
                  "eventId": "p2-kafka-missing-%s",
                  "eventType": "CELL_KPI_OBSERVED",
                  "schemaVersion": "1.0",
                  "source": "SNIP_SIMULATOR",
                  "cellId": "CELL-MISSING",
                  "metric": "BLER_DL",
                  "value": 0.12,
                  "unit": "ratio",
                  "eventTime": "2026-08-24T12:05:00Z",
                  "synthetic": true
                }
                """.formatted(UUID.randomUUID())).get();

        kafkaTemplate.send("snip.telemetry.cell-kpi.v1", "CELL-001", "{not-json").get();

        ConsumerRecord<String, String> dlq = pollDlq();
        assertTrue("CELL-MISSING".equals(dlq.key()) || "CELL-001".equals(dlq.key()));
        assertTrue(dlq.value().contains("CELL-MISSING") || dlq.value().contains("not-json"));
        assertEquals(cells, cellRepository.count());
        assertFalse(cellRepository.findByCellId("CELL-MISSING").isPresent());
        assertTrue(metrics.getEventsDlq() >= 1);
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(200);
        }
        fail("timed out waiting for Kafka projection");
    }

    private ConsumerRecord<String, String> pollDlq() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "snip-npo-dlq-it-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of("snip.telemetry.cell-kpi.dlq.v1"));
            for (int i = 0; i < 40; i++) {
                var records = consumer.poll(Duration.ofMillis(250));
                if (!records.isEmpty()) {
                    return records.iterator().next();
                }
            }
        }
        fail("timed out waiting for DLQ record");
        return null;
    }
}
