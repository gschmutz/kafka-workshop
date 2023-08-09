package com.trivadis.kafkaws.producer;

import com.trivadis.types.NotificationV1;
import io.confluent.kafka.serializers.KafkaJsonSerializerConfig;
import io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.LongSerializer;

import java.util.Properties;


public class KafkaProducerJson {

    private final static String TOPIC = "test-java-json-topic";
    private final static String BOOTSTRAP_SERVERS =
            "dataplatform:9092, dataplatform:9093, dataplatform:9094";
    private final static String SCHEMA_REGISTRY_URL = "http://dataplatform:8081";

    private static Producer<Long, NotificationV1> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaExampleProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaJsonSchemaSerializer.class.getName());
        props.put("auto.register.schemas", true);
        props.put("schema.registry.url", SCHEMA_REGISTRY_URL);   // use constant for "schema.registry.url"

        return new KafkaProducer<>(props);
    }

    static void runProducer(final int sendMessageCount, final int waitMsInBetween, final long id) throws Exception {
        final Producer<Long, NotificationV1> producer = createProducer();
        long time = System.currentTimeMillis();
        Long key = (id > 0) ? id : null;

        try {
            for (long index = 0; index < sendMessageCount; index++) {
                NotificationV1 notification = new NotificationV1().withId(1).withMessage( "Hello Kafka " + index+1);
                final ProducerRecord<Long, NotificationV1> record =
                        new ProducerRecord<>(TOPIC, key, notification);

                RecordMetadata metadata = producer.send(record).get();

                long elapsedTime = System.currentTimeMillis() - time;
                System.out.printf("[" + id + "] sent record(key=%s value=%s) " +
                                "meta(partition=%d, offset=%d) time=%d\n",
                        record.key(), record.value(), metadata.partition(),
                        metadata.offset(), elapsedTime);
                time = System.currentTimeMillis();

                Thread.sleep(waitMsInBetween);
            }
        } finally {
            producer.flush();
            producer.close();
        }
    }

    public static void main(String... args) throws Exception {
        if (args.length == 0) {
            runProducer(100,10,0);
        } else {
            runProducer(Integer.parseInt(args[0]),Integer.parseInt(args[1]),Long.parseLong(args[2]));
        }
    }
}