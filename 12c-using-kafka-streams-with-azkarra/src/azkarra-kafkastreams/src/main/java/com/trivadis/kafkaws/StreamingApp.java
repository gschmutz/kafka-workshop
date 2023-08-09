package com.trivadis.kafkaws;

import io.streamthoughts.azkarra.api.annotations.Component;
import io.streamthoughts.azkarra.api.config.Conf;
import io.streamthoughts.azkarra.api.config.Configurable;
import io.streamthoughts.azkarra.api.streams.TopologyProvider;
import io.streamthoughts.azkarra.streams.AzkarraApplication;
import io.streamthoughts.azkarra.streams.autoconfigure.annotations.AzkarraStreamsApplication;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.*;

import java.time.Duration;
import java.time.ZoneId;
/**
 * Azkarra Streams Application
 *
 * <p>For a tutorial how to write a Azkarra Streams application, check the
 * tutorials and examples on the <a href="https://www.azkarrastreams.io/docs/">Azkarra Website</a>.
 * </p>
 */
@AzkarraStreamsApplication
public class StreamingApp {

    public static void main(final String[] args) {
        AzkarraApplication.run(StreamingApp.class, args);
    }

    @Component
    public static class TumbleWindowCountTopologyProvider implements TopologyProvider, Configurable {

        private String topicSource;
        private String topicSink;
        private String stateStoreName;

        @Override
        public void configure(final Conf conf) {
            topicSource = conf.getOptionalString("topic.source").orElse("test-kstream-azkarra-input-topic");
            topicSink = conf.getOptionalString("topic.sink").orElse("test-kstream-azkarra-output-topic");
            stateStoreName = conf.getOptionalString("state.store.name").orElse("count");
        }

        @Override
        public String version() {
            return Version.getVersion();
        }

        @Override
        public Topology topology() {
            final StreamsBuilder builder = new StreamsBuilder();

            final KStream<String, String> stream = builder.stream(topicSource);

            // create a tumbling window of 60 seconds
            TimeWindows tumblingWindow = TimeWindows.of(Duration.ofSeconds(60));

            KTable<Windowed<String>, Long> counts = stream.groupByKey()
                    .windowedBy(tumblingWindow)
                    .count(Materialized.as(stateStoreName));

            counts.toStream().print(Printed.<Windowed<String>, Long>toSysOut().withLabel("counts"));

            final Serde<String> stringSerde = Serdes.String();
            final Serde<Long> longSerde = Serdes.Long();
            counts.toStream( (wk,v) -> wk.key() + ":" + wk.window().startTime().atZone(ZoneId.of("Europe/Zurich")) + " to " + wk.window().endTime().atZone(ZoneId.of("Europe/Zurich"))).to(topicSink, Produced.with(stringSerde, longSerde));

            return builder.build();
        }
    }
}