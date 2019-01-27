package com.trivadis.kafkaws.producer;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaProducerASync {

    private final static String TOPIC = "test-java-topic";
    private final static String BOOTSTRAP_SERVERS
            = "localhost:9092,localhost:9093";

    private static Producer<Long, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaExampleProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                LongSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    private static void runProducer(int sendMessageCount, int waitMsInBetween, long id) throws Exception {
        Long key = (id > 0) ? id : null;
        final CountDownLatch countDownLatch = new CountDownLatch(sendMessageCount);

        try (Producer<Long, String> producer = createProducer()) {
            for (int index = 0; index < sendMessageCount; index++) {
                long time = System.currentTimeMillis();

                ProducerRecord<Long, String> record
                        = new ProducerRecord<>(TOPIC, key,
                                "[" + id + "] Hello Kafka " + LocalDateTime.now());

                producer.send(record, (metadata, exception) -> {
                    long elapsedTime = System.currentTimeMillis() - time;
                    if (metadata != null) {
                        System.out.printf("[" + id + "] sent record(key=%s value=%s) "
                                + "meta(partition=%d, offset=%d) time=%d\n",
                                record.key(), record.value(), metadata.partition(),
                                metadata.offset(), elapsedTime);
                    } else {
                        exception.printStackTrace();
                    }
                    countDownLatch.countDown();
                });
            }
        }
    }

    public static void main(String... args) throws Exception {
        if (args.length == 0) {
            runProducer(100, 10, 0);
        } else {
            runProducer(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Long.parseLong(args[2]));
        }
    }

}
