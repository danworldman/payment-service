package com.innowise.payment_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConfigTest {

    @Test
    void paymentTopic_shouldCreateTopicWithCorrectConfiguration() {
        KafkaConfig config = new KafkaConfig();
        NewTopic topic = config.paymentTopic();

        assertThat(topic).isNotNull();
        assertThat(topic.name()).isEqualTo("payment-events");
        assertThat(topic.numPartitions()).isEqualTo(1);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
    }
}