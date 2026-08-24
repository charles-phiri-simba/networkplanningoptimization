package com.simba.snip.npo.config;

import com.simba.snip.npo.telemetry.TelemetryMetrics;
import com.simba.snip.npo.telemetry.UnrecoverableTelemetryException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@EnableKafka
@EnableConfigurationProperties(KafkaProperties.class)
@ConditionalOnProperty(name = "snip.kafka-enabled", havingValue = "true")
public class KafkaTelemetryConfig {

    @Bean
    KafkaAdmin kafkaAdmin(KafkaProperties kafkaProperties) {
        return new KafkaAdmin(kafkaProperties.buildAdminProperties(null));
    }

    @Bean
    NewTopic cellKpiTopic(SnipProperties properties) {
        return TopicBuilder.name(properties.getTelemetryTopic()).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic cellKpiDlqTopic(SnipProperties properties) {
        return TopicBuilder.name(properties.getTelemetryDlqTopic()).partitions(1).replicas(1).build();
    }

    @Bean
    ProducerFactory<String, String> producerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties(null));
    }

    @Bean
    KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    @Bean
    ConsumerFactory<String, String> consumerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(null));
    }

    @Bean
    DefaultErrorHandler telemetryErrorHandler(
            KafkaTemplate<String, String> kafkaTemplate,
            SnipProperties properties,
            TelemetryMetrics metrics
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(properties.getTelemetryDlqTopic(), record.partition())
        );
        ConsumerRecordRecoverer countingRecoverer = (record, ex) -> {
            metrics.incrementDlq();
            recoverer.accept(record, ex);
        };
        DefaultErrorHandler handler = new DefaultErrorHandler(
                countingRecoverer,
                new FixedBackOff(properties.getKafkaRetryIntervalMs(), properties.getKafkaRetryAttempts())
        );
        handler.addNotRetryableExceptions(UnrecoverableTelemetryException.class);
        return handler;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler telemetryErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(telemetryErrorHandler);
        factory.setAutoStartup(true);
        return factory;
    }
}
