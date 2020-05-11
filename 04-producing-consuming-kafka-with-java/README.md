# Working with Kafka from Java

In this workshop we will learn how to produce and consume messages using the [Kafka Java API](https://kafka.apache.org/documentation/#api).

## Create the project in Eclipse IDE

Start the Eclipse IDE if not yet done. 

### Create the project and the project definition (pom.xml)

Create a new [Maven project](../99-misc/97-working-with-eclipse/README.md) and in the last step use `com.trivadis.kafkaws` for the **Group Id** and `java-kafka` for the **Artifact Id**.

Navigate to the **pom.xml** and double-click on it. The POM Editor will be displayed. 

You can either use the GUI to edit your pom.xml or click on the last tab **pom.xml** to switch to the "code view". Let's do that. 

You will see the still rather empty definition.

```
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.trivadis.kafkaws</groupId>
  <artifactId>java-kafka</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</project>
```

Let's add some initial dependencies for our project. We will add some more dependencies to the POM throughout this workshop.

Copy the following block right after the <version> tag, before the closing </project> tag.

```
   <properties>
       <kafka.version>2.2.0</kafka.version>
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
		    <groupId>org.slf4j</groupId>
		    <artifactId>slf4j-log4j12</artifactId>
		    <version>${slf4j-version}</version>
		</dependency>
    </dependencies>
    
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
							<mainClass>com.trivadis.kafkaws.producer.KafkaProducerSync</mainClass>
						</configuration>						
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
```

In a terminal window, perform the following command to update the Eclipse IDE project settings. 

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
    --topic test-java-topic \
    --zookeeper zookeeper-1:2181
```

Cross check that the topic has been created.

```
kafka-topics --list \
    --zookeeper zookeeper-1:2181
```

This finishes the setup steps and our new project is ready to be used. Next we will start implementing a **Kafka Producer**.

## Create a Kafka Producer

Let's first create a producer in synchronous mode.

### Using the Synchronous mode

First create a new Java Package `com.trivadis.kafkaws.producer` in the folder **src/main/java**.

Create a new Java Class `KafkaProducerSync` in the package `com.trivadis.kafkaws.producer` just created. 

Add the following code to the empty class to create a Kafka Producer. 

```java
package com.trivadis.kafkaws.producer;

import java.time.LocalDateTime;
import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaProducerSync {

    private final static String TOPIC = "test-java-topic";
    private final static String BOOTSTRAP_SERVERS
            = "dataplatform:9092,dataplatform:9093";

    private static Producer<Long, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaExampleProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                LongSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }
}
```

Kafka provides a synchronous send method to send a record to a topic. Let’s use this method to send some message ids and messages to the Kafka topic we created earlier.

```java
    private static void runProducer(int sendMessageCount, int waitMsInBetween, long id) throws Exception {
        try (Producer<Long, String> producer = createProducer()) {
            for (int index = 0; index < sendMessageCount; index++) {
                long time = System.currentTimeMillis();

                ProducerRecord<Long, String> record
                        = new ProducerRecord<>(TOPIC, "[" + id + "] Hello Kafka " + LocalDateTime.now());

                RecordMetadata metadata = producer.send(record).get();

                long elapsedTime = System.currentTimeMillis() - time;
                System.out.printf("[" + id + "] sent record(key=%s value=%s) "
                        + "meta(partition=%d, offset=%d) time=%d\n",
                        record.key(), record.value(), metadata.partition(),
                        metadata.offset(), elapsedTime);

                // Simulate slow processing
                Thread.sleep(waitMsInBetween);
            }
        }
    }
```

Next you define the main method.
    
```java
    public static void main(String... args) throws Exception {
        if (args.length == 0) {
            runProducer(100, 10, 0);
        } else {
            runProducer(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Long.parseLong(args[2]));
        }
    }
