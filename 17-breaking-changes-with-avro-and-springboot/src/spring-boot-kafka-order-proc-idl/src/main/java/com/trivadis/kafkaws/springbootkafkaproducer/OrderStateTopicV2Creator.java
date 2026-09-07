package com.trivadis.kafkaws.springbootkafkaproducer;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.stereotype.Component;

@Component
public class OrderStateTopicV2Creator {
    @Value(value = "${topic.v2.name}")
    private String testTopic;

    @Value(value = "${topic.v2.partitions}")
    private Integer testTopicPartitions;

    @Value(value = "${topic.v2.replication-factor}")
    private short testTopicReplicationFactor;

    @Bean
    public NewTopic orderStateV2Topic() {
        return TopicBuilder.name(testTopic)
                        .partitions(testTopicPartitions)
                        .replicas(testTopicReplicationFactor)
                        .build();
    }
}
