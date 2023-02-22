package com.trivadis.kafkaws.kstream.aggregate;

import com.trivadis.kafkaws.kstream.avro.AggregatedList;
import com.trivadis.kafkaws.kstream.avro.ListItem;
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.SessionStore;

import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

public class KafkaStreamsRunnerAggregateWithAvroListDSL {

    private static <VT extends SpecificRecord> SpecificAvroSerde<VT> createSerde(String schemaRegistryUrl) {
        SpecificAvroSerde<VT> serde = new SpecificAvroSerde<>();
        Map<String, String> serdeConfig = Collections
                .singletonMap(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl);
        serde.configure(serdeConfig, false);
        return serde;
    }

    public static void main(String[] args) {
        // the builder is used to construct the topology
        StreamsBuilder builder = new StreamsBuilder();

        final String schemaRegistryUrl = "http://dataplatform:8081";
        final SpecificAvroSerde<AggregatedList> valueSerde = createSerde(schemaRegistryUrl);

        // read from the source topic, "test-kstream-input-topic"
        KStream<String, String> stream = builder.stream("test-kstream-input-topic");

        // transform the value from String to Long (this is necessary as from the console you can only produce String)
        KStream<String, Long> transformed = stream.mapValues(v -> Long.valueOf(v));

        // group by key
        KGroupedStream<String, Long> grouped = transformed.groupByKey();

        // create a tumbling window of 60 seconds
        TimeWindows tumblingWindow =
                TimeWindows.of(Duration.ofSeconds(60));
        SessionWindows sessionWindow =
                SessionWindows.with(Duration.ofSeconds(30)).grace(Duration.ofSeconds(10));

        Initializer<AggregatedList> initializer = () -> {
                            AggregatedList list = AggregatedList.newBuilder().setList(new ArrayList<>()).build();
//                            System.out.println("Initializer Called");
                            return list;
        };

        Aggregator<String, Long, AggregatedList> aggregator = (k, v, aggV) -> {
//            System.out.println("aggregator - k: " + k + "::v: " + v + "::aggV: " + aggV);
            aggV.getList().add(ListItem.newBuilder().setValue(v).build());
            return aggV;
        };

        Merger<String, AggregatedList> merger = (k, v1,v2) -> {
//            System.out.println("merge - k: " + k + "::v1: " + v1 + "::v2: " + v2);
            v1.getList().addAll(v2.getList());
            return v1;
        };

        // sum the values over the tumbling window of 60 seconds
        KTable<Windowed<String>, AggregatedList> sumOfValues = grouped
                //.windowedBy(tumblingWindow)
                .windowedBy(sessionWindow)
                .aggregate(
                        initializer,    /* initializer */
                        aggregator,
                        merger,   /* merger */
                        Materialized.<String, AggregatedList, SessionStore<Bytes, byte[]>>as("time-windowed-aggregated-list-stream-store") /* state store name */
                                .withKeySerde(Serdes.String())
                                .withValueSerde(valueSerde)
                )
                .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.unbounded()));

        // print the sumOfValues stream
        sumOfValues.toStream().print(Printed.<Windowed<String>,AggregatedList>toSysOut().withLabel("aggregatedValues"));

        // publish the sumOfValues stream
        final Serde<String> stringSerde = Serdes.String();
        KStream<Windowed<String>, AggregatedList> sumOfValuesStream = sumOfValues.toStream();
        KStream<String,String> sumStream = sumOfValuesStream.filter((k,v) -> k.key() == null).map((wk,v) -> new KeyValue<String,String>(wk.key() + " : " + wk.window().startTime().atZone(ZoneId.of("Europe/Zurich")) + " to " + wk.window().endTime().atZone(ZoneId.of("Europe/Zurich")), v.toString()));
        sumStream.to("test-kstream-output-topic", Produced.with(stringSerde, stringSerde));
        
        // set the required properties for running Kafka Streams
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "AvroListDSL");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dataplatform:9092");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);
        config.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 0);

        // build the topology and start streaming
        KafkaStreams streams = new KafkaStreams(builder.build(), config);
        streams.start();

        // close Kafka Streams when the JVM shuts down (e.g. SIGTERM)
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}