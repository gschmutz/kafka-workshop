package com.trivadis.kafkaws.springbootkafkaproducer;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class KafkaEventProducerAsync {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventProducerAsync.class);

    @Autowired
    private KafkaTemplate<Long, String> kafkaTemplate;

    @Value("${topic.name}")
    String kafkaTopic;

    public void produce(Integer id, Long key, String value) {
        long time = System.currentTimeMillis();

        CompletableFuture<SendResult<Long, String>> future = kafkaTemplate.send(kafkaTopic, key, value);

        future.thenAcceptAsync(result -> {
                long elapsedTime = System.currentTimeMillis() - time;
                System.out.printf("[" + id + "] sent record(key=%s value=%s) "
                                + "meta(partition=%d, offset=%d) time=%d\n",
                        key, value, result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(), elapsedTime);
            }).exceptionally(err -> {
                //log error;
                System.out.print("Unable to send message=[" + value + "] due to : " + err.getMessage());
                return null; //this usually lets you handle exceptions - like providing a value in case or errors - so you can continue the chain, but in this case, nothing doing so...
            });

    }
}