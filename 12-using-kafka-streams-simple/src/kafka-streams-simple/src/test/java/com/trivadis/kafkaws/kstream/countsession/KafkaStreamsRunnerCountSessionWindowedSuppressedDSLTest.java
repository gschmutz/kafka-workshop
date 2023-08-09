package com.trivadis.kafkaws.kstream.countsession;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.TestOutputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.test.TestRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.in;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class KafkaStreamsRunnerCountSessionWindowedSuppressedDSLTest {

    private TopologyTestDriver testDriver ;
    private TestInputTopic<String, String> inputTopic;
    private TestOutputTopic<String, String> outputTopic;

    private Properties props = KafkaStreamsRunnerCountSessionWindowedSuppressedDSL.getKafkaProperties();;

    private final String KEY_A = "A";
    private final String KEY_B = "B";
    private final Instant TS_09_00_00 = Instant.parse("2022-02-22T09:00:00.000Z");
    private final Instant TS_09_00_15 = Instant.parse("2022-02-22T09:00:15.000Z");
    private final Instant TS_09_00_16 = Instant.parse("2022-02-22T09:00:16.000Z");
    private final Instant TS_09_00_17 = Instant.parse("2022-02-22T09:00:17.000Z");
    private final Instant TS_09_00_45 = Instant.parse("2022-02-22T09:00:45.000Z");
    private final Instant TS_09_00_46 = Instant.parse("2022-02-22T09:00:46.000Z");
    private final Instant TS_09_00_47 = Instant.parse("2022-02-22T09:00:47.000Z");
    private final Instant TS_09_00_48 = Instant.parse("2022-02-22T09:00:48.000Z");
    private final Instant TS_09_00_51 = Instant.parse("2022-02-22T09:00:51.000Z");
    private final Instant TS_09_01_00 = Instant.parse("2022-02-22T09:01:00.000Z");
    private final Instant TS_END = Instant.parse("2022-02-22T09:01:32.000Z");

    @BeforeEach
    void setup() {
    }

    @AfterEach
    void stop() {
        //this.testTopology.stop();
    }


    // 10
    @Test
    void test30SecGapWithNoGracePeriod_AllInOrder() {
        // test a session window with an inactivity gap to 30 seconds and 15 seconds grace period
        Topology topology = KafkaStreamsRunnerCountSessionWindowedSuppressedDSL.getTopology(30,0);
        testDriver = new TopologyTestDriver(topology, props);
        inputTopic = testDriver.createInputTopic("test-kstream-input-topic", Serdes.String().serializer(), Serdes.String().serializer());
        outputTopic = testDriver.createOutputTopic("test-kstream-output-topic", Serdes.String().deserializer(), Serdes.String().deserializer());

        // start at 09:00:00
        inputTopic.pipeInput(KEY_A, "cat", TS_09_00_00.toEpochMilli());
        // still within inactivity gap
        inputTopic.pipeInput(KEY_A, "dog", TS_09_00_15.toEpochMilli());
        // after activity gap so window will close with 09:00:15 and new one starts with 09:00:46
        inputTopic.pipeInput(KEY_A, "foo", TS_09_00_46.toEpochMilli());
        // still within inactivity gap
        inputTopic.pipeInput(KEY_A, "foo", TS_09_00_47.toEpochMilli());
        // end previous session window
        inputTopic.pipeInput(KEY_A, "foo", TS_END.toEpochMilli());

        List<TestRecord<String, String>> outRecords =
                outputTopic.readRecordsToList();
        // two session windows
        assertThat(outRecords).hasSize(2);
        assertEquals(outRecords.get(0).value(), "2");
        assertEquals(outRecords.get(1).value(), "2");

        System.out.println(outRecords.get(0));
        System.out.println(outRecords.get(1));
    }

    // 15
    @Test
    void test30SecGapWithNoGracePeriod_WithLateArrival() {
        // test a session window with an inactivity gap to 30 seconds and 0 seconds grace period
        Topology topology = KafkaStreamsRunnerCountSessionWindowedSuppressedDSL.getTopology(30,0);
        testDriver = new TopologyTestDriver(topology, props);
        inputTopic = testDriver.createInputTopic("test-kstream-input-topic", Serdes.String().serializer(), Serdes.String().serializer());
        outputTopic = testDriver.createOutputTopic("test-kstream-output-topic", Serdes.String().deserializer(), Serdes.String().deserializer());

        // start at 09:00:00
        inputTopic.pipeInput(KEY_A, "cat", TS_09_00_00.toEpochMilli());
        // still within inactivity gap
        inputTopic.pipeInput(KEY_A, "dog", TS_09_00_15.toEpochMilli());
        // after activity gap so window will close with 09:00:15 and new one starts with 09:00:47
        inputTopic.pipeInput(KEY_A, "foo", TS_09_00_47.toEpochMilli());
        // this is ignored with 09:00:16 as it is after the window has been closed and there is no grace period
        inputTopic.pipeInput(KEY_A, "foo", TS_09_00_16.toEpochMilli());
        // end previous session window
        inputTopic.pipeInput(KEY_A, "foo", TS_END.toEpochMilli());

        List<TestRecord<String, String>> outRecords =
                outputTopic.readRecordsToList();
        // two session windows
        assertThat(outRecords).hasSize(2);
        assertEquals(outRecords.get(0).value(), "2");
        assertEquals(outRecords.get(1).value(), "1");

        System.out.println(outRecords.get(0));
        System.out.println(outRecords.get(1));
    }

    // 20
    @Test
    void test30SecGapWithNoGracePeriod_WithSecondKey() {
        // test a session window with an inactivity gap to 30 seconds and 0 seconds grace period
        Topology topology = KafkaStreamsRunnerCountSessionWindowedSuppressedDSL.getTopology(30,0);
        testDriver = new TopologyTestDriver(topology, props);
        inputTopic = testDriver.createInputTopic("test-kstream-input-topic", Serdes.String().serializer(), Serdes.String().serializer());
        outputTopic = testDriver.createOutputTopic("test-kstream-output-topic", Serdes.String().deserializer(), Serdes.String().deserializer());

        // start at 09:00:00
        inputTopic.pipeInput(KEY_A, "cat", TS_09_00_00.toEpochMilli());
        // still within inactivity gap
        inputTopic.pipeInput(KEY_A, "dog", TS_09_00_15.toEpochMilli());
        // after activity gap with other KEY so window will close with 09:00:15 and new one starts with 09:00:47
        inputTopic.pipeInput(KEY_B, "foo", TS_09_00_47.toEpochMilli());
        // this is ignored with 09:00:16 as it is after the window has been closed and there is no grace period
        inputTopic.pipeInput(KEY_B, "foo", TS_09_00_48.toEpochMilli());
        // end previous session window
        inputTopic.pipeInput(KEY_A, "foo", TS_END.toEpochMilli());

        List<TestRecord<String, String>> outRecords =
                outputTopic.readRecordsToList();
        // two session windows
        assertThat(outRecords).hasSize(2);
        assertEquals(outRecords.get(0).value(), "2");
        assertEquals(outRecords.get(1).value(), "2");

        System.out.println(outRecords.get(0));
        System.out.println(outRecords.get(1));
    }

    // 25
    @Test
    void test30SecGapWithNoGracePeriod_WithLateArrivalSameAsWindowEnd() {
        // test a session window with an inactivity gap to 30 seconds and 0 seconds grace period
        Topology topology = KafkaStreamsRunnerCountSessionWindowedSuppressedDSL.getTopology(30,0);
        testDriver = new TopologyTestDriver(topology, props);
        inputTopic = testDriver.createInputTopic("test-kstream-input-topic", Serdes.String().serializer(), Serdes.String().serializer());
        outputTopic = testDriver.createOutputTopic("test-kstream-output-topic", Serdes.String().deserializer(), Serdes.String().deserializer());

        // start at 09:00:00
        inputTopic.pipeInput(KEY_A, "cat", TS_09_00_00.toEpochMilli());
        // still within inactivity gap
        inputTopic.pipeInput(KEY_A, "dog", TS_09_00_15.toEpochMilli());
        // after activity gap so window will close with 09:00:15 and new one starts with 09:00:47
        inputTopic.pipeInput(KEY_A, "foo", TS_09_00_47.toEpochMilli());
        // this is ignored with 09:00:15 as it is after the window has been closed and there is no grace period
        inputTopic.pipeInput(KEY_A, "foo", TS_09_00_15.toEpochMilli());
        // end previous session window
        inputTopic.pipeInput(KEY_A, "foo", TS_END.toEpochMilli());

        List<TestRecord<String, String>> outRecords =
                outputTopic.readRecordsToList();
        // two session windows
        assertThat(outRecords).hasSize(2);
        assertEquals(outRecords.get(0).value(), "2");
        assertEquals(outRecords.get(1).value(), "1");

        System.out.println(outRecords.get(0));
        System.out.println(outRecords.get(1));
    }

    // 30
    @Test
    void test30SecGapWith15SecGracePeriod_WithLateArrival() {
        // test a session window with an inactivity gap to 30 seconds and 15 seconds grace period
        Topology topology = KafkaStreamsRunnerCountSessionWindowedSuppressedDSL.getTopology(30,15);
        testDriver = new TopologyTestDriver(topology, props);
        inputTopic = testDriver.createInputTopic("test-kstream-input-topic", Serdes.String().serializer(), Serdes.String().serializer());
        outputTopic = testDriver.createOutputTopic("test-kstream-output-topic", Serdes.String().deserializer(), Serdes.String().deserializer());

        // start at 09:00:00
        inputTopic.pipeInput(KEY_A, "cat", TS_09_00_00.toEpochMilli());
        // still within inactivity gap
        inputTopic.pipeInput(KEY_A, "dog", TS_09_00_15.toEpochMilli());
        // after activity gap so window will close with 09:00:15 and new one starts with 09:00:47
        inputTopic.pipeInput(KEY_A, "foo", TS_09_00_47.toEpochMilli());
        // this is still within grace period (window was closed at 09:00:15, by that window end is extended to 09:00:47 (9:00:17 + 30s) and previous event included in the session
        inputTopic.pipeInput(KEY_A, "foo", TS_09_00_17.toEpochMilli());
        // end previous session window
        inputTopic.pipeInput(KEY_A, "foo", TS_END.toEpochMilli());

        List<TestRecord<String, String>> outRecords =
                outputTopic.readRecordsToList();
        // two session windows
        assertThat(outRecords).hasSize(1);
        assertEquals(outRecords.get(0).value(), "4");

        System.out.println(outRecords.get(0));
    }

    // 31
    @Test
    void test30SecGapWith0SecGracePeriod_WithLateArrival() {
        // test a session window with an inactivity gap to 30 seconds and 0 seconds grace period
        Topology topology = KafkaStreamsRunnerCountSessionWindowedSuppressedDSL.getTopology(30,0);
        testDriver = new TopologyTestDriver(topology, props);
        inputTopic = testDriver.createInputTopic("test-kstream-input-topic", Serdes.String().serializer(), Serdes.String().serializer());
        outputTopic = testDriver.createOutputTopic("test-kstream-output-topic", Serdes.String().deserializer(), Serdes.String().deserializer());

        // start at 09:00:00
        inputTopic.pipeInput(KEY_A, "cat", TS_09_00_00.toEpochMilli());
        // still within inactivity gap
        inputTopic.pipeInput(KEY_A, "dog", TS_09_00_15.toEpochMilli());
        // after activity gap so window will close with 09:00:15 and new one starts with 09:00:47
        //inputTopic.advanceTime(Duration.ofSeconds(30));
        inputTopic.pipeInput(KEY_A, "foo", TS_09_00_46.toEpochMilli());
        // this is still within grace period (window was closed at 09:00:15, by that window end is extended to 09:00:47 (9:00:17 + 30s) and previous event included in the session
        inputTopic.pipeInput(KEY_A, "foo", TS_09_00_17.toEpochMilli());
        inputTopic.pipeInput(KEY_A, "foo", TS_09_00_47.toEpochMilli());

        // end previous session window
        inputTopic.pipeInput(KEY_A, "foo", TS_END.toEpochMilli());

        List<TestRecord<String, String>> outRecords =
                outputTopic.readRecordsToList();
        System.out.println(outRecords.get(0));
        System.out.println(outRecords.get(1));

        // two session windows
        assertThat(outRecords).hasSize(1);
        assertEquals(outRecords.get(0).value(), "4");

        System.out.println(outRecords.get(0));
    }


    // 35
    @Test
    void test30SecGapWith15SecGracePeriod_WithLateArrivalSameAsEndOfWindow() {
        // test a session window with an inactivity gap to 30 seconds and 15 seconds grace period
        Topology topology = KafkaStreamsRunnerCountSessionWindowedSuppressedDSL.getTopology(30,15);
        testDriver = new TopologyTestDriver(topology, props);
        inputTopic = testDriver.createInputTopic("test-kstream-input-topic", Serdes.String().serializer(), Serdes.String().serializer());
        outputTopic = testDriver.createOutputTopic("test-kstream-output-topic", Serdes.String().deserializer(), Serdes.String().deserializer());

        // start at 09:00:00
        inputTopic.pipeInput(KEY_A, "cat", TS_09_00_00.toEpochMilli());
        // still within inactivity gap
        inputTopic.pipeInput(KEY_A, "dog", TS_09_00_15.toEpochMilli());
        // after activity gap so window will close with 09:00:15 and new one starts with 09:00:47
        inputTopic.pipeInput(KEY_A, "foo", TS_09_00_47.toEpochMilli());
        // this is still within grace period (window was closed at 09:00:15, by that window end is not extended and stays at 09:00:45 (9:00:15 + 30s)
        inputTopic.pipeInput(KEY_A, "foo", TS_09_00_15.toEpochMilli());
        // end previous session window
        inputTopic.pipeInput(KEY_A, "foo", TS_END.toEpochMilli());

        List<TestRecord<String, String>> outRecords =
                outputTopic.readRecordsToList();
        // two session windows
        assertThat(outRecords).hasSize(2);
        assertEquals(outRecords.get(0).value(), "3");
        assertEquals(outRecords.get(1).value(), "1");

        System.out.println(outRecords.get(0));
        System.out.println(outRecords.get(1));
    }
}
