package com.trivadis.kafkaws.kstream.repartition;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Named;
import org.apache.kafka.streams.kstream.Printed;

import java.util.Properties;

public class KafkaStreamsRunnerRepartitionDSL {

    public static void main(String[] args) {
        // the builder is used to construct the topology
        StreamsBuilder builder = new StreamsBuilder();

        // read from the source topic, "test-kstream-input-topic"
        KStream<String, String> stream = builder.stream("test-kstream-input-topic");

        // read from the source topic, "test-kstream-input-topic"
        KTable<String, String> table = builder.table("test-kstream-compacted-topic");

        // repartition the input stream (will write to a technical topic which will be named as "${applicationId}-<name>-repartition",
        // where "applicationId" is user-specified in StreamsConfig via parameter APPLICATION_ID_CONFIG, "<name>" is an internally
        // generated name, and "-repartition" is a fixed suffix.
        KStream<String, String> repartitioned = stream.selectKey((k,v) -> v, Named.as("select-key")).repartition();

        // join the repartitioned stream with the table
        KStream<String, String> joined = repartitioned.join(table, (s,t) -> String.join (":",s, t));

        joined.to("test-kstream-output-topic");

        // set the required properties for running Kafka Streams
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "dev1");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dataplatform:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // build the topology and start streaming
        Topology topology = builder.build();
        System.out.println(topology.describe());

        KafkaStreams streams = new KafkaStreams(topology, config);
        streams.start();

        // close Kafka Streams when the JVM shuts down (e.g. SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
