package com.trivadis.kafkaws.kstream.aggregate;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Properties;

public class KafkaStreamsRunnerAggregateDSL {

    public static void main(String[] args) {
        // the builder is used to construct the topology
        StreamsBuilder builder = new StreamsBuilder();

        // read from the source topic, "test-kstream-input-topic"
        KStream<String, String> stream = builder.stream("test-kstream-input-topic");

        // transform the value from String to Long (this is necessary as from the console you can only produce String)
        KStream<String, Long> transformed = stream.mapValues(v -> Long.valueOf(v));

        // group by key
        KGroupedStream<String, Long> grouped = transformed.groupByKey();

        // create a tumbling window of 60 seconds
        TimeWindows tumblingWindow =
                TimeWindows.of(Duration.ofSeconds(60));

        // sum the values over the tumbling window of 60 seconds
        KTable<Windowed<String>, Long> sumOfValues = grouped
                .windowedBy(tumblingWindow)
                .aggregate(
                        () -> 0L,    /* initializer */
                        (aggKey, newValue, aggValue) -> aggValue + newValue, /* adder */
                        Materialized.<String, Long, WindowStore<Bytes, byte[]>>as("time-windowed-aggregated-stream-store") /* state store name */
                                .withValueSerde(Serdes.Long())
                );

        // print the sumOfValues stream
        sumOfValues.toStream().print(Printed.<Windowed<String>,Long>toSysOut().withLabel("sumOfValues"));

        // publish the sumOfValues stream
        final Serde<String> stringSerde = Serdes.String();
        final Serde<Long> longSerde = Serdes.Long();
        sumOfValues.toStream( (wk,v) -> wk.key() + " : " + wk.window().startTime().atZone(ZoneId.of("Europe/Zurich")) + " to " + wk.window().endTime().atZone(ZoneId.of("Europe/Zurich")))
                .to("test-kstream-output-topic", Produced.with(stringSerde, longSerde));
        
        // set the required properties for running Kafka Streams
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "count");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dataplatform:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // build the topology and start streaming
        KafkaStreams streams = new KafkaStreams(builder.build(), config);
        streams.start();

        // close Kafka Streams when the JVM shuts down (e.g. SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}