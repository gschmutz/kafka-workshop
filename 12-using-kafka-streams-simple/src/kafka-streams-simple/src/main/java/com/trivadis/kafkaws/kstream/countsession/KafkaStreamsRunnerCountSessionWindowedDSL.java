package com.trivadis.kafkaws.kstream.countsession;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.kstream.internals.SessionWindow;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Properties;

public class KafkaStreamsRunnerCountSessionWindowedDSL {

    private static Object Produced;

    public static void main(String[] args) {
        // the builder is used to construct the topology
        StreamsBuilder builder = new StreamsBuilder();

        // read from the source topic, "test-kstream-input-topic"
        KStream<String, String> stream = builder.stream("test-kstream-input-topic");

        // create a tumbling window of 60 seconds
        SessionWindows sessionWindow =
                SessionWindows.with(Duration.ofSeconds(30));

        KTable<Windowed<String>, Long> counts = stream.groupByKey()
                .windowedBy(sessionWindow)
                .count(Materialized.as("coundSessionWindowed"));

        counts.toStream().print(Printed.<Windowed<String>, Long>toSysOut().withLabel("counts"));

        counts.toStream( (wk,v) -> wk.key() + " : " + wk.window().startTime().atZone(ZoneId.of("Europe/Zurich")) + " to " + wk.window().endTime().atZone(ZoneId.of("Europe/Zurich")) + " : " + v )
                .to("test-kstream-output-topic");

        // set the required properties for running Kafka Streams
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "countSessionWindowed");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dataplatform:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);

        // build the topology and start streaming
        Topology topology = builder.build();
        KafkaStreams streams = new KafkaStreams(topology, config);
        streams.start();

        // close Kafka Streams when the JVM shuts down (e.g. SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}