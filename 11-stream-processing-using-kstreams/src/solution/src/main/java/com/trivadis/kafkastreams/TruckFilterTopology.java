package com.trivadis.kafkastreams;

import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Printed;

public class TruckFilterTopology {

	public static void main(String[] args) {
		
		// Serializers/deserializers (serde) for String and Long types
		final Serde<String> stringSerde = Serdes.String();
		final Serde<Long> longSerde = Serdes.Long();
		
	    final String bootstrapServers = args.length > 0 ? args[0] : "streamingplatform:9092";
	    final Properties streamsConfiguration = new Properties();
	    
	    // Give the Streams application a unique name.  The name must be unique in the Kafka cluster
	    // against which the application is run.
	    streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "kafka-streams-truck-filter");

	    // Where to find Kafka broker(s).
	    streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
	    streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
	    
	    // Specify default (de)serializers for record keys and for record values.
	    streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
	    streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());		
	    		
		// In the subsequent lines we define the processing topology of the Streams application.
		// used to be KStreamBuilder ....
	    final StreamsBuilder builder = new StreamsBuilder();

		/*
		 * Consume TruckPositions data from Kafka topic
		 */
		KStream<String, String> positions = builder.stream("truck_position", Consumed.with(stringSerde, stringSerde));

		/*
		 * Create a Stream of TruckPosition's by parsing the CSV into TruckPosition instances
		 */
		KStream<String, TruckPosition> positionsTruck = positions.mapValues(value -> TruckPosition.create(value.substring(7, value.length())));
		
		positionsTruck.peek((k, v) -> System.out.println (k + ":" +v));
		
		/*
		 * Non stateful transformation => filter out normal behaviour
		 */
		KStream<String, TruckPosition> positionsTruckFiltered = positionsTruck.filter(TruckPosition::filterNonNORMAL);
		
		/*
		 * Convert the Truck Position back into a CSV format and publish to the dangerous_driving_kstreams topic
		 */
		positionsTruckFiltered.mapValues(value -> value.toCSV()).to("dangerous_driving_kstreams");
		
		// Create the topology
		final KafkaStreams streams = new KafkaStreams(builder.build(), streamsConfiguration);
		
		// clean up all local state by application-id
		streams.cleanUp();

	    streams.setUncaughtExceptionHandler((Thread thread, Throwable throwable) -> {
	    	System.out.println("Within UncaughtExceptionHandler =======>");
	    	System.out.println(throwable);
	    	  // here you should examine the throwable/exception and perform an appropriate action!
	    	});

		// Start the topology
		streams.start();

	    // Add shutdown hook to respond to SIGTERM and gracefully close Kafka Streams
	    Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
	    
	}	
	
}