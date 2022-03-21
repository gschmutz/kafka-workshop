package com.trivadis.kafkaws.springbootkafkaproducer;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaProducerException;
import org.springframework.kafka.core.KafkaSendCallback;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;

@Component
public class KafkaEventProducerAsync {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventProducerAsync.class);

    @Autowired
    private KafkaTemplate<Long, String> kafkaTemplate;

    @Value("${topic.name}")
    String kafkaTopic;

    public void produce(Integer id, Long key, String value) {
        long time = System.currentTimeMillis();

        ListenableFuture<SendResult<Long, String>> future = kafkaTemplate.send(kafkaTopic, key, value);

        future.addCallback(new KafkaSendCallback<Long, String>() {
            @Override
            public void onSuccess(SendResult<Long, String> result) {
                long elapsedTime = System.currentTimeMillis() - time;
                System.out.printf("[" + id + "] sent record(key=%s value=%s) "
                                + "meta(partition=%d, offset=%d) time=%d\n",
                        key, value, result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(), elapsedTime);
            }

            @Override
            public void onFailure(KafkaProducerException ex) {
                ProducerRecord<Long, String> failed = ex.getFailedProducerRecord();
            }
        } );

    }
}