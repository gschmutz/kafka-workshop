package com.trivadis.kafkaws.springbootkafkastreams.simple;

import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@Configuration
@EnableKafkaStreams
public class KafkaStreamsRunnerDSL {

    private static final String INPUT_TOPIC_NAME = "test-kstream-spring-input-topic";
    private static final String OUTPUT_TOPIC_NAME = "test-kstream-spring-output-topic";

    @Bean
    public KStream<Void, String> kStream(StreamsBuilder kStreamBuilder) {
        KStream<Void, String> stream = kStreamBuilder.stream(INPUT_TOPIC_NAME);
        stream.foreach(
                (key, value) -> {
                    System.out.println("(From DSL) " + value);
                });
        // transform the values to upper case
        KStream<Void, String> upperStream = stream.mapValues(value -> value.toUpperCase());

        // you can also print using the `print` operator
        upperStream.print(Printed.<Void, String>toSysOut().withLabel("upperValue"));

        upperStream.to(OUTPUT_TOPIC_NAME);

        return stream;
    }

}
