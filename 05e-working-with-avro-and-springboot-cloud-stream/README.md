# Kafka from Spring Boot using Cloud Stream with Avro & Schema Registry

In this workshop we will learn how to use the **Spring Cloud Stream**  with Avro message serialization from within a Spring Boot application. We will implement both a consumer and a producer. We are using the same Avro schema and reuse the `meta` project from [Workshop 5a: Kafka from Spring Boot with Avro & Schema Registry](../05a-working-with-avro-and-springboot).

We will create two Spring Boot projects, one for the Producer and one for the Consumer, simulating two independent microservices interacting with eachother via events.

Before you can go further, make sure to create and build the [meta](https://github.com/gschmutz/kafka-workshop/tree/master/05a-working-with-avro-and-springboot#create-the-avro-metadata-project) project as demonstrated in [Workshop 5a: Kafka from Spring Boot with Avro & Schema Registry](../05a-working-with-avro-and-springboot).

## Create the Spring Boot Producer Project

First we create an test the Producer microservice using Spring Cloud Stream abstraction.

### Creating the Spring Boot Project

First, let’s navigate to [Spring Initializr](https://start.spring.io/) to generate our project. Our project will need the Spring Cloud Stream support. 

Select Generate a **Maven Project** with **Java** and Spring Boot **2.6.5**. Enter `com.trivadis.kafkaws` for the **Group**, `spring-boot-cloud-stream-kafka-producer-avro` for the **Artifact** field and `Kafka Producer with Avro project for Spring Cloud Stream` for the **Description** field. 

Click on **Add Dependencies** and search for the  **Spring Cloud Stream** depencency. 

Select the dependency and hit the **Enter** key. You should now see the dependency on the right side of the screen.

Click on **Generate Project** and unzip the ZIP file to a convenient location for development. 

Once you have unzipped the project, you’ll have a very simple structure. 

Import the project as a Maven Project into your favourite IDE for further development. 

### Extend the Maven POM with some configurations

In oder to use the Avro serializer and the class generated above, we have to add the following dependencies to the `pom.xml`. 

```xml
	<dependencies>
	   ...
	   	
	   	<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-stream-binder-kafka</artifactId>
		</dependency>
		
		<dependency>
			<groupId>io.confluent</groupId>
			<artifactId>kafka-avro-serializer</artifactId>
			<version>${confluent.version}</version>			<exclusions>
				<exclusion>
					<groupId>org.slf4j</groupId>
					<artifactId>slf4j-api</artifactId>
				</exclusion>
				<exclusion>
					<groupId>org.slf4j</groupId>
					<artifactId>slf4j-log4j12</artifactId>
				</exclusion>
			</exclusions>
		</dependency>
		<dependency>
			<groupId>io.confluent</groupId>
			<artifactId>kafka-schema-registry-client</artifactId>
			<version>${confluent.version}</version>
		</dependency>
				
		<dependency>
			<groupId>com.trivadis.kafkaws.meta</groupId>
			<artifactId>meta</artifactId>
			<version>1.0-SNAPSHOT</version>
		</dependency>
```

Add the version of the Confluent Platform to use as an additional property

```xml
	<properties>
	   ...
		<confluent.version>7.0.0</confluent.version>
	</properties>
```

We also have to specify the additional Maven repository 

```xml
	<repositories>
		<repository>
			<id>confluent</id>
			<url>https://packages.confluent.io/maven/</url>
		</repository>
	</repositories>
```

### Implement a Kafka Producer in Spring

Now create a simple Java class `KafkaEventProducer` within the `com.trivadis.kafkaws.springbootkafkaproducer ` package, which we will use to produce messages to Kafka.

```java
package com.trivadis.kafkaws.springcloudstreamkafkaproducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
public class KafkaEventProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventProducer.class);

    @Autowired
    private Processor processor;

    @Value("${topic.name}")
    String kafkaTopic;

    public void produce(Integer id, Long key, String value) {
        long time = System.currentTimeMillis();

        Message<String> message = MessageBuilder.withPayload(value)
                .setHeader(KafkaHeaders.MESSAGE_KEY, key)
                .build();

        processor.output()
                .send(message);

        long elapsedTime = System.currentTimeMillis() - time;

        System.out.printf("[" + id + "] sent record(key=%s value=%s) time=%d\n",key, value,elapsedTime);

    }
}
```

We no longer use `String` as type for the value but the `Nofification` class, which has been generated based on the Avro schema above.

### Create the necessary Topics through code

Spring Kafka can automatically add topics to the broker, if they do not yet exists. By that you can replace the `kafka-topics` CLI commands seen so far to create the topics, if you like. This code is exactly the same as in workshop 4 with the non-avro version.

```java
package com.trivadis.kafkaws.springcloudstreamkafkaproduceravro;

import com.trivadis.kafkaws.avro.v1.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventProducer.class);

    @Autowired
    private Processor processor;

    @Value("${topic.name}")
    String kafkaTopic;

    public void produce(Integer id, Long key, Notification notification) {
        long time = System.currentTimeMillis();

        Message<Notification> message = MessageBuilder.withPayload(notification)
                .setHeader(KafkaHeaders.MESSAGE_KEY, key)
                .build();

        processor.output()
                .send(message);

        long elapsedTime = System.currentTimeMillis() - time;

        System.out.printf("[" + id + "] sent record(key=%s value=%s) time=%d\n",key, notification,elapsedTime);
    }
}
```

We again refer to properties, which will be defined later in the `application.yml` config file. 

### Add Producer logic to the SpringCloudStreamKafkaProducerAvroApplication class

We change the generated Spring Boot application to be a console appliation by implementing the `CommandLineRunner` interface. The `run` method holds the same code as the `main()` method in [Workshop 4: Working with Kafka from Java](../04-producing-consuming-kafka-with-java). The `runProducer` method is also similar, we just use the `kafkaEventProducer` instance injected by Spring to produce the messages to Kafka.

```java
package com.trivadis.kafkaws.springcloudstreamkafkaproducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;

import java.time.LocalDateTime;

@SpringBootApplication
@EnableBinding(Processor.class)
public class SpringCloudStreamKafkaProducerApplication implements CommandLineRunner {

	private static Logger LOG = LoggerFactory.getLogger(SpringCloudStreamKafkaProducerApplication.class);

	@Autowired
	private KafkaEventProducer kafkaEventProducer;

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudStreamKafkaProducerApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		LOG.info("EXECUTING : command line runner");

		if (args.length == 0) {
			runProducer(100, 10, 0);
		} else {
			runProducer(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Long.parseLong(args[2]));
		}

	}

	private void runProducer(int sendMessageCount, int waitMsInBetween, long id) throws Exception {
		Long key = (id > 0) ? id : null;

		for (int index = 0; index < sendMessageCount; index++) {
			String value =  "[" + id + "] Hello Kafka " + index + " => " + LocalDateTime.now();

			kafkaEventProducer.produce(index, key, value);

			// Simulate slow processing
			Thread.sleep(waitMsInBetween);

		}
	}

}
```

The difference here to the non-avro version is, that we are using the builder of the generated `Nofification` class to create an instance of a notification, which we then pass as the value to the `produce()` method.

### Configure Kafka through application.yml configuration file

First let's rename the existing `application.properties` file to `application.yml` to use the `yml` format. 

Add the following settings to configure the Kafka cluster and the name of the topic:

```yml
topic:
  name: test-spring-cloud-stream-topic-avro
  replication-factor: 3
  partitions: 12

spring:
  cloud:
    stream:
      kafka:
        bindings:
          output:
            producer:
              configuration:
                value.serializer: io.confluent.kafka.serializers.KafkaAvroSerializer
                schema.registry.url: http://${DATAPLATFORM_IP}:8081

      bindings:
        output:
          destination: ${topic.name}
          producer:
            useNativeEncoding: true

  kafka:
    bootstrap-servers: ${DATAPLATFORM_IP}:9092
```

Here again we switch the value-serializer from `StringSerializer` to the `KafkaAvroSerializer` and add the property `schema.registry.url` to configure the location of the Confluent Schema Registry REST API.

For the IP address of the Kafka cluster we refer to an environment variable, which we have to declare before running the application.

```bash
export DATAPLATFORM_IP=nnn.nnn.nnn.nnn
```

### Build the application

First lets build the application:

```bash
mvn package -Dmaven.test.skip=true
```

### Use Console to test the application

In a terminal window start consuming from the output topic:

```bash
kafkacat -b $DATAPLATFORM_IP -t test-spring-avro-topic -s avro -r http://$DATAPLATFORM_IP:8081 -o end
```

### Run the application

Now let's run the application

```bash
mvn spring-boot:run
```

Make sure that you see the messages through the console consumer.

To run the producer with custom parameters (for example to specify the key to use), use the `-Dspring-boot.run.arguments`:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="100 10 10"
```

## Create the Spring Boot Consumer Project

Now let's create an test the Consumer microservice.

### Creating the Spring Boot Project

Use again the [Spring Initializr](https://start.spring.io/) to generate the project.

Select Generate a **Maven Project** with **Java** and Spring Boot **2.6.5**. Enter `com.trivadis.kafkaws` for the **Group**, `spring-boot-cloud-stream-kafka-consumer-avro` for the **Artifact** field and `Kafka Consumer with Avro project for Spring Cloud Stream` for the **Description** field. 

Click on **Add Dependencies** and search for the  **Cloud Stream** depencency. 

Select the dependency and hit the **Enter** key. You should now see the dependency on the right side of the screen.

Click on **Generate Project** and unzip the ZIP file to a convenient location for development. 

Once you have unzipped the project, you’ll have a very simple structure. 

Import the project as a Maven Project into your favourite IDE for further development. 

### Extend the Maven POM with some configurations

In oder to use the Avro deserializer and the Avro generated classes, we have to add the following dependencies to the `pom.xml`. 

```xml
	<dependencies>
	   ...
	   
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-stream-binder-kafka</artifactId>
        </dependency>
        <dependency>
            <groupId>io.confluent</groupId>
            <artifactId>kafka-avro-serializer</artifactId>
            <version>${confluent.version}</version>
            <exclusions>
                <exclusion>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-api</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.slf4j</groupId>
                    <artifactId>slf4j-log4j12</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>io.confluent</groupId>
            <artifactId>kafka-schema-registry-client</artifactId>
            <version>${confluent.version}</version>
        </dependency>
		
		<dependency>
			<groupId>com.trivadis.kafkaws.meta</groupId>
			<artifactId>meta</artifactId>
			<version>1.0-SNAPSHOT</version>
		</dependency>
```

Add the version of the Confluent Platform to use as an additional property

```xml
	<properties>
	   ...
		<confluent.version>7.0.0</confluent.version>
	</properties>
```

We also have to specify the additional Maven repository 

```xml
	<repositories>
		<repository>
			<id>confluent</id>
			<url>https://packages.confluent.io/maven/</url>
		</repository>
	</repositories>
```

### Implement a Kafka Consumer in Spring

Start by creating a simple Java class `KafkaEventConsumer` within the `com.trivadis.kafkaws.springbootkafkaconsumer` package, which we will use to consume messages from Kafka. 

```java
package com.trivadis.kafkaws.springcloudstreamkafkaconsumeravro;

import com.trivadis.kafkaws.avro.v1.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventConsumer.class);

    @StreamListener(Processor.INPUT)
    public void receive(Message<Notification> msg) {
        Notification value = msg.getPayload();
        Long key = (Long)msg.getHeaders().get(KafkaHeaders.RECEIVED_MESSAGE_KEY);
        LOGGER.info("received key = '{}' with payload='{}'", key, value);
    }

}
```

This class uses the `Component` annotation to have it registered as bean in the Spring context and the `KafkaListener` annotation to specify a listener method to be called for each record consumed from the Kafka input topic. The name of the topic is specified as a property to be read again from the `application.yml` configuration file.

In the code we only log the key and value received to the console. In real life, we would probably inject another bean into the `KafkaEventConsumer` to perform the message processing.

### Add @EnableBinding to Application class  

Add the following annotation to the `SpringCloudStreamKafkaConsumerAvroApplication` class

```yml
@EnableBinding(Processor.class)
public class SpringCloudStreamKafkaConsumerAvroApplication {
...
```

### Configure Kafka through application.yml configuration file

First let's rename the existing `application.properties` file to `application.yml` to use the `yml` format. 

Add the following settings to configure the Kafka cluster and the name of the two topics:

```yml
topic:
  name: test-spring-cloud-stream-topic-avro

spring:
  cloud:
    stream:
      kafka:
        bindings:
          input:
            consumer:
              configuration:
                value.deserializer: io.confluent.kafka.serializers.KafkaAvroDeserializer
                schema.registry.url: http://${DATAPLATFORM_IP}:8081
                specific.avro.reader: true

      bindings:
        input:
          destination: ${topic.name}
          consumer:
            useNativeEncoding: true

  kafka:
    bootstrap-servers:
      - ${DATAPLATFORM_IP}:9092
      - ${DATAPLATFORM_IP}:9093
```

For the IP address of the Kafka cluster we refer to an environment variable, which we have to declare before running the application.

```bash
export DATAPLATFORM_IP=nnn.nnn.nnn.nnn
```

### Build the application

First lets build the application:

```bash
mvn package -Dmaven.test.skip=true
```

### Run the application

Now let's run the application

```bash
mvn spring-boot:run
```


