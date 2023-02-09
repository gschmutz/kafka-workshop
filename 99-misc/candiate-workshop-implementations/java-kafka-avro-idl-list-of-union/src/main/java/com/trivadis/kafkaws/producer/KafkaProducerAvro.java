package com.trivadis.kafkaws.producer;

import com.trivadis.kafkaws.avro.v1.*;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.producer.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class KafkaProducerAvro {

    private final static String TOPIC = "test-java-avro-idl-topic";
    private final static String BOOTSTRAP_SERVERS =
            "dataplatform:9092, dataplatform:9093, dataplatform:9094";
    private final static String SCHEMA_REGISTRY_URL = "http://dataplatform:8081";

    private static Producer<Long, ListOfEvents> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaExampleProducer");
        //props.put(KafkaAvroSerializerConfig.AUTO_REGISTER_SCHEMAS, "false");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);   // use constant for "schema.registry.url"

        return new KafkaProducer<>(props);
    }

    static void runProducer(final int sendMessageCount, final int waitMsInBetween, final long id) throws Exception {
        final Producer<Long, ListOfEvents> producer = createProducer();
        long time = System.currentTimeMillis();
        Long key = (id > 0) ? id : null;

        try {
            for (long index = 0; index < sendMessageCount; index++) {

                NotificationSentEvent notification = NotificationSentEvent.newBuilder()
                    .setNotification(Notification.newBuilder()
                        .setId(id)
                        .setMessage("Hello Kafka " + index)
                        .build())
                    .build();

                AlertSentEvent alert = AlertSentEvent.newBuilder()
                        .setAlert(Alert.newBuilder()
                                .setId(UUID.randomUUID())
                                .setMessage("Alert Message")
                                .setSeverity("ERROR")
                                .setWhen(Instant.now())
                                .build())
                        .build();

                List<Event> events = new ArrayList<>();
                events.add(Event.newBuilder().setEvent(notification).build());
                events.add(Event.newBuilder().setEvent(alert).build());

                ListOfEvents listOfEvents = ListOfEvents.newBuilder()
                        .setId(1L)
                        .setEvents(events)
                        .build();

                final ProducerRecord<Long, ListOfEvents> record =
                        new ProducerRecord<>(TOPIC, key, listOfEvents);
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