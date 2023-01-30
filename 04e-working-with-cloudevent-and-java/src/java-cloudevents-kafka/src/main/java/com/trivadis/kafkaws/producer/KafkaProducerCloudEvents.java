package com.trivadis.kafkaws.producer;

import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventExtension;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.message.Encoding;
import io.cloudevents.jackson.JsonFormat;
import io.cloudevents.kafka.CloudEventSerializer;
import io.cloudevents.kafka.PartitionKeyExtensionInterceptor;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.LongSerializer;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;

public class KafkaProducerCloudEvents {

    private final static String TOPIC = "test-java-ce-topic";
    private final static String BOOTSTRAP_SERVERS =
            "dataplatform:9092, dataplatform:9093, dataplatform:9094";

    private static Producer<Long, CloudEvent> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaExampleProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CloudEventSerializer.class);

        // Configure the CloudEventSerializer to emit events as json structured events
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CloudEventSerializer.class);
        props.put(CloudEventSerializer.ENCODING_CONFIG, Encoding.STRUCTURED);
        props.put(CloudEventSerializer.EVENT_FORMAT_CONFIG, JsonFormat.CONTENT_TYPE);

        // add Interceptor for the partitionKey extension
        //props.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, io.cloudevents.kafka.PartitionKeyExtensionInterceptor.class.getName());

        return new KafkaProducer<>(props);
    }

    static void runProducer(final int sendMessageCount, final int waitMsInBetween, final long id) throws Exception {
        final Producer<Long, CloudEvent> producer = createProducer();
        long time = System.currentTimeMillis();
        Long key = (id > 0) ? id : null;

        // Create an event template to set basic CloudEvent attributes
        CloudEventBuilder eventTemplate = CloudEventBuilder.v1()
                .withSource(URI.create("https://github.com/gschmutz/kafka-workshop/examples/kafka"))
                .withType("producer.example");

        try {
            for (long index = 0; index < sendMessageCount; index++) {
                String data = "[" + id + "] Hello Kafka " + index + " => " + LocalDateTime.now();

                // Create the event starting from the template
                CloudEvent event = eventTemplate.newBuilder()
                        .withId(UUID.randomUUID().toString())
                        .withData("text/plain", data.getBytes(StandardCharsets.UTF_8))
                        // .withExtension(PartitionKeyExtensionInterceptor.PARTITION_KEY_EXTENSION, 5L)
                        .build();

                ProducerRecord<Long, CloudEvent> record = new ProducerRecord<>(TOPIC, event);
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