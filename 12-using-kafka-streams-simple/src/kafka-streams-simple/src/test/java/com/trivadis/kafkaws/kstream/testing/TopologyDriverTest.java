package com.trivadis.kafkaws.kstream.testing;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG;
import static org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;

public class TopologyDriverTest {

    private TopologyTestDriver testDriver = null;

    private static Properties getProperties() {
        Properties properties = new Properties();
//        properties.put(APPLICATION_ID_CONFIG, "KafkaStreamsRunnerDSLTest");
//        properties.put(BOOTSTRAP_SERVERS_CONFIG, "dummy");
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        return properties;
    }

    public static Topology getTopology() {
        // the builder is used to construct the topology
        StreamsBuilder builder = new StreamsBuilder();

        KStream<Void, String> stream = builder.stream("test-kstream-spring-input-topic");
        stream.foreach(
                (key, value) -> {
                    System.out.println("(From DSL) " + value);
                });
        // transform the values to upper case
        KStream<Void, String> upperStream = stream.mapValues(value -> value.toUpperCase());

        // you can also print using the `print` operator
        upperStream.print(Printed.<Void, String>toSysOut().withLabel("upperValue"));

        upperStream.to("test-kstream-spring-output-topic");


        // build the topology and start streaming
        Topology topology = builder.build();
        return topology;
    }

    @BeforeEach
    void start() {
        testDriver = new TopologyTestDriver(getTopology(), getProperties());
    }

    @AfterEach
    void stop() {
        //this.testTopology.stop();
    }

    @Test
    void shouldAggregateSameWordStream() {

        String KEY = null;

        TestInputTopic<String, String> inputTopic = testDriver.createInputTopic(
                "test-kstream-spring-input-topic",
                new StringSerializer(),
                new StringSerializer());
        inputTopic.pipeInput(KEY, "cat");

        TestOutputTopic<String, String> outputTopic = testDriver.createOutputTopic(
                "test-kstream-spring-output-topic",
                new StringDeserializer(),
                new StringDeserializer());
        assertThat(outputTopic.readValue()).isEqualTo(("CAT"));
    }

}