```

The `main()` method accepts 3 parameters, the number of messages to produce, the time in ms to wait in-between sending each message and the ID of the producer.


Now run it using the `mvn exec:java` command. It will generate 1000 messages, waiting 10ms in-between sending each message and use 0 for the ID. 

```
mvn exec:java@producer -Dexec.args="1000 100 0"
```

Use `kafkacat` or `kafka-console-consumer` to consume the messages from the topic `test-java-topic`.

```
kafkacat -b localhost -t test-java-topic -f 'Part-%p => %k:%s\n'
```

```
% Auto-selecting Consumer mode (use -P or -C to override)
Part-5 => :[0] Hello Kafka 0
Part-4 => :[0] Hello Kafka 1
Part-3 => :[0] Hello Kafka 4
Part-2 => :[0] Hello Kafka 7
Part-5 => :[0] Hello Kafka 8
Part-4 => :[0] Hello Kafka 9
Part-7 => :[0] Hello Kafka 2
Part-7 => :[0] Hello Kafka 10
Part-1 => :[0] Hello Kafka 3
Part-1 => :[0] Hello Kafka 11
Part-3 => :[0] Hello Kafka 12
Part-6 => :[0] Hello Kafka 5
Part-6 => :[0] Hello Kafka 13
Part-0 => :[0] Hello Kafka 6
Part-0 => :[0] Hello Kafka 14
Part-2 => :[0] Hello Kafka 15
Part-5 => :[0] Hello Kafka 16
Part-4 => :[0] Hello Kafka 17
Part-7 => :[0] Hello Kafka 18
Part-1 => :[0] Hello Kafka 19
Part-3 => :[0] Hello Kafka 20
Part-6 => :[0] Hello Kafka 21
Part-0 => :[0] Hello Kafka 22
Part-2 => :[0] Hello Kafka 23
Part-2 => :[0] Hello Kafka 31
Part-5 => :[0] Hello Kafka 24
Part-5 => :[0] Hello Kafka 32
Part-4 => :[0] Hello Kafka 25
Part-7 => :[0] Hello Kafka 26
Part-1 => :[0] Hello Kafka 27
Part-4 => :[0] Hello Kafka 33
Part-3 => :[0] Hello Kafka 28
Part-6 => :[0] Hello Kafka 29
Part-0 => :[0] Hello Kafka 30
Part-3 => :[0] Hello Kafka 36
Part-2 => :[0] Hello Kafka 39
Part-5 => :[0] Hello Kafka 40
...
```

### Using the Id field as the key

```java
    private static void runProducer(int sendMessageCount, int waitMsInBetween, long id) throws Exception {
        Long key = (id > 0) ? id : null;

        try (Producer<Long, String> producer = createProducer()) {
            for (int index = 0; index < sendMessageCount; index++) {
                long time = System.currentTimeMillis();

                ProducerRecord<Long, String> record
                        = new ProducerRecord<>(TOPIC, key,
                                "[" + id + "] Hello Kafka " + LocalDateTime.now());

                RecordMetadata metadata = producer.send(record).get();

                long elapsedTime = System.currentTimeMillis() - time;
                System.out.printf("[" + id + "] sent record(key=%s value=%s) "
                        + "meta(partition=%d, offset=%d) time=%d\n",
                        record.key(), record.value(), metadata.partition(),
                        metadata.offset(), elapsedTime);

                // Simulate slow processing
                Thread.sleep(waitMsInBetween);
            }
        }
    }
```

### Changing to Asynchronous mode

The following class shows the same logic but this time using the asynchronous way for sending records to Kafka. The difference can be seen in the `runProducer` method.

```
package com.trivadis.kafkaws.producer;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class KafkaProducerASync {

    private final static String TOPIC = "test-java-topic";
    private final static String BOOTSTRAP_SERVERS
            = "dataplatform:9092, dataplatform:9093";

    private static Producer<Long, String> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaExampleProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                LongSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    private static void runProducer(int sendMessageCount, int waitMsInBetween, long id) throws Exception {
        Long key = (id > 0) ? id : null;
        final CountDownLatch countDownLatch = new CountDownLatch(sendMessageCount);

        try (Producer<Long, String> producer = createProducer()) {
            for (int index = 0; index < sendMessageCount; index++) {
                long time = System.currentTimeMillis();

                ProducerRecord<Long, String> record
                        = new ProducerRecord<>(TOPIC, key,
                                "[" + id + "] Hello Kafka " + LocalDateTime.now());

                producer.send(record, (metadata, exception) -> {
                    long elapsedTime = System.currentTimeMillis() - time;
                    if (metadata != null) {
                        System.out.printf("[" + id + "] sent record(key=%s value=%s) "
                                + "meta(partition=%d, offset=%d) time=%d\n",
                                record.key(), record.value(), metadata.partition(),
                                metadata.offset(), elapsedTime);
                    } else {
                        exception.printStackTrace();
                    }
                    countDownLatch.countDown();
                });
            }
        }
    }

    public static void main(String... args) throws Exception {
        if (args.length == 0) {
            runProducer(100, 10, 0);
        } else {
            runProducer(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Long.parseLong(args[2]));
        }
    }
}
```

## Review Producer

- What will happen if the first server is down in the bootstrap list? Can the producer still connect to the other Kafka brokers in the cluster?

- When would you use Kafka async send vs. sync send?

- Why do you need two serializers for a Kafka record?

## Create a Kafka Consumer

Just like we did with the producer, you need to specify bootstrap servers. You also need to define a `group.id` that identifies which consumer group this consumer belongs. Then you need to designate a Kafka record key deserializer and a record value deserializer. Then you need to subscribe the consumer to the topic you created in the producer tutorial.

### Kafka Consumer with Automatic Offset Committing

First create a new Java Package `com.trivadis.kafkaws.consumer` in the folder **src/main/java**.

Create a new Java Class `KafkaConsumerAuto` in the package `com.trivadis.kafkaws.consumer` just created. 

Add the following code to the empty class. 

Now, that we imported the Kafka classes and defined some constants, let’s create a Kafka producer.

First imported the Kafka classes, define some constants and create the Kafka consumer.

```java
package com.trivadis.kafkaws.consumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

