package com.trivadis.kafkaws.consumer;

import com.trivadis.kafkaws.avro.v1.ListOfEvents;
import com.trivadis.kafkaws.avro.v1.NotificationSentEvent;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaConsumerAvro {
    private final static String TOPIC = "test-java-avro-idl-topic";
    private final static String BOOTSTRAP_SERVERS =
            "dataplatform:9092, dataplatform:9093, dataplatform:9094";
    private final static String SCHEMA_REGISTRY_URL = "http://dataplatform:8081";

    private static Consumer<Long, ListOfEvents> createConsumer() {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "KafkaConsumerAvro");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 10000);
        props.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, "false");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);   // use constant for "schema.registry.url"

        // Create the consumer using props.
        final Consumer<Long, ListOfEvents> consumer =
                new KafkaConsumer<>(props);

        // Subscribe to the topic.
        consumer.subscribe(Collections.singletonList(TOPIC));
        return consumer;
    }

    static void runConsumer(int waitMsInBetween) throws InterruptedException {
        final Consumer<Long, ListOfEvents> consumer = createConsumer();

        final int giveUp = 100;
        int noRecordsCount = 0;

        while (true) {
            final ConsumerRecords<Long, ListOfEvents> consumerRecords = consumer.poll(Duration.ofMillis(1000));

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