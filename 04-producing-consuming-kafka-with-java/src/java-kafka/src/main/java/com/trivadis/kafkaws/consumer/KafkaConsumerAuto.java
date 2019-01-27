package com.trivadis.kafkaws.consumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class KafkaConsumerAuto {

    private final static String TOPIC = "test-java-topic";
    private final static String BOOTSTRAP_SERVERS
            = "localhost:9092,localhost:9093,localhost:9094";
    private final static Duration CONSUMER_TIMEOUT = Duration.ofSeconds(1);

    private static Consumer<Long, String> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "KakfaConsumerAuto");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 10000);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // Create the consumer using props.
        Consumer<Long, String> consumer = new KafkaConsumer<>(props);

        // Subscribe to the topic.
        consumer.subscribe(Collections.singletonList(TOPIC));
        return consumer;
    }

    private static void runConsumer(int waitMsInBetween) throws InterruptedException {
        final int giveUp = 100;

        try (Consumer<Long, String> consumer = createConsumer()) {
            int noRecordsCount = 0;

            while (true) {
                ConsumerRecords<Long, String> consumerRecords = consumer.poll(CONSUMER_TIMEOUT);

                if (consumerRecords.isEmpty()) {
                    noRecordsCount++;
                    if (noRecordsCount > giveUp) {
                        break;
                    } else {
                        continue;
                    }
                }

                consumerRecords.forEach(record -> {
                    System.out.printf("%d - Consumer Record:(Key: %d, Value: %s, Partition: %d, Offset: %d)\n",
                            consumerRecords.count(), record.key(), record.value(),
                            record.partition(), record.offset());
                    try {
                        // Simulate slow processing
                        Thread.sleep(waitMsInBetween);
                    } catch (InterruptedException e) {
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("DONE");
    }

    public static void main(String... args) throws Exception {
        if (args.length == 0) {
            runConsumer(10);
        } else {
            runConsumer(Integer.parseInt(args[0]));
        }
    }

}
