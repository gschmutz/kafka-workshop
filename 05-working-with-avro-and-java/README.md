# Working with Avro and Java

In this workshop we will learn how to produce and consume messages using the [Kafka Java API](https://kafka.apache.org/documentation/#api) using Avro for serializing and deserializing messages.

## Create the project in Eclipse IDE

Start the Eclipse IDE if not yet done. 

### Create the project and the project definition (pom.xml)

Create a new [Maven project](../99-misc/97-working-with-eclipse/README.md) and in the last step use `com.trivadis.kafkaws` for the **Group Id** and `java-avro-kafka` for the **Artifact Id**.

Navigate to the **pom.xml** and double-click on it. The POM Editor will be displayed. 

You can either use the GUI to edit your `pom.xml` or navigate to the last tab **pom.xml** to switch to the "code view". Let's do that. 

You will see the still rather empty definition.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.trivadis.kafkaws</groupId>
  <artifactId>java-avro-kafka</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</project>
```

Let's add some initial dependencies for our project. We will add some more dependencies to the POM throughout this workshop. 

Copy the following block right after the `<version>` tag, before the closing `</project>` tag.

```xml
   <properties>
       <kafka.version>2.2.0</kafka.version>
       <confluent.version>5.1.0</confluent.version>
       <avro.version>1.8.2</avro.version>
       <java.version>1.8</java.version>
       <slf4j-version>1.7.5</slf4j-version>
       
       <!-- use utf-8 encoding -->
       <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
       <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    </properties>

    <dependencies>
     	<dependency>
           <groupId>org.apache.kafka</groupId>
           <artifactId>kafka-clients</artifactId>
           <version>${kafka.version}</version>
       </dependency>    
          
		<dependency>
			<groupId>io.confluent</groupId>
			<artifactId>kafka-avro-serializer</artifactId>
			<version>${confluent.version}</version>
		</dependency>

     	<dependency>
           <groupId>org.apache.avro</groupId>
           <artifactId>avro</artifactId>
           <version>${avro.version}</version>
       </dependency>    

		<dependency>
		    <groupId>org.slf4j</groupId>
		    <artifactId>slf4j-log4j12</artifactId>
		    <version>${slf4j-version}</version>
		</dependency>
    </dependencies>

	<repositories>
		<repository>
			<id>confluent</id>
			<url>http://packages.confluent.io/maven/</url>
		</repository>
	</repositories>
	<pluginRepositories>
		<pluginRepository>
			<id>confluent</id>
			<url>http://packages.confluent.io/maven/</url>
		</pluginRepository>
	</pluginRepositories>    
    
	<build>
		<defaultGoal>install</defaultGoal>

		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<version>2.5</version>
				<configuration>
					<source>1.8</source>
					<target>1.8</target>
					<maxmem>256M</maxmem>
					<showDeprecation>true</showDeprecation>
				</configuration>
			</plugin>
			<plugin>
				<groupId>org.codehaus.mojo</groupId>
				<artifactId>exec-maven-plugin</artifactId>
				<version>1.6.0</version>
				<executions>
					<execution>
						<id>producer</id>
						<goals>
							<goal>java</goal>
						</goals>
						<configuration>
							<mainClass>com.trivadis.kafkaws.producer.KafkaProducerAvro</mainClass>
						</configuration>						
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
```

In a terminal window, perform the following command to update the Eclipse IDE project settings.

> **Tip:** a quick way to open the terminal at the right place: right-click on the project and select **Show In** | **System Explorer** and then right-click on the folder **java-avro-kafka** and select **Open in Terminal**.

```
mvn eclipse:eclipse
```

Refresh the project in Eclipse to re-read the project settings.

### Create log4j settings

Let's also create the necessary log4j configuration. 

In the code we are using the [Log4J Logging Framework](https://logging.apache.org/log4j/2.x/), which we have to configure using a property file. 

Create a new file `log4j.properties` in the folder **src/main/resources** and add the following configuration properties. 

```
## ------------------------------------------------------------------------
## Licensed to the Apache Software Foundation (ASF) under one or more
## contributor license agreements.  See the NOTICE file distributed with
## this work for additional information regarding copyright ownership.
## The ASF licenses this file to You under the Apache License, Version 2.0
## (the "License"); you may not use this file except in compliance with
## the License.  You may obtain a copy of the License at
##
## http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software
## distributed under the License is distributed on an "AS IS" BASIS,
## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
## See the License for the specific language governing permissions and
## limitations under the License.
## ------------------------------------------------------------------------

#
# The logging properties used for eclipse testing, We want to see INFO output on the console.
#
log4j.rootLogger=INFO, out

#log4j.logger.org.apache.kafka=INFO

log4j.logger.org.apache.camel.impl.converter=INFO
log4j.logger.org.apache.camel.util.ResolverUtil=INFO

log4j.logger.org.springframework=WARN
log4j.logger.org.hibernate=WARN

# CONSOLE appender not used by default
log4j.appender.out=org.apache.log4j.ConsoleAppender
log4j.appender.out.layout=org.apache.log4j.PatternLayout
log4j.appender.out.layout.ConversionPattern=[%30.30t] %-30.30c{1} %-5p %m%n
#log4j.appender.out.layout.ConversionPattern=%d [%-15.15t] %-5p %-30.30c{1} - %m%n

log4j.throwableRenderer=org.apache.log4j.EnhancedThrowableRenderer
```
### Creating the necessary Kafka Topic 
We will use the topic `test-java-topic` in the Producer and Consumer code below. Due to the fact that `auto.topic.create.enable` is set to `false`, we have to manually create the topic. 

Connect to the `broker-1` container

```
docker exec -ti broker-1 bash
```

and execute the necessary kafka-topics command. 

```
kafka-topics --create \
--replication-factor 3 \
--partitions 8 \
--topic test-java-avro-topic \
--zookeeper zookeeper-1:2181
```

Cross check that the topic has been created.

```
kafka-topics --list --zookeeper zookeeper-1:2181
```

This finishes the setup steps and our new project is ready to be used. Next we will start implementing the **Kafka Producer** which uses Avro for the serialization. 

## Create an Avro Schema representing the Notification Message

First create a new Folder `avro` under the existing folder **src/main/**.

Create a new File `Notification-v1.avsc` in the folder  **src/main/avro** just created above.

Add the following Avro schema to the empty file.  

```json
{
  "type" : "record",
  "namespace" : "com.trivadis.kafkaws.avro.v1",
  "name" : "Notification",
  "description" : "A simple Notification Event message",
  "fields" : [
	    { "type" : ["long", "null"],
	      "name" : "id"
	    },
	    { "type" : ["string", "null"],
	      "name" : "message"
	    }
  ]
}
```

In the `pom.xml`, add the `avro-maven-plugin` plugin to the `<build><plugins>` section, just below the `exec-maven-plugin`. 

```xml
			<plugin>
				<groupId>org.apache.avro</groupId>
				<artifactId>avro-maven-plugin</artifactId>
				<version>${avro.version}</version>
				<executions>
					<execution>
						<phase>generate-sources</phase>
						<goals>
							<goal>schema</goal>
							<goal>protocol</goal>
							<goal>idl-protocol</goal>
						</goals>
						<configuration>
							<fieldVisibility>private</fieldVisibility>
							<sourceDirectory>${project.basedir}/src/main/avro</sourceDirectory>
						</configuration>
					</execution>
				</executions>
			</plugin>
```

This plugin will make sure, that classes are generated based on the Avro schema, whenever a `mvn compile` is executed. Let's exactly do that on the still rather empty project. But first also execute an `mvn eclipse:eclipse` to regenerate the Eclipse project settings. 

```
mvn eclipse:eclipse
mvn compile
```

After running this command, refresh the project in Eclipse and you should see a new folder named `target/generated-sources/avro`. Expand into this folder and you should see one generated Java class named `Notification`.

![Alt Image Text](./images/avro-generated-sources-folder.png "Schema Registry UI")

Double click on the `Notification` class to inspect the code. 

```java
package com.trivadis.kafkaws.avro.v1;

import org.apache.avro.specific.SpecificData;

@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public class Notification extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = 799361421243801515L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Notification\",\"namespace\":\"com.trivadis.kafkaws.avro.v1\",\"fields\":[{\"name\":\"id\",\"type\":[\"long\",\"null\"]},{\"name\":\"message\",\"type\":[\"string\",\"null\"]}],\"description\":\"A simple Notification Event message\"}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
   private java.lang.Long id;
   private java.lang.CharSequence message;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public Notification() {}

  /**
   * All-args constructor.
   * @param id The new value for id
   * @param message The new value for message
   */
  public Notification(java.lang.Long id, java.lang.CharSequence message) {
    this.id = id;
    this.message = message;
  }
  ...
```

You can see that the code is based on the information in the Avro schema. We will use this class when we produce as well as consume Avro messages to/from Kafka.

## Create a Kafka Producer using Avro for serialisation

First create a new Java Package `com.trivadis.kafkaws.producer` in the folder **src/main/java**.

Create a new Java Class `KafkaProducerAvro` in the package `com.trivadis.kafakws.producer` just created. 

Add the following code to the empty class to create a Kafka Producer. It is similar to the code we have seen in the previous workshop. We have changed both serializer to use the Confluent `KafkaAvroSerializer` class and added the URL to the Confluent Schema Registry API. 

```java
package com.trivadis.kafkaws.producer;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;

public class KafkaProducerAvro {

    private final static String TOPIC = "test-java-avro-topic";
    private final static String BOOTSTRAP_SERVERS =
            "dataplatform:9092, dataplatform:9093, dataplatform:9094";
    private final static String SCHEMA_REGISTRY_URL = "http://dataplatform:8081";
    
    private static Producer<Long, Notification> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaExampleProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);   // use constant for "schema.registry.url"

        return new KafkaProducer<>(props);
    }
}	
```

We will be using the synchronous way for producing messages to the Kafka topic we created above, but the other methods would work as well with Avro.

```java
    static void runProducer(final int sendMessageCount, final int waitMsInBetween, final long id) throws Exception {
        final Producer<Long, Notification> producer = createProducer();
        long time = System.currentTimeMillis();
        Long key = (id > 0) ? id : null;
                
        try {
            for (long index = 0; index < sendMessageCount; index++) {
            	Notification notification = new Notification(id, "Hello Kafka " + index);
                final ProducerRecord<Long, Notification> record =
                        new ProducerRecord<>(TOPIC, key, notification);

                RecordMetadata metadata = producer.send(record).get();

                long elapsedTime = System.currentTimeMillis() - time;
                System.out.printf("[" + id + "] sent record(key=%s value=%s) " +
                                "meta(partition=%d, offset=%d) time=%d\n",
                        record.key(), record.value(), metadata.partition(),
                        metadata.offset(), elapsedTime);
                time = System.currentTimeMillis();
                
                Thread.sleep(waitMsInBetween);
            }
        } finally {
            producer.flush();
            producer.close();
        }
    } 
