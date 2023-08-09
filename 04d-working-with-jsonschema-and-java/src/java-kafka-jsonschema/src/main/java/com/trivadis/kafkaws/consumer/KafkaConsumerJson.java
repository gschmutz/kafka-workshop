package com.trivadis.kafkaws.consumer;

import com.trivadis.types.NotificationV1;
import io.confluent.kafka.serializers.KafkaJsonDeserializer;
import io.confluent.kafka.serializers.KafkaJsonDeserializerConfig;
import io.confluent.kafka.serializers.KafkaJsonSerializerConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;

import java.util.Collections;
import java.util.Properties;


public class KafkaConsumerJson {
    private final static String TOPIC = "test-java-json-topic";
    private final static String BOOTSTRAP_SERVERS =
            "dataplatform:9092, dataplatform:9093, dataplatform:9094";
    private final static String SCHEMA_REGISTRY_URL = "http://dataplatform:8081";

    private static Consumer<Long, NotificationV1> createConsumer() {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "KafkaConsumerJson2");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 10000);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaJsonDeserializer.class.getName());
        props.put(KafkaJsonDeserializerConfig.JSON_VALUE_TYPE, NotificationV1.class.getName());
        //props.put(KafkaJsonDeserializerConfig.FAIL_UNKNOWN_PROPERTIES, false);
        props.put("schema.registry.url", SCHEMA_REGISTRY_URL);   // use constant for "schema.registry.url"

        // Create the consumer using props.
        final Consumer<Long, NotificationV1> consumer =
                new KafkaConsumer<>(props);

        // Subscribe to the topic.
        consumer.subscribe(Collections.singletonList(TOPIC));
        return consumer;
    }

    static void runConsumer(int waitMsInBetween) throws InterruptedException {
        final Consumer<Long, NotificationV1> consumer = createConsumer();

        final int giveUp = 100000;
        int noRecordsCount = 0;

        while (true) {
            final ConsumerRecords<Long, NotificationV1> consumerRecords = consumer.poll(100);

            if (consumerRecords.count()==0) {
                noRecordsCount++;
                if (noRecordsCount > giveUp)
                    break;
            }

            consumerRecords.forEach(record -> {
                System.out.printf("%d - Consumer Record:(Key: %d, Value: %s, Partition: %d, Offset: %d)\n",
                        consumerRecords.count(), record.key(), record.value(),
                        record.partition(), record.offset());
                try {
                    Thread.sleep(waitMsInBetween);
                } catch (InterruptedException e) {
                }
            });

            consumer.commitSync();
        }
        consumer.close();
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