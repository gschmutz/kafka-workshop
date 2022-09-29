# Kafka Workshop

Kafka Workshops with hands-on tutorials for working with Kafka, Java, Spring Boot, KafkaStreams and ksqlDB ...

This workshop is part of the Trivadis course [Apache Kafka for Developer](https://www.trivadis-training.com/en/training/apache-kafka-fuer-entwickler-bd-kafka-dev).

All the workshops can be done on a container-based infrastructure using Docker Compose for the container orchestration. It can be run on a local machine or in a cloud environment. Check [01-environment](https://github.com/gschmutz/kafka-workshop/tree/master/01-environment) for instructions on how to setup the infrastructure.


## List of Workshops

ID  | Title   | Descritpion
------------- | ------------- | -------------
2 | [Getting started with Apache Kafka](./02-working-with-kafka-broker) | Create topics from the command line, use the Console Producer and Console Consumer to publish and consume messages and show how to use `kcat` (used to be `kafakcat`).
3 | [Testing Consumer Scalability and Kafka Failover](./03-understanding-failover)  | demonstrates consumer failover and broker failover and load balancing over various consumers within a consumer group
4 | [Simple Kafka Consumer & Producer from Java](./04-producing-consuming-kafka-with-java)  | learn how to produce and consume simple messages using the **Kafka Java API**. Secondly we will see how to produce/consume complex objects using JSON serialization.
4a | [Simple Kafka Consumer & Producer from Java with Avro & Schema Registry](./04a-working-with-avro-and-java)  | learn how to produce and consume messages using the **Kafka Java API** using Avro for serialization with the Confluent Schema Registry.
4b | [Using Kafka from Java with Avro IDL & Schema Registry](./04b-working-with-avro-avdl-and-java)  | learn how to produce and consume messages using the **Kafka Java API** using Avro for serialization with the Confluent Schema Registry.
4c | [Simple Kafka Consumer & Producer from Java with Protocol Buffers & Schema Registry](./04c-working-with-protobuf-and-java)  | learn how to produce and consume messages using the **Kafka Java API** using Protocol Buffers for serialisation with the Confluent Schema Registry.
4d | [Simple Kafka Consumer & Producer from Java with JSON Schema & Schema Registry](./04d-working-with-jsonschema-and-java)  | learn how to produce and consume messages using the **Kafka Java API** using JSON Schema for serialisation with the Confluent Schema Registry.
5 | [Kafka from Spring Boot using Kafka Spring](./05-producing-consuming-kafka-with-springboot)  | learn how to use **Spring Boot** to consume and produce from/to Kafka
5a | [Kafka from Spring Boot with Avro & Schema Registry](./05a-working-with-avro-and-springboot)  | learn how to use **Spring Boot** to consume and produce from/to Kafka using Avro for serialization with the Confluent Schema Registry.
5b | [Kafka from Spring Boot with Avro IDL & Schema Registry](./05b-working-with-avro-idl-and-springboot)  | learn how to use **Spring Boot** to consume and produce from/to Kafka using Avro (using IDL for schema definition) for serialization with the Confluent Schema Registry.
5c | [Kafka from Spring Boot using Cloud Stream](./05c-producing-consuming-kafka-with-springboot-cloud-stream)  | learn how to use **Spring Cloud Streams** to consume and produce from/to Kafka
5d | [Kafka from Spring Boot using Cloud Stream with Avro & Schema Registry](./05d-working-with-avro-and-springboot-cloud-stream)  | learn how to use **Spring Cloud Streams** to consume and produce from/to Kafka using Avro for serialization with the Confluent Schema Registry.
6 | [Simple Kafka Consumer & Producer from Kotlin](./06-producing-consuming-kafka-with-kotlin)  | learn how to produce and consume messages using the **Kotlin** with the Java API.
6a | [Using Kafka from Kotlin with Avro & Schema Registry](./06a-working-with-avro-and-kotlin)  | learn how to produce and consume messages with **Kotlin** and the Kafka Java API using Avro for serialization with the Confluent Schema Registry.
6b | [Using Kafka from Spring Boot with Kotlin using Kafka Spring](./06b-producing-consuming-kafka-with-kotlin-springboot)  | learn how to use **Kotlin** with **Spring Boot** to consume and produce from/to Kafka
6c | [Using Kafka from Spring Boot with Kotlin and Avro & Schema Registry](./06c-working-with-avro-and-kotlin-springboot)  | learn how to use **Kotlin** with **Spring Boot** to consume and produce from/to Kafka using Avro for serialization with the Confluent Schema Registry. 
7 | [Kafka with Confluent's .NET client](./07-producing-consuming-kafka-with-dotnet)  | learn how to use **.NET** to consume and produce from/to Kafka
7b | [Using Kafka from #C (.Net) with Avro & Schema Registry](./07b-working-with-avro-and-dotnet)  | learn how to use **.NET** to consume and produce messages from/to Kafka using Avro for serialization with the Confluent Schema Registry
8 |[Kafka with Confluent's Python client](./08-producing-consuming-kafka-with-python)  | learn how to use **Python** to consume and produce from/to Kafka
9 |[Kafka from Node.js](./09-working-with-nodejs)  | learn how to use **Node.js** to consume and produce from/to Kafka
10 | [Working with Kafka Connect and Change Data Capture (CDC)](./10-working-with-kafka-connect-and-cdc)  | uses **Kafka Connect** to demonstrate various way for doing CDC on a set of PostgreSQL tables.
12 | [Using Kafka Streams for Stream Analytics](./12-using-kafka-streams-simple)  | uses **Kafka Streams** to demonstrate analytics on data-in-motion.
12a | [Using Kafka Streams from Spring Boot for Stream Analytics](./12a-using-kafka-streams-from-springboot)  | uses **Kafka Streams** from **Spring Boot** to demonstrate analytics on data-in-motion.
12b | [Using Kafka Streams with Azkarra Streams for Stream Analytics](./12b-using-kafka-streams-with-azkarra)  | uses **Kafka Streams** with **Azkarra Streams** to demonstrate analytics on data-in-motion.
13 | [IoT Vehicle Tracking Workshop](./13-iot-vehicle-tracking-workshop)  | a multi-step end-to-end workshop around IoT Vehicle Tracking
13a | [IoT Data Ingestion through MQTT into Kafka](./13a-iot-data-ingestion-over-mqtt)  | learn how to use Kafka Connect to ingest data from MQTT into Kafka
13b | [Stream Analytics using ksqlDB](./13b-stream-analytics-using-ksql)  | learn how to use ksqlDB to execute processing and analytics directly on a stream of data
13c | [Stream Analytics with using Kafka Streams](./13c-stream-analytics-using-java-kstreams)  | learn how to use Kafka Streams from Java to execute processing and analytics directly on a stream of data
13d | [Stream Analytics with .Net using Streamiz](./13d-stream-analytics-using-dotnet-streamiz)  | learn how to use Streamiz from .NET to execute processing and analytics directly on a stream of data
13e | [Stream Analytics using Spark Structured Streaming](./13e-stream-analytics-using-spark-structured-streaming)  | learn how to use Spark Structured Streaming to execute processing and analytics directly on a stream of data
13f | [Static Data Ingestion into Kafka](./13f-static-data-ingestion)  | use Kafka Connect to ingest data from relational database (static data) and then join it with a data stream using ksqlDB
14 | [Using Confluent REST APIs (REST Proxy)](./14-using-rest-proxy)  | use Confluent REST Proxy to create topics and produce and consume messages (JSON and Avro) to these topics.
16 |[Manipulating Consumer Offsets](./16-manipulating-consumer-offsets)  | learn how to use the `kafka-consumer-groups` CLI for manipulating consumer offsets

