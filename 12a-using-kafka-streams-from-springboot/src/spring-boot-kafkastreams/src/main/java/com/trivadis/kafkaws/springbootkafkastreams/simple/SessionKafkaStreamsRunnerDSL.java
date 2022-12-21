package com.trivadis.kafkaws.springbootkafkastreams.simple;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@Configuration
public class SessionKafkaStreamsRunnerDSL {

    private static final Logger LOG = LoggerFactory.getLogger(SessionKafkaStreamsRunnerDSL.class);

    private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy hh:mm.ss")
            .withZone(ZoneId.systemDefault());

    public static String formatKey (String key, Instant startTime, Instant endTime) {
        return key + "@" + formatter.format(startTime) + "->" + formatter.format(endTime);
    }

    @Autowired
    private StreamsBuilderFactoryBean factoryBean;

    private static final String INPUT_TOPIC_NAME = "test-kstream-spring-input-topic";
    private static final String OUTPUT_TOPIC_NAME = "test-kstream-spring-output-topic";
    @Bean
    @Profile("session-suppressed")
    public KStream buildPipeline (StreamsBuilder kStreamBuilder) {
        KStream<String, String> stream = kStreamBuilder.stream(INPUT_TOPIC_NAME, Consumed.with(Serdes.String(), Serdes.String()));
        // create a session window with an inactivity gap to 30 seconds and a Grace period of 10 seconds
        SessionWindows sessionWindow =
                SessionWindows.ofInactivityGapAndGrace(Duration.ofSeconds(30), Duration.ofSeconds(0));

        KTable<Windowed<String>, Long> counts = stream.groupByKey()
                .windowedBy(sessionWindow)
                .count(Materialized.as("countSessionWindowed"));

        KStream<Windowed<String>, Long> sessionedStream = counts.toStream();

        KStream<String, Long> printableStream = sessionedStream.map((key, value) -> new KeyValue<>(SessionKafkaStreamsRunnerDSL.formatKey(key.key(), key.window().startTime(), key.window().endTime()), value));

        // you can also print using the `print` operator
        printableStream.print(Printed.<String, Long>toSysOut().withLabel("session"));

        printableStream.to(OUTPUT_TOPIC_NAME, Produced.with(Serdes.String(), Serdes.Long()));

        return printableStream;

    }

}
