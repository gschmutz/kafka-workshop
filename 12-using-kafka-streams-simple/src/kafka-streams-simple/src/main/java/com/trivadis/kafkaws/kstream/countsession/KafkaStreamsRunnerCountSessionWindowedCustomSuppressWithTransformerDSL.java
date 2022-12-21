package com.trivadis.kafkaws.kstream.countsession;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.*;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class KafkaStreamsRunnerCountSessionWindowedCustomSuppressWithTransformerDSL {
    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm.ss")
            .withZone(ZoneId.systemDefault());

    public static String formatKey (String key, Instant startTime, Instant endTime) {
        return key + "@" + formatter.format(startTime) + "->" + formatter.format(startTime);
    }

    public static void main(String[] args) {
        Map<String, Object> serdeProps = new HashMap<>();
        final Serializer<StateValue> pageViewSerializer = new JsonPOJOSerializer<>();
        serdeProps.put("JsonPOJOClass", StateValue.class);
        pageViewSerializer.configure(serdeProps, false);

        final Deserializer<StateValue> pageViewDeserializer = new JsonPOJODeserializer<>();
        serdeProps.put("JsonPOJOClass", StateValue.class);
        pageViewDeserializer.configure(serdeProps, false);

        final Serde<StateValue> stateValueSerde = Serdes.serdeFrom(pageViewSerializer, pageViewDeserializer);

        // the builder is used to construct the topology
        StreamsBuilder builder = new StreamsBuilder();

        // create custom state store
        final StoreBuilder<KeyValueStore<String, StateValue>> myStateStore = Stores
                .keyValueStoreBuilder(Stores.persistentKeyValueStore("suppressed_windowed_transformer"), Serdes.String(), stateValueSerde)
                .withCachingDisabled();
        builder.addStateStore(myStateStore);

        // read from the source topic, "test-kstream-input-topic"
        KStream<String, String> stream = builder.stream("test-kstream-input-topic");

        // create a session window with an inactivity gap to 30 seconds
        SessionWindows sessionWindow =
                SessionWindows.ofInactivityGapAndGrace(Duration.ofSeconds(30), Duration.ofSeconds(10));

        KTable<Windowed<String>, Long> counts = stream.groupByKey()
                .windowedBy(sessionWindow)
                .count(Materialized.as("countSessionWindowedCustomSuppressed"));

        KStream<String, StateValue> sessionedStream = counts.toStream().transform(SuppressWindowTransformer::new, myStateStore.name());
        KStream<String, Long> printableStream = sessionedStream.map((key, value) -> new KeyValue<>(formatKey(key, value.getStartTime(), value.getEndTime())
                                                                                                , value.getCount()));
        printableStream.to("test-kstream-output-topic", Produced.with(Serdes.String(), Serdes.Long()));

        // set the required properties for running Kafka Streams
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "countSessionWindowedCustomSuppressed7");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dataplatform:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        // disable caching to see session merging
        config.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);

        // build the topology and start streaming
        Topology topology = builder.build();
        KafkaStreams streams = new KafkaStreams(topology, config);
        streams.start();

        // close Kafka Streams when the JVM shuts down (e.g. SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
