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
import java.util.UUID;

import static org.apache.kafka.streams.kstream.Suppressed.BufferConfig.unbounded;

public class KafkaStreamsRunnerCountSessionWindowedSuppressedDSL {
    private static final Serde<String> STRING_SERDE = Serdes.String();
    private static final Serde<Long> LONG_SERDE = Serdes.Long();

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm.ss")
            .withZone(ZoneId.of("UTC"));

    public static String formatKey (String key, Instant startTime, Instant endTime) {
        return key + "@" + formatter.format(startTime) + "->" + formatter.format(endTime);
    }

    public static void main(String[] args) {
        final KafkaStreams streams = new KafkaStreams(getTopology(30,15), getKafkaProperties());
        streams.cleanUp();
        streams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    public static Properties getKafkaProperties() {
        // set the required properties for running Kafka Streams
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, UUID.randomUUID().toString());
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dataplatform:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        // disable caching to see session merging
        config.put(StreamsConfig.BUFFERED_RECORDS_PER_PARTITION_CONFIG, 1);
        config.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        config.put(StreamsConfig.POLL_MS_CONFIG, 1);
        config.put(StreamsConfig.WINDOW_STORE_CHANGE_LOG_ADDITIONAL_RETENTION_MS_CONFIG, 1);

        return config;
    }

    public static Topology getTopology(int gap, int grace) {

        // the builder is used to construct the topology
        StreamsBuilder builder = new StreamsBuilder();

        Serde<String> stringSerde = Serdes.String();
        Serde<Long> longSerde = Serdes.Long();

        // read from the source topic, "test-kstream-input-topic"
        KStream<String, String> stream = builder.stream("test-kstream-input-topic");

        // create a session window with an inactivity gap to <gap> seconds and <grace> seconds grace period
        SessionWindows sessionWindow = null;
        if (grace > 0) {
            sessionWindow =
                    SessionWindows.ofInactivityGapAndGrace(Duration.ofSeconds(gap), Duration.ofSeconds(grace));
        } else {
            sessionWindow =
                    SessionWindows.ofInactivityGapWithNoGrace(Duration.ofSeconds(gap));

        }
        sessionWindow =
                SessionWindows.ofInactivityGapAndGrace(Duration.ofSeconds(gap), Duration.ofSeconds(grace));

        KTable<Windowed<String>, Long> counts = stream.groupByKey()
                .windowedBy(sessionWindow)
                .count(Materialized.<String, Long, SessionStore<Bytes, byte[]>>as("counts")
                        .withKeySerde(Serdes.String())
                        .withCachingDisabled());

        KStream<Windowed<String>, Long> sessionedStream = counts.suppress(Suppressed.untilWindowCloses(unbounded())).toStream();

        KStream<String, String> printableStream = sessionedStream.map((key, value) -> new KeyValue<>(formatKey(key.key(), key.window().startTime(), key.window().endTime())
                                                                                                        , String.valueOf(value)));

        //printableStream.print(Printed.<String,String>toSysOut().withLabel("count"));
        printableStream.to("test-kstream-output-topic", Produced.with(Serdes.String(), Serdes.String()));


        // build the topology and start streaming
        Topology topology = builder.build();
        return topology;
    }
}