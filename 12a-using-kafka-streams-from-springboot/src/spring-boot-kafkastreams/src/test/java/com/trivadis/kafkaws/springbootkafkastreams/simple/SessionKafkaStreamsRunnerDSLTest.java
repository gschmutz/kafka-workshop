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
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.*;

import java.time.Instant;
import java.util.Properties;

import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG;

public class SessionKafkaStreamsRunnerDSLTest {

    private static Properties getProperties() {
        Properties properties = new Properties();
        properties.put(APPLICATION_ID_CONFIG, "KafkaStreamsRunnerDSLTestAggregate");
        properties.put(BOOTSTRAP_SERVERS_CONFIG, "dummy");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        return properties;
    }

    public static Topology getTopology() {
        // the builder is used to construct the topology
        StreamsBuilder builder = new StreamsBuilder();

        SessionKafkaStreamsRunnerDSL uot = new SessionKafkaStreamsRunnerDSL();
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

        String KEY = "A";
        Instant TIME1 = Instant.now();
        Instant TIME2 = TIME1.plusSeconds(10);

        this.testTopology.input()
                .add(KEY, "value", TIME1.toEpochMilli());

        this.testTopology.streamOutput().withSerde(Serdes.String(), Serdes.Long())
                .expectNextRecord().hasKey(SessionKafkaStreamsRunnerDSL.formatKey(KEY,TIME1,TIME1)).hasValue(1L)
                .expectNoMoreRecord();

        this.testTopology.input()
                .add(KEY, "value", TIME2.toEpochMilli());

        this.testTopology.streamOutput().withSerde(Serdes.String(), Serdes.Long())
                .expectNextRecord().hasKey(SessionKafkaStreamsRunnerDSL.formatKey(KEY,TIME1,TIME1)).hasValueSatisfying(v -> assertThat(v).isNull())
                .expectNextRecord().hasKey(SessionKafkaStreamsRunnerDSL.formatKey(KEY,TIME1,TIME2)).hasValue(2L)
                .expectNoMoreRecord();

    }

}