```

Next you define the main method.
    
```
    public static void main(String... args) throws Exception {
        if (args.length == 0) {
            runProducer(100,10,0);
        } else {
            runProducer(Integer.parseInt(args[0]),Integer.parseInt(args[1]),Long.parseLong(args[2]));
        }
    }
```

The `main()` method accepts 3 parameters, the number of messages to produce, the time in ms to wait in-between sending each message and the ID of the producer.

Use `kafkacat` or `kafka-console-consumer` to consume the messages from the topic `test-java-avro-topic `.

```
kafkacat -b $PUBLIC_IP -t test-java-avro-topic
```

Now run it using the `mvn exec:java` command. It will generate 1000 messages, waiting 10ms in-between sending each message and use 0 for the ID. 

```
mvn exec:java@producer -Dexec.args="1000 100 0"
```

You can see that kafkacat shows some special, non-printable characters. This is due to the Avro format. If you want to display the Avro, you can use the `kafka-avro-console-consumer` CLI, which is part of the Schema Registry. 

So let's connect to the schema registry container:

```
docker exec -ti streamingplatform_schema_registry_1 bash
```

And then invoke the `kafka-avro-console-consumer` similar to the "normal" consumer seen so far. 

```
kafka-avro-console-consumer --bootstrap-server kafka-1:19092 --topic test-java-avro-topic
```

You should see an output similar to the one below. 

```
...
[2018-07-11 21:32:43,155] INFO [Consumer clientId=consumer-1, groupId=console-consumer-88150] Resetting offset for partition test-java-avro-topic-6 to offset 0. (org.apache.kafka.clients.consumer.internals.Fetcher)
{"id":{"long":0},"message":{"string":"Hello Kafka 0"}}
{"id":{"long":0},"message":{"string":"Hello Kafka 3"}}
{"id":{"long":0},"message":{"string":"Hello Kafka 1"}}
{"id":{"long":0},"message":{"string":"Hello Kafka 2"}}
{"id":{"long":0},"message":{"string":"Hello Kafka 4"}}
{"id":{"long":0},"message":{"string":"Hello Kafka 5"}}
{"id":{"long":0},"message":{"string":"Hello Kafka 6"}}
{"id":{"long":0},"message":{"string":"Hello Kafka 9"}}
{"id":{"long":0},"message":{"string":"Hello Kafka 7"}}
{"id":{"long":0},"message":{"string":"Hello Kafka 10"}}
{"id":{"long":0},"message":{"string":"Hello Kafka 8"}}
...
```

## View the Schema in the Registry

The Avro Serializer and Deserializer automatically register the Avro schema, if it is not already in the registry. 

The Streamingplatform also contains a tool made by a company called Landoop which allows us to see what's in the registry. 

In a browser, navigate to <http://dataplatform:28039> and you should see the home page of the Schema Registry UI. 

![Alt Image Text](./images/schema-registry-ui-welcome.png "Schema Registry UI")

If you click on the schema to the left, you can view the details of the schema. You can see that version v1 has been assigned automatically.

![Alt Image Text](./images/schema-registry-ui-schema-details.png "Schema Registry UI")

## Create a Kafka Consumer using Avro for serialization

First create a new Java Package `com.trivadis.kafkaws.consumer` in the folder **src/main/java**.

Create a new Java Class `KafkaConsumerAvro` in the package `com.trivadis.kafakws.consumer` just created. 

Add the following code to the empty class. 

```java
package com.trivadis.kafkaws.consumer;

