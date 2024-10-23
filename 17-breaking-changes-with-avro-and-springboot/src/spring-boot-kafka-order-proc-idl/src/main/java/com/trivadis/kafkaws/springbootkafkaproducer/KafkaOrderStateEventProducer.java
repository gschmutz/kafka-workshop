package com.trivadis.kafkaws.springbootkafkaproducer;

import com.trivadis.kafkaws.order.avro.v1.OrderStateEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class KafkaOrderStateEventProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaOrderStateEventProducer.class);

    @Autowired
    private KafkaTemplate<Long, OrderStateEvent> kafkaTemplate;

    @Value("${topic.v1.name}")
    String kafkaTopic;

    public void produce(Integer id, Long key, OrderStateEvent orderStateEvent) {
        long time = System.currentTimeMillis();

        SendResult<Long, OrderStateEvent> result = null;
        try {
            result = kafkaTemplate.send(kafkaTopic, key, orderStateEvent).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        long elapsedTime = System.currentTimeMillis() - time;
        System.out.printf("[" + id + "] sent record(key=%s value=%s) "
                        + "meta(partition=%d, offset=%d) time=%d\n",
                key, orderStateEvent, result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset(), elapsedTime);
    }
}