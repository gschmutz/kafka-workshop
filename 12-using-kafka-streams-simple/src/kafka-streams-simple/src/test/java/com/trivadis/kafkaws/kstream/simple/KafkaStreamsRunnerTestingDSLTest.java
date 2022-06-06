package com.trivadis.kafkaws.kstream.simple;

import com.trivadis.kafkaws.kstream.testing.KafkaStreamsRunnerTestingDSL;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.test.TestRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaStreamsRunnerTestingDSLTest {
    private TopologyTestDriver testDriver;
    private TestInputTopic<Void, String> inputTopic;
    private TestOutputTopic<Void, String> outputTopic;

    @BeforeEach
    void setup() {
        Topology topology = KafkaStreamsRunnerTestingDSL.build();

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Void().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        testDriver = new TopologyTestDriver(topology, props);

        inputTopic = testDriver.createInputTopic("test-kstream-input-topic", Serdes.Void().serializer(), Serdes.String().serializer());

        outputTopic = testDriver.createOutputTopic("test-kstream-output-topic", Serdes.Void().deserializer(), Serdes.String().deserializer());
    }

    @AfterEach
    void teardown() {
        testDriver.close();
    }

    @Test
    void testUpperCase() {
        String value = "this is a test message";

        inputTopic.pipeInput(value);

        assertThat(outputTopic.isEmpty()).isFalse();

        List<TestRecord<Void, String>> outRecords =
                outputTopic.readRecordsToList();
        assertThat(outRecords).hasSize(1);

        String greeting = outRecords.get(0).getValue();
        assertThat(greeting).isEqualTo("THIS IS A TEST MESSAGE");
    }
}
