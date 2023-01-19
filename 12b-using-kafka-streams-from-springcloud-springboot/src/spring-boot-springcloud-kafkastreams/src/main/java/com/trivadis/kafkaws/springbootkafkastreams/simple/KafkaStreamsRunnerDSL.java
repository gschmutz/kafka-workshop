package com.trivadis.kafkaws.springbootkafkastreams.simple;

import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;
import org.springframework.boot.autoconfigure.kafka.StreamsBuilderFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class KafkaStreamsRunnerDSL {

    @Bean
    public Function<KStream<Void, String>, KStream<Void, String>> process() {
        return input ->
        {
            input.foreach(
                    (key, value) -> {
                        System.out.println("(From DSL) " + value);
                    });

            // transform the values to upper case
            KStream<Void, String> upperStream = input.mapValues(value -> value.toUpperCase());

            // you can also print using the `print` operator
            upperStream.print(Printed.<Void, String>toSysOut().withLabel("upperValue"));
            return upperStream;
        };
    }


}
