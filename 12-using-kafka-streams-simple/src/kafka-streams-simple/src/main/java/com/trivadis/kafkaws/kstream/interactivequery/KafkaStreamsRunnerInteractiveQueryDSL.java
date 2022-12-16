package com.trivadis.kafkaws.kstream.interactivequery;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;

import java.util.Properties;

import static java.lang.Thread.*;

public class KafkaStreamsRunnerInteractiveQueryDSL {

    public static void main(String[] args) throws InterruptedException {
        // the builder is used to construct the topology
        StreamsBuilder builder = new StreamsBuilder();

        // read from the source topic, "test-kstream-input-topic"
        KStream<String, String> stream = builder.stream("test-kstream-input-topic");

        KGroupedStream<String,String> groupedByKey = stream.groupByKey();
        KTable<String, Long> counts = groupedByKey.count(Materialized.as("count"));

        counts.toStream().print(Printed.<String, Long>toSysOut().withLabel("counts"));

        final Serde<String> stringSerde = Serdes.String();
        final Serde<Long> longSerde = Serdes.Long();
        counts.toStream().to("test-kstream-output-topic", Produced.with(stringSerde, longSerde));

        // set the required properties for running Kafka Streams
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "count");
        config.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/count");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dataplatform:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);

        // build the topology and start streaming
        Topology topology = builder.build();
        System.out.println(topology.describe());
        KafkaStreams streams = new KafkaStreams(topology, config);
        streams.start();

        // close Kafka Streams when the JVM shuts down (e.g. SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

        sleep(20000);

        System.out.println ("******* Interactive Query *******");

        // Get the value by key
        ReadOnlyKeyValueStore<String, Long> keyValueStore = streams.store(StoreQueryParameters.fromNameAndType("count", QueryableStoreTypes.keyValueStore()));
        System.out.println("Querying State Store for key A:" + keyValueStore.get("AAA"));

        // Get the values for all of the keys available in this application instance
        KeyValueIterator<String, Long> range = keyValueStore.all();
        while (range.hasNext()) {
            KeyValue<String, Long> next = range.next();
            System.out.println("count for " + next.key + ": " + next.value);
        }
        range.close();
    }
}