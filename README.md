# Kafka Workshop

Kafka Workshops with hands-on tutorials for working with Kafka, Java, Spring Boot, KafkaStreams and ksqlDB ...

This workshop is part of the Trivadis course [Apache Kafka for Developer](https://www.trivadis-training.com/en/training/apache-kafka-fuer-entwickler-bd-kafka-dev).

All the workshops can be done on a container-based infrastructure using Docker Compose for the container orchestration. It can be run on a local machine or in a cloud environment. Check [01-environment](https://github.com/gschmutz/kafka-workshop/tree/master/01-environment) for instructions on how to setup the infrastructure.


## List of Workshops

ID  | Title   | Descritpion
------------- | ------------- | -------------
2 | [Getting started with Apache Kafka](./02-working-with-kafka-broker) | Create topics from the command line, use the Console Producer and Console Consumer to publish and consume messages and show how to use `kafakcat`.
3 | [Testing Consumer Scalability and Kafka Failover](./03-understanding-failover)  | demonstrates consumer failover and broker failover and load balancing over various consumers within a consumer group
4 | [Kafka from Java](./04-producing-consuming-kafka-with-java)  | learn how to produce and consume simple messages using the Kafka Java API. Secondly we will see how to produce/consume complex objects using JSON serialization.
4a | [Kafka from Java with Avro & Schema Registry](./04a-working-with-avro-and-java)  | learn how to produce and consume messages using the Kafka Java API using Avro for serialization with the Confluent Schema Registry.
4b | [Working with CloudEvents and Java](./04b-working-with-cloudenvent-and-java)  | learn how to produce  messages using the Kafka Java API with CloudEvents.
5 | [Kafka from Spring Boot using Kafka Spring](./05-producing-consuming-kafka-with-springboot)  | learn how to use Spring Boot to consume and produce from/to Kafka
5a | [Kafka from Spring Boot with Avro & Schema Registry](./05a-working-with-avro-and-springboot)  | learn how to use Spring Boot to consume and produce from/to Kafka using Avro for serialization with the Confluent Schema Registry.
5b | [Kafka from Spring Boot using Cloud Stream](./05b-producing-consuming-kafka-with-springboot-cloud-stream)  | learn how to use Spring Boot to consume and produce from/to Kafka using Avro for serialization with the Confluent Schema Registry.
6 |[Kafka with Confluent's Python client](./06-producing-consuming-kafka-with-python)  | learn how to use Python to consume and produce from/to Kafka
7 | [Kafka with Confluent's .NET client](./07-producing-consuming-kafka-with-dotnet)  | learn how to use .NET to consume and produce from/to Kafka
8 | [Kafka Streams](./08-using-kafka-streams-simple)  | learn how to use Kafka Streams from Java to perform stream analytics
8a | [Kafka Streams from Spring Boot](./08a-using-kafka-streams-from-springboot)  | learn how to use Kafka Streams from Spring Boot to extend your Spring Boot microservice with stream analytics capabilities
8b | [Kafka Streams with Azkarra Streams](./08b-using-kafka-streams-with-azkarra)  | learn how to use Kafka Streams with Azkarra Streams Framework to simplify writing Kafka Streams microservices.
9 | [Vehicle Tracking End-to-End application](./09-vehicle-tracking-application)  | an end-to-end workshop demonstrting Kafka Connect, Kafka Streams and ksqlDB in Action. 
