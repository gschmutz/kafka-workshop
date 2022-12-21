package com.trivadis.kafkaws.springbootkafkastreams.simple;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.StreamsBuilderFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.KafkaStreamsCustomizer;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
//@EnableKafka
//@EnableKafkaStreams
@Configuration
//@SpringBootApplication(scanBasePackages = {"com.trivadis.kafkaws.springbootkafkastreams.simple"})
public class KafkaStreamsRunnerDSL {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaStreamsRunnerDSL.class);

    @Autowired
    private StreamsBuilderFactoryBean factoryBean;

    private static final String INPUT_TOPIC_NAME = "test-kstream-spring-input-topic";
    private static final String OUTPUT_TOPIC_NAME = "test-kstream-spring-output-topic";
    @Bean
    @Profile("upper")
    public KStream buildPipeline (StreamsBuilder kStreamBuilder) {
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
        return upperStream;
    }

    @Bean
    KafkaStreamsCustomizer getKafkaStreamsCustomizer() {
        return kafkaStreams -> kafkaStreams.setStateListener((newState, oldState) -> {
            if (newState == KafkaStreams.State.RUNNING) {
                LOG.info("Streams now in running state");
                kafkaStreams.localThreadsMetadata().forEach(tm -> LOG.info("{} assignments {}", tm.threadName(), tm.activeTasks()));
            }
        });
    }


    @Bean
    StreamsBuilderFactoryBeanCustomizer kafkaStreamsCustomizer() {
        return  streamsFactoryBean -> streamsFactoryBean.setKafkaStreamsCustomizer(getKafkaStreamsCustomizer());
    }

}
