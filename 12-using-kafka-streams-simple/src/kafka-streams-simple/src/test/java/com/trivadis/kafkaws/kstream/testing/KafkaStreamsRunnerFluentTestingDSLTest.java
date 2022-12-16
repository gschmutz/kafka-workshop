package com.trivadis.kafkaws.kstream.testing;

import com.bakdata.fluent_kafka_streams_tests.junit5.TestTopologyExtension;
import com.trivadis.kafkaws.kstream.countsession.KafkaStreamsRunnerCountSessionWindowedDSL;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.test.TestRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaStreamsRunnerFluentTestingDSLTest {
    private final KafkaStreamsRunnerTestingDSL app = new KafkaStreamsRunnerTestingDSL();

    @RegisterExtension
    final TestTopologyExtension<Object, String> testTopology =
            new TestTopologyExtension<>(KafkaStreamsRunnerTestingDSL::getTopology, KafkaStreamsRunnerTestingDSL.getKafkaProperties());

    @BeforeEach
    void start() {
        this.testTopology.start();
    }

    @AfterEach
    void stop() {
        //this.testTopology.stop();
    }

    @Test
    void testUpperCase() {
        String KEY = "A";
        String KEY2 = "B";
        String VALUE = "aaa";

        this.testTopology.input()
                .add(KEY, VALUE);

        this.testTopology.streamOutput().withSerde(Serdes.String(), Serdes.String())
                .expectNextRecord().hasKey(KEY).hasValue(VALUE.toUpperCase())
                .expectNoMoreRecord();

    }
}
