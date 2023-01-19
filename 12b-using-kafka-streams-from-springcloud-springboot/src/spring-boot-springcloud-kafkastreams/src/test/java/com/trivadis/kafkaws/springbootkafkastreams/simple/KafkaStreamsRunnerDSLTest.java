package com.trivadis.kafkaws.springbootkafkastreams.simple;

import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.test.TestUtils;
import org.junit.jupiter.api.*;

import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

public class KafkaStreamsRunnerDSLTest {

    private TopologyTestDriver testDriver;
    private static final String INPUT_TOPIC = "input-topic";
    private static final String OUTPUT_TOPIC = "output-topic";;
    private TestInputTopic inputTopic;
    private TestOutputTopic outputTopic;

    final Serde<String> stringSerde = Serdes.String();
    final Serde<Void> voidSerde = Serdes.Void();

    public static Properties getStreamsConfig(final String applicationId,
                                              final String bootstrapServers,
                                              final String keySerdeClassName,
                                              final String valueSerdeClassName,
                                              final Properties additional) {

        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationId);
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, keySerdeClassName);
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, valueSerdeClassName);
        props.put(StreamsConfig.STATE_DIR_CONFIG, TestUtils.tempDirectory().getPath());
//        props.put(StreamsConfig.METRICS_RECORDING_LEVEL_CONFIG, "DEBUG");
        props.putAll(additional);
        return props;
    }

    public static Properties getStreamsConfig(final Serde<?> keyDeserializer,
                                              final Serde<?> valueDeserializer) {
        return getStreamsConfig(
                UUID.randomUUID().toString(),
                "localhost:9091",
                keyDeserializer.getClass().getName(),
                valueDeserializer.getClass().getName(),
                new Properties());
    }

    /**
     * Setup Stream topology
     * Add KStream based on @StreamListener annotation
     * Add to(topic) based @SendTo annotation
     */
    @BeforeEach
    public void setup() {
        final StreamsBuilder builder = new StreamsBuilder();
        buildStreamProcessingPipeline(builder);

        final Properties props = getStreamsConfig(Serdes.Void(), Serdes.String());
        testDriver = new TopologyTestDriver(builder.build(), props);
        inputTopic = testDriver.createInputTopic(INPUT_TOPIC, voidSerde.serializer(), stringSerde.serializer());
        outputTopic = testDriver.createOutputTopic(OUTPUT_TOPIC, voidSerde.deserializer(), stringSerde.deserializer());
    }

    private void buildStreamProcessingPipeline(StreamsBuilder builder) {
        KStream<Void, String> input = builder.stream(INPUT_TOPIC, Consumed.with(voidSerde, stringSerde));
        KafkaStreamsRunnerDSL app = new KafkaStreamsRunnerDSL();
        final Function<KStream<Void, String>, KStream<Void, String>> process = app.process();
        final KStream<Void, String> output = process.apply(input);
        output.to(OUTPUT_TOPIC, Produced.with(voidSerde, stringSerde));
    }

    @AfterEach
    public void tearDown() {
        try {
            testDriver.close();
        } catch (RuntimeException e) {
            // https://issues.apache.org/jira/browse/KAFKA-6647 causes exception when executed in Windows, ignoring it
            // Logged stacktrace cannot be avoided
            System.out.println("Ignoring exception, test failing in Windows due this exception:" + e.getLocalizedMessage());
        }
    }

    @Test
    void shouldUpperCase() {

        final Void voidKey = null;
        String KEY = null;

        inputTopic.pipeInput(voidKey, "Hello", 1L);

        final Object output = outputTopic.readValue();
        // assert that the output has a value
        assertThat(output).isNotNull();
        assertThat(output).isEqualTo("HELLO");
        // no more data in topic
        assertThat(outputTopic.isEmpty()).isTrue();

    }

    @Test
    void shouldUpperCase2() {

        final Void voidKey = null;
        String KEY = null;

        inputTopic.pipeInput(voidKey, "Hello", 1L);

        final Object output = outputTopic.readValue();
        // assert that the output has a value
        assertThat(output).isNotNull();
        assertThat(output).isEqualTo("HELLO");
        // no more data in topic
        assertThat(outputTopic.isEmpty()).isTrue();

    }
}