import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import com.trivadis.kafkaws.avro.v1.Notification;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;

public class KafkaConsumerAvro {
	private final static String TOPIC = "test-java-avro-topic";
    private final static String BOOTSTRAP_SERVERS =
            "dataplatform:9092, dataplatform:9093, dataplatform:9094";
    private final static String SCHEMA_REGISTRY_URL = "http://dataplatform:8081";
  
    private static Consumer<Long, Notification> createConsumer() {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "KakfaConsumerAvro");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 10000);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, SCHEMA_REGISTRY_URL);   // use constant for "schema.registry.url"

        // Create the consumer using props.
        final Consumer<Long, Notification> consumer =
                                    new KafkaConsumer<>(props);

        // Subscribe to the topic.
        consumer.subscribe(Collections.singletonList(TOPIC));
        return consumer;
    }    
    
    static void runConsumer(int waitMsInBetween) throws InterruptedException {
        final Consumer<Long, Notification> consumer = createConsumer();

        final int giveUp = 100;   
        int noRecordsCount = 0;

        while (true) {
            final ConsumerRecords<Long, Notification> consumerRecords = consumer.poll(1000);

            if (consumerRecords.count()==0) {
                noRecordsCount++;
                if (noRecordsCount > giveUp) 
                	break;
            }
            
            consumerRecords.forEach(record -> {
                System.out.printf("%d - Consumer Record:(Key: %d, Value: %s, Partition: %d, Offset: %d)\n",
                        consumerRecords.count(), record.key(), record.value(),
                        record.partition(), record.offset());
                try {
                	Thread.sleep(waitMsInBetween);
                } catch (InterruptedException e) {
				} 
            });

            consumer.commitSync();
        }
        consumer.close();
        System.out.println("DONE");
    }
   
    public static void main(String... args) throws Exception {
        if (args.length == 0) {
        	runConsumer(10);
        } else {
        	runConsumer(Integer.parseInt(args[0]));
        }
    }
    
}
```

## Register in Schema Registry using Maven

To register an Avro schema automatically, you can use the following Maven plugin. 

Add the following definition to the `pom.xml`.

```xml
			<plugin>
				<groupId>io.confluent</groupId>
				<artifactId>kafka-schema-registry-maven-plugin</artifactId>
				<version>3.2.0</version>
				<configuration>
					<schemaRegistryUrls>
						<param>http://dataplatform:8081</param>
					</schemaRegistryUrls>
					<subjects>
						<tweet-value>src/main/avro/Notification-v1.avsc</tweet-value>
					</subjects>
				</configuration>
				<goals>
					<goal>register</goal>
					<goal>test-compatibility</goal>
				</goals>
			</plugin>
```

Now you can use the following command to register the schemas with the Schema Registry:

```
mvn schema-registry:register
```


	