public class KafkaConsumerAuto {
    private final static String TOPIC = "test-java-topic";
    private final static String BOOTSTRAP_SERVERS
            = "dataplatform:9092,dataplatform:9093,dataplatform:9094";
    private final static Duration CONSUMER_TIMEOUT = Duration.ofSeconds(1);

    private static Consumer<Long, String> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "KakfaConsumerAuto");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 10000);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // Create the consumer using props.
        Consumer<Long, String> consumer = new KafkaConsumer<>(props);

        // Subscribe to the topic.
        consumer.subscribe(Collections.singletonList(TOPIC));
        return consumer;
    }
```

With that in place, let's process the record with the Kafka Consumer. 

```java
    private static void runConsumer(int waitMsInBetween) throws InterruptedException {
        final int giveUp = 100;

        try (Consumer<Long, String> consumer = createConsumer()) {
            int noRecordsCount = 0;

            while (true) {
                ConsumerRecords<Long, String> consumerRecords = consumer.poll(CONSUMER_TIMEOUT);

                if (consumerRecords.isEmpty()) {
                    noRecordsCount++;
                    if (noRecordsCount > giveUp) {
                        break;
                    } else {
                        continue;
                    }
                }

                consumerRecords.forEach(record -> {
                    System.out.printf("%d - Consumer Record:(Key: %d, Value: %s, Partition: %d, Offset: %d)\n",
                            consumerRecords.count(), record.key(), record.value(),
                            record.partition(), record.offset());
                    try {
                        // Simulate slow processing
                        Thread.sleep(waitMsInBetween);
                    } catch (InterruptedException e) {
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("DONE");
    }
```

Notice you use `ConsumerRecords` which is a group of records from a Kafka topic partition. The `ConsumerRecords` class is a container that holds a list of ConsumerRecord(s) per partition for a particular topic. There is one `ConsumerRecord` list for every topic partition returned by the `consumer.poll()`.

Next you define the main method. You can pass the amount of time the consumer spends for processing each record consumed.

```java
    public static void main(String... args) throws Exception {
        if (args.length == 0) {
            runConsumer(10);
        } else {
            runConsumer(Integer.parseInt(args[0]));
        }
    }
```

The main method just calls `runConsumer`.

Before we run the consumer, let's add a new line to the `log4j.properties` configuration, just right after the `log4j.logger.org.apache.kafka=INFO` line. 

```
log4j.logger.org.apache.kafka.clients.consumer.internals.ConsumerCoordinator=DEBUG
```

If will show a DEBUG message whenever the auto commit is done. 

Before we can run it, add the consumer to the `<executions>` section in the `pom.xml`.

```
					<execution>
						<id>consumer</id>
						<goals>
							<goal>java</goal>
						</goals>
						<configuration>
							<mainClass>com.trivadis.kafkaws.consumer.KafkaConsumerAuto</mainClass>
						</configuration>						
					</execution>
```

Now run it using the `mvn exec:java` command.

```
mvn exec:java@consumer -Dexec.args="0"
```

### Kafka Consumer with Manual Offset Control

Create a new Java Class `KafkaConsumerManual` in the package `com.trivadis.kafkaws.consumer` just created by copy-pasting from the class `KafkaConsumerAuto`.

Replace the `runConsumer()` method with the code below.

```java
    private static void runConsumer(int waitMsInBetween) throws InterruptedException {
        final int giveUp = 100;

        try (Consumer<Long, String> consumer = createConsumer()) {
            int noRecordsCount = 0;

            while (true) {
                ConsumerRecords<Long, String> consumerRecords = consumer.poll(CONSUMER_TIMEOUT);

                if (consumerRecords.isEmpty()) {
                    noRecordsCount++;
                    if (noRecordsCount > giveUp) {
                        break;
                    }
                }

                consumerRecords.forEach(record -> {
                    System.out.printf("%d - Consumer Record:(Key: %d, Value: %s, Partition: %d, Offset: %d)\n",
                            consumerRecords.count(), record.key(), record.value(),
                            record.partition(), record.offset());
                    try {
                        // Simulate slow processing
                        Thread.sleep(waitMsInBetween);
                    } catch (InterruptedException e) {
                    }
                });

                consumer.commitAsync();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("DONE");
    }
```

Make sure to change the Consumer Group (`ConsumerConfig.GROUP_ID_CONFIG`) to `KafkaConsumerManual` and set the `ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG` to `false` in the `createConsumer()` method. 

```java
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "KakfaConsumerManual");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
```

## Try running Producer and Consumer
Run the consumer from your IDE or Terminal (Maven). Then run the producer from above from your IDE or Terminal (Maven). You should see the consumer get the records that the producer sent.

### Three Consumers in same group and one Producer sending 25 messages

Start the consumer 3 times by executing the following command in 3 different terminal windows.

```
mvn exec:java@consumer -Dexec.args="0"
```

and then start the producer

```
mvn exec:java@producer -Dexec.args="25 0 0"
```

#### Producer Output

```
[0] sent record(key=null value=[0] Hello Kafka 0) meta(partition=0, offset=284) time=804
[0] sent record(key=null value=[0] Hello Kafka 1) meta(partition=2, offset=283) time=27
[0] sent record(key=null value=[0] Hello Kafka 2) meta(partition=5, offset=284) time=11
[0] sent record(key=null value=[0] Hello Kafka 3) meta(partition=4, offset=1284) time=65
[0] sent record(key=null value=[0] Hello Kafka 4) meta(partition=7, offset=284) time=26
[0] sent record(key=null value=[0] Hello Kafka 5) meta(partition=1, offset=283) time=8
[0] sent record(key=null value=[0] Hello Kafka 6) meta(partition=3, offset=283) time=18
[0] sent record(key=null value=[0] Hello Kafka 7) meta(partition=6, offset=283) time=16
[0] sent record(key=null value=[0] Hello Kafka 8) meta(partition=0, offset=285) time=19
[0] sent record(key=null value=[0] Hello Kafka 9) meta(partition=2, offset=284) time=17
[0] sent record(key=null value=[0] Hello Kafka 10) meta(partition=5, offset=285) time=21
[0] sent record(key=null value=[0] Hello Kafka 11) meta(partition=4, offset=1285) time=11
[0] sent record(key=null value=[0] Hello Kafka 12) meta(partition=7, offset=285) time=7
[0] sent record(key=null value=[0] Hello Kafka 13) meta(partition=1, offset=284) time=15
[0] sent record(key=null value=[0] Hello Kafka 14) meta(partition=3, offset=284) time=21
[0] sent record(key=null value=[0] Hello Kafka 15) meta(partition=6, offset=284) time=18
[0] sent record(key=null value=[0] Hello Kafka 16) meta(partition=0, offset=286) time=12
[0] sent record(key=null value=[0] Hello Kafka 17) meta(partition=2, offset=285) time=18
[0] sent record(key=null value=[0] Hello Kafka 18) meta(partition=5, offset=286) time=9
[0] sent record(key=null value=[0] Hello Kafka 19) meta(partition=4, offset=1286) time=4
[0] sent record(key=null value=[0] Hello Kafka 20) meta(partition=7, offset=286) time=7
[0] sent record(key=null value=[0] Hello Kafka 21) meta(partition=1, offset=285) time=17
[0] sent record(key=null value=[0] Hello Kafka 22) meta(partition=3, offset=285) time=14
[0] sent record(key=null value=[0] Hello Kafka 23) meta(partition=6, offset=285) time=8
[0] sent record(key=null value=[0] Hello Kafka 24) meta(partition=0, offset=287) time=16
```

#### Consumer 1 Output (same consumer group)
```
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 0, Partition: 0, Offset: 284)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 1, Partition: 2, Offset: 283)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 5, Partition: 1, Offset: 283)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 8, Partition: 0, Offset: 285)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 9, Partition: 2, Offset: 284)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 13, Partition: 1, Offset: 284)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 16, Partition: 0, Offset: 286)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 17, Partition: 2, Offset: 285)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 21, Partition: 1, Offset: 285)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 24, Partition: 0, Offset: 287)
```
#### Consumer 2 Output (same consumer group)

```
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 2, Partition: 5, Offset: 284)
2 - Consumer Record:(Key: null, Value: [0] Hello Kafka 6, Partition: 3, Offset: 283)
2 - Consumer Record:(Key: null, Value: [0] Hello Kafka 3, Partition: 4, Offset: 1284)
2 - Consumer Record:(Key: null, Value: [0] Hello Kafka 10, Partition: 5, Offset: 285)
2 - Consumer Record:(Key: null, Value: [0] Hello Kafka 11, Partition: 4, Offset: 1285)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 14, Partition: 3, Offset: 284)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 18, Partition: 5, Offset: 286)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 19, Partition: 4, Offset: 1286)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 22, Partition: 3, Offset: 285)
```

#### Consumer 3 Output (same consumer group)

```
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 4, Partition: 7, Offset: 284)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 7, Partition: 6, Offset: 283)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 12, Partition: 7, Offset: 285)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 15, Partition: 6, Offset: 284)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 20, Partition: 7, Offset: 286)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 23, Partition: 6, Offset: 285)
```
**Questions**

- Which consumer owns partition 10?
- How many ConsumerRecords objects did Consumer 0 get?
- What is the next offset from Partition 11 that Consumer 2 should get?
- Why does each consumer get unique messages?

### Three Consumers in same group and one Producer sending 10 messages using key

Start the consumer 3 times by executing the following command in 3 different terminal windows.

```
mvn exec:java@consumer -Dexec.args="0"
```

and then start the producer (using 10 for the ID)

```
mvn exec:java@producer -Dexec.args="25 0 10"
```

#### Producer Output

```
[10] sent record(key=10 value=[10] Hello Kafka 0) meta(partition=3, offset=289) time=326
[10] sent record(key=10 value=[10] Hello Kafka 1) meta(partition=3, offset=290) time=7
[10] sent record(key=10 value=[10] Hello Kafka 2) meta(partition=3, offset=291) time=3
[10] sent record(key=10 value=[10] Hello Kafka 3) meta(partition=3, offset=292) time=3
[10] sent record(key=10 value=[10] Hello Kafka 4) meta(partition=3, offset=293) time=3
[10] sent record(key=10 value=[10] Hello Kafka 5) meta(partition=3, offset=294) time=2
[10] sent record(key=10 value=[10] Hello Kafka 6) meta(partition=3, offset=295) time=2
[10] sent record(key=10 value=[10] Hello Kafka 7) meta(partition=3, offset=296) time=2
[10] sent record(key=10 value=[10] Hello Kafka 8) meta(partition=3, offset=297) time=2
[10] sent record(key=10 value=[10] Hello Kafka 9) meta(partition=3, offset=298) time=3
```

#### Consumer 1 Output (same consumer group)
nothing consumed

#### Consumer 2 Output (same consumer group)
```
1 - Consumer Record:(Key: 10, Value: [10] Hello Kafka 0, Partition: 3, Offset: 299)
3 - Consumer Record:(Key: 10, Value: [10] Hello Kafka 1, Partition: 3, Offset: 300)
3 - Consumer Record:(Key: 10, Value: [10] Hello Kafka 2, Partition: 3, Offset: 301)
3 - Consumer Record:(Key: 10, Value: [10] Hello Kafka 3, Partition: 3, Offset: 302)
6 - Consumer Record:(Key: 10, Value: [10] Hello Kafka 4, Partition: 3, Offset: 303)
6 - Consumer Record:(Key: 10, Value: [10] Hello Kafka 5, Partition: 3, Offset: 304)
6 - Consumer Record:(Key: 10, Value: [10] Hello Kafka 6, Partition: 3, Offset: 305)
6 - Consumer Record:(Key: 10, Value: [10] Hello Kafka 7, Partition: 3, Offset: 306)
6 - Consumer Record:(Key: 10, Value: [10] Hello Kafka 8, Partition: 3, Offset: 307)
6 - Consumer Record:(Key: 10, Value: [10] Hello Kafka 9, Partition: 3, Offset: 308)
```

#### Consumer 3 Output (same consumer group)
nothing consumed

**Questions**

- Why is consumer 2 the only one getting data?

## Review Consumer

- How did we demonstrate Consumers in a Consumer Group dividing up topic partitions and sharing them?
- How did we demonstrate Consumers in different Consumer Groups each getting their own offsets?
- How many records does poll get?
- Does a call to poll ever get records from two different partitions?
