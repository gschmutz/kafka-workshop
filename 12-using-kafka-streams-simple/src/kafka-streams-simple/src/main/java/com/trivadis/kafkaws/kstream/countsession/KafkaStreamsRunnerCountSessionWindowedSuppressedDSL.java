package com.trivadis.kafkaws.kstream.countsession;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.SessionStore;
import org.apache.kafka.streams.state.WindowStore;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import static org.apache.kafka.streams.kstream.Suppressed.BufferConfig.unbounded;

public class KafkaStreamsRunnerCountSessionWindowedSuppressedDSL {
    private static final Serde<String> STRING_SERDE = Serdes.String();
    private static final Serde<Long> LONG_SERDE = Serdes.Long();

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm.ss")
            .withZone(ZoneId.systemDefault());

    public static String formatKey (String key, Instant startTime, Instant endTime) {
        return key + "@" + formatter.format(startTime) + "->" + formatter.format(startTime);
    }

    public static void main(String[] args) {
        // the builder is used to construct the topology
        StreamsBuilder builder = new StreamsBuilder();

        Serde<String> stringSerde = Serdes.String();
        Serde<Long> longSerde = Serdes.Long();

        // read from the source topic, "test-kstream-input-topic"
        KStream<String, String> stream = builder.stream("test-kstream-input-topic");

        // create a session window with an inactivity gap to 15 seconds and 30 seconds grace period
        SessionWindows sessionWindow =
                SessionWindows.ofInactivityGapAndGrace(Duration.ofSeconds(15), Duration.ofSeconds(30));

        KTable<Windowed<String>, Long> counts = stream.groupByKey()
                .windowedBy(sessionWindow)
                .count(Materialized.<String, Long, SessionStore<Bytes, byte[]>>as("counts")
                        .withKeySerde(Serdes.String())
                        .withCachingDisabled());

        KStream<Windowed<String>, Long> sessionedStream = counts.suppress(Suppressed.untilWindowCloses(unbounded())).toStream();

        KStream<String, String> printableStream = sessionedStream.map((key, value) -> new KeyValue<>(formatKey(key.key(), key.window().startTime(), key.window().endTime())
                                                                                                        , String.valueOf(value)));
        printableStream.to("test-kstream-output-topic", Produced.with(Serdes.String(), Serdes.String()));

        // set the required properties for running Kafka Streams
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "countSessionWindowedWithGrace");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dataplatform:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        config.put(StreamsConfig.BUFFERED_RECORDS_PER_PARTITION_CONFIG, 1);
        config.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        config.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 1);
        config.put(StreamsConfig.POLL_MS_CONFIG, 1);
        config.put(StreamsConfig.WINDOW_STORE_CHANGE_LOG_ADDITIONAL_RETENTION_MS_CONFIG, 1);

        // build the topology and start streaming
        Topology topology = builder.build();
        KafkaStreams streams = new KafkaStreams(topology, config);
        streams.start();

        // close Kafka Streams when the JVM shuts down (e.g. SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}