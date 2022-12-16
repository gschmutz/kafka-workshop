package com.trivadis.kafkaws.kstream.countsession;

import com.bakdata.fluent_kafka_streams_tests.junit5.TestTopologyExtension;
import org.apache.kafka.common.serialization.Serdes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.Instant;

public class KafkaStreamsRunnerCountSessionWindowedDSLTest {

    private final KafkaStreamsRunnerCountSessionWindowedDSL app = new KafkaStreamsRunnerCountSessionWindowedDSL();

    @RegisterExtension
    final TestTopologyExtension<Object, String> testTopology =
            new TestTopologyExtension<>(KafkaStreamsRunnerCountSessionWindowedDSL::getTopology, KafkaStreamsRunnerCountSessionWindowedDSL.getKafkaProperties());

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

        System.out.println(TIME1);

        System.out.println(KafkaStreamsRunnerCountSessionWindowedDSL.formatKey(KEY,TIME1, TIME1));

        this.testTopology.input()
                .add(KEY, "cat", TIME1.toEpochMilli());

        this.testTopology.streamOutput().withSerde(Serdes.String(), Serdes.Long())
                .expectNextRecord().hasKey(KafkaStreamsRunnerCountSessionWindowedDSL.formatKey(KEY,TIME1, TIME1)).hasValue(1L)
                .expectNoMoreRecord();
    }
}
