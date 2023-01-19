package com.trivadis.kafkaws.springbootkafkastreams.simple;

import com.bakdata.fluent_kafka_streams_tests.junit5.TestTopologyExtension;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Properties;

import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG;

public class KafkaStreamsRunnerDSLTest2 {

    private static Properties getProperties() {
        Properties properties = new Properties();
        properties.put(APPLICATION_ID_CONFIG, "KafkaStreamsRunnerDSLTest");
        properties.put(BOOTSTRAP_SERVERS_CONFIG, "dummy");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        return properties;
    }

    public static Topology getTopology() {
        // the builder is used to construct the topology
        StreamsBuilder builder = new StreamsBuilder();

        KafkaStreamsRunnerDSL uot = new KafkaStreamsRunnerDSL();
        uot.buildPipeline(builder);

        // build the topology and start streaming
        Topology topology = builder.build();
        return topology;
    }

    @RegisterExtension
    final TestTopologyExtension<Object, String> testTopology =
            new TestTopologyExtension<>(getTopology(), getProperties());

    @BeforeEach
    void start() {
        this.testTopology.start();
    }

    @AfterEach
    void stop() {
        //this.testTopology.stop();
    }

    @Test
    void shouldAggregateSameWordStream() {

        String KEY = null;

        this.testTopology.input()
                .add(KEY, "cat");

        this.testTopology.streamOutput().withSerde(Serdes.Void(), Serdes.String())
                .expectNextRecord().hasValue("CAT")
                .expectNoMoreRecord();
    }

}
