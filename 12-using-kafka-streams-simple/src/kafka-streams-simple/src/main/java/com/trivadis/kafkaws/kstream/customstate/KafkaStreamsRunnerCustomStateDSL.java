package com.trivadis.kafkaws.kstream.customstate;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import java.util.Properties;

public class KafkaStreamsRunnerCustomStateDSL {

    public static void main(String[] args) {
        // the builder is used to construct the topology
        StreamsBuilder builder = new StreamsBuilder();

        final StoreBuilder<KeyValueStore<String, String>> myStateStore = Stores
                .keyValueStoreBuilder(Stores.persistentKeyValueStore("MyStateStore"), Serdes.String(), Serdes.String())
                .withCachingEnabled();
        builder.addStateStore(myStateStore);

        final MyStateHandler myStateHandler = new MyStateHandler(myStateStore.name());

        // read from the source topic, "test-kstream-input-topic"
        KStream<String, String> stream = builder.stream("test-kstream-input-topic");

        // invoke the transformer
        KStream<String, String> transformedStream = stream.transform(() -> myStateHandler, myStateStore.name() );

        // peek into the stream and execute a println
        transformedStream.peek((k,v) -> System.out.println("key: " + k + " - value:" + v));

        // publish result
        transformedStream.to("test-kstream-output-topic");

        // set the required properties for running Kafka Streams
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "customstate");
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

    private static final class MyStateHandler implements Transformer<String, String, KeyValue<String, String>> {
        final private String storeName;
        private KeyValueStore<String, String> stateStore;
        private ProcessorContext context;

        public MyStateHandler(final String storeName) {
            this.storeName = storeName;
        }


        @Override
        public void init(ProcessorContext processorContext) {
            this.context = processorContext;
            stateStore = (KeyValueStore<String, String>) this.context.getStateStore(storeName);
        }

        @Override
        public KeyValue<String,String> transform(String key, String value) {
            if (stateStore.get(key) == null) {
                stateStore.put(key, value);
            } else {
                stateStore.put(key, stateStore.get(key) + "," + value);
            }

            return new KeyValue<>(key, stateStore.get(key));
        }

        @Override
        public void close() {

        }
    }
}
