package com.trivadis.kafkaws.kstream.testing;

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
        Topology topology = KafkaStreamsRunnerTestingDSL.getTopology();

        Properties props = KafkaStreamsRunnerTestingDSL.getKafkaProperties();

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
