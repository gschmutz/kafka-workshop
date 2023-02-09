package com.trivadis.kafkaws.kstream.customstate;

import com.trivadis.kafkaws.kstream.countsession.StateValue;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.processor.api.*;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import java.util.*;

public class KafkaStreamsRunnerCustomStateWithProcessorDSL {

    public static final String MY_STATE_STORE = "MyStateStore";

    public static void main(String[] args) {
        // the builder is used to construct the topology
        StreamsBuilder builder = new StreamsBuilder();

        // create custom state store
        final StoreBuilder<KeyValueStore<String, List<String>>> myStateStore = Stores
                .keyValueStoreBuilder(Stores.persistentKeyValueStore(MY_STATE_STORE), Serdes.String(), Serdes.ListSerde(ArrayList.class, Serdes.String()))
                .withCachingEnabled();
        builder.addStateStore(myStateStore);

        // read from the source topic, "test-kstream-input-topic"
        KStream<String, String> stream = builder.stream("test-kstream-input-topic");

        // invoke the transformer
        KStream<String, List<String>> transformedStream = stream.process(new MyProcessorSupplier(myStateStore), myStateStore.name());

        // peek into the stream and execute a println
        transformedStream.peek((k,v) -> System.out.println("key: " + k + " - value:" + v));

        // publish result
        transformedStream.mapValues(v -> v.toString()).to("test-kstream-output-topic");

        // set the required properties for running Kafka Streams
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "customstate2");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dataplatform:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // build the topology and start streaming
        KafkaStreams streams = new KafkaStreams(builder.build(), config);
        streams.start();

        // close Kafka Streams when the JVM shuts down (e.g. SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }

    private static final class MyProcessorSupplier implements ProcessorSupplier<String, String, String, List<String>> {
        private StoreBuilder<?> storeBuilder;

        public MyProcessorSupplier(StoreBuilder<?> storeBuilder) {
            this.storeBuilder = storeBuilder;
        }

        @Override
        public Processor<String, String, String, List<String>> get() {
            return new MyProcessorSupplier.MyProcessor();
        }

        @Override
        public Set<StoreBuilder<?>> stores() {
            return ProcessorSupplier.super.stores();
        }

        class MyProcessor extends ContextualProcessor<String, String, String, List<String>> {

            private KeyValueStore<String, List<String>> stateStore;

            @Override
            public void init(ProcessorContext<String, List<String>> context) {
                super.init(context);
                stateStore = (KeyValueStore) context.getStateStore(MY_STATE_STORE);

                // context().schedule(Duration.ofMillis(2000), PunctuationType.WALL_CLOCK_TIME, timestamp -> flushOldWindow(timestamp));
            }

            @Override
            public void process(Record<String, String> record) {
                if (stateStore.get(record.key()) == null) {
                    stateStore.put(record.key(), Collections.singletonList(record.value()));
                } else {
                    List entries = stateStore.get(record.key());
                    entries.add(record.value());
                    stateStore.put(record.key(), entries);
                }

                context().forward(new Record<String, List<String>>(record.key(), stateStore.get(record.key()), record.timestamp()));
            }

        }
    }
}
