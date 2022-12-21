package com.trivadis.kafkaws.kstream.countsession;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;


public class KafkaStreamsRunnerCountSessionWindowedDSL {

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm.ss")
            .withZone(ZoneId.systemDefault());

    public static String formatKey (String key, Instant startTime, Instant endTime) {
        return key + "@" + formatter.format(startTime) + "->" + formatter.format(startTime);
    }

    public static void main(final String[] args) {
        final KafkaStreams streams = new KafkaStreams(getTopology(), getKafkaProperties());
        streams.cleanUp();
        streams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    public static Properties getKafkaProperties() {
        // set the required properties for running Kafka Streams
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "countSessionWindowedWithGrace3");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dataplatform:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        // disable caching to see session merging
        config.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);

        return config;
    }

    public static Topology getTopology() {
        // the builder is used to construct the topology
        StreamsBuilder builder = new StreamsBuilder();
        // read from the source topic, "test-kstream-input-topic"
        KStream<String, String> stream = builder.stream("test-kstream-input-topic");

        // create a session window with an inactivity gap to 30 seconds and a Grace period of 10 seconds
        SessionWindows sessionWindow =
                SessionWindows.with(Duration.ofSeconds(30)).grace(Duration.ofSeconds(10));

        KTable<Windowed<String>, Long> counts = stream.groupByKey()
                .windowedBy(sessionWindow)
                .count(Materialized.as("countSessionWindowed"));

        KStream<Windowed<String>, Long> sessionedStream = counts.toStream();

        KStream<String, Long> printableStream = sessionedStream.map((key, value) -> new KeyValue<>(KafkaStreamsRunnerCountSessionWindowedDSL.formatKey(key.key(), key.window().startTime(), key.window().endTime()), value));

        // you can also print using the `print` operator
        printableStream.print(Printed.<String, Long>toSysOut().withLabel("session"));

        printableStream.to("test-kstream-output-topic", Produced.with(Serdes.String(), Serdes.Long()));

        // build the topology and start streaming
        Topology topology = builder.build();
        return topology;
    }
}