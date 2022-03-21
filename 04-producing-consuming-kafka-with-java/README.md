# Using Kafka from Java

In this workshop we will learn how to produce and consume messages using the [Kafka Java API](https://kafka.apache.org/documentation/#api).

We will fist use just the `StringSerializer` to serialize a `java.lang.String` object for the value of the message. In a second step we will change it to serialize a custom Java bean as a JSON document.

## Create the project in your Java IDE

Create a new Maven Project (using the functionality of your favourite IDE) and use `com.trivadis.kafkaws` for the **Group Id** and `java-kafka` for the **Artifact Id**.

Navigate to the **pom.xml** and double-click on it. The POM Editor will be displayed. 

You can either use the GUI to edit your pom.xml or click on the last tab **pom.xml** to switch to the "code view". Let's do that. 

You will see the still rather empty definition.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.trivadis.kafkaws</groupId>
  <artifactId>java-kafka</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</project>
```

Let's add some initial dependencies for our project. We will add some more dependencies to the POM throughout this workshop.

Copy the following block right after the <version> tag, before the closing </project> tag.

```xml
   <properties>
       <kafka.version>2.7.0</kafka.version>
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
				<version>3.0.0</version>
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

## Create log4j settings

Let's also create the necessary log4j configuration. 

In the code we are using the [Log4J Logging Framework](https://logging.apache.org/log4j/2.x/), which we have to configure using a property file. 

Create a new file `log4j.properties` in the folder **src/main/resources** and add the following configuration properties. 

```properties
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

## Creating the necessary Kafka Topic 

We will use the topic `test-java-topic` in the Producer and Consumer code below. Due to the fact that `auto.topic.create.enable` is set to `false`, we have to manually create the topic. 

Connect to the `kafka-1` container

```bash
docker exec -ti kafka-1 bash
```

and execute the necessary kafka-topics command. 

```bash
kafka-topics --create \
    --replication-factor 3 \
    --partitions 8 \
    --topic test-java-topic \
    --zookeeper zookeeper-1:2181
```

Cross check that the topic has been created.

```bash
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
                        = new ProducerRecord<>(TOPIC, 
                        			"[" + id + "] Hello Kafka " + index + " => " + LocalDateTime.now());

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


Now run it using the `mvn exec:java` command. It will generate 1000 messages, waiting 100ms in-between sending each message and use 0 for the ID, which will set the key to `null`. 

```bash
mvn clean package -Dmaven.test.skip=true

mvn exec:java@producer -Dexec.args="1000 100 0"
```

Use `kafkacat` or `kafka-console-consumer` to consume the messages from the topic `test-java-topic`.

```bash
kafkacat -b dataplatform -t test-java-topic -f 'Part-%p => %k:%s\n'
```

```bash
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

If we specify an id <> 0 when runnning the producer, the id is used as the key

```java
    private static void runProducer(int sendMessageCount, int waitMsInBetween, long id) throws Exception {
        Long key = (id > 0) ? id : null;

        try (Producer<Long, String> producer = createProducer()) {
            for (int index = 0; index < sendMessageCount; index++) {
                long time = System.currentTimeMillis();

                ProducerRecord<Long, String> record
                        = new ProducerRecord<>(TOPIC, key, "[" + id + "] Hello Kafka " + index + " => " + LocalDateTime.now());

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

So let's run it for id=`10`

```bash
mvn exec:java@producer -Dexec.args="1000 100 10"
```


### Changing to Asynchronous mode

The following class shows the same logic but this time using the asynchronous way for sending records to Kafka. The difference can be seen in the `runProducer` method.

```java
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
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "KafkaConsumerAuto");
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

```properties
log4j.logger.org.apache.kafka.clients.consumer.internals.ConsumerCoordinator=DEBUG
```

If will show a DEBUG message whenever the auto commit is executed. 

Before we can run it, add the consumer to the `<executions>` section in the `pom.xml`.

```xml
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

```bash
mvn clean package -Dmaven.test.skip=true

mvn exec:java@consumer -Dexec.args="0"
```

on the console you should see an output similar to the one below, with some Debug messages whenever the auto-commit is happening

```bash
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 399 => 2021-08-06T14:33:09.882489, Partition: 2, Offset: 317)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 400 => 2021-08-06T14:33:09.995583, Partition: 7, Offset: 327)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 401 => 2021-08-06T14:33:10.098579, Partition: 1, Offset: 295)
1 - Consumer Record:(Key: null, Value: [0] Hello Kafka 402 => 2021-08-06T14:33:10.206131, Partition: 7, Offset: 328)
[sumer.KafkaConsumerAuto.main()] ConsumerCoordinator            DEBUG [Consumer clientId=consumer-KafkaConsumerAuto-1, groupId=KafkaConsumerAuto] Sending asynchronous auto-commit of offsets {test-java-topic-7=OffsetAndMetadata{offset=329, leaderEpoch=0, metadata=''}, test-java-topic-5=OffsetAndMetadata{offset=299, leaderEpoch=0, metadata=''}, test-java-topic-6=OffsetAndMetadata{offset=318, leaderEpoch=0, metadata=''}, test-java-topic-3=OffsetAndMetadata{offset=295, leaderEpoch=0, metadata=''}, test-java-topic-4=OffsetAndMetadata{offset=317, leaderEpoch=0, metadata=''}, test-java-topic-1=OffsetAndMetadata{offset=296, leaderEpoch=0, metadata=''}, test-java-topic-2=OffsetAndMetadata{offset=318, leaderEpoch=0, metadata=''}, test-java-topic-0=OffsetAndMetadata{offset=284, leaderEpoch=0, metadata=''}}
[sumer.KafkaConsumerAuto.main()] ConsumerCoordinator            DEBUG [Consumer clientId=consumer-KafkaConsumerAuto-1, groupId=KafkaConsumerAuto] Committed offset 318 for partition test-java-topic-6
[sumer.KafkaConsumerAuto.main()] ConsumerCoordinator            DEBUG [Consumer clientId=consumer-KafkaConsumerAuto-1, groupId=KafkaConsumerAuto] Committed offset 296 for partition test-java-topic-1
[sumer.KafkaConsumerAuto.main()] ConsumerCoordinator            DEBUG [Consumer clientId=consumer-KafkaConsumerAuto-1, groupId=KafkaConsumerAuto] Committed offset 329 for partition test-java-topic-7
[
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
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "KafkaConsumerManual");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
```

## Try running Producer and Consumer

Run the consumer from your IDE or Terminal (Maven). Then run the producer from above from your IDE or Terminal (Maven). You should see the consumer get the records that the producer sent.

### Three Consumers in same group and one Producer sending 25 messages

Start the consumer 3 times by executing the following command in 3 different terminal windows.

```bash
mvn exec:java@consumer -Dexec.args="0"
```

and then start the producer

```bash
mvn exec:java@producer -Dexec.args="25 0 0"
```

#### Producer Output

```bash
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

```bash
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

```bash
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

```bash
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

```bash
mvn exec:java@consumer -Dexec.args="0"
```

and then start the producer (using 10 for the ID)

```bash
mvn exec:java@producer -Dexec.args="1000 100 10"
```

#### Producer Output

```bash
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
```bash
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

## Resetting offsets

Let's first remove and recreate the topic, so the offsets will again start at 0. 

```bash
docker exec -ti kafka-1 kafka-topics --delete --topic test-java-topic --zookeeper zookeeper-1:2181

docker exec kafka-1 kafka-topics --create \
    --replication-factor 3 \
    --partitions 8 \
    --topic test-java-topic \
    --zookeeper zookeeper-1:2181
```

Start the a producer to produce 1000 messages with a delay of 100ms between sending each message. We deliberetly only use one partition by specifying a key (`1`). 

Make sure to use the synchronous producer!

```bash
mvn exec:java@producer -Dexec.args="1000 100 1"
```

in the log check the output for a timestamp to use for the reset

```
[1] sent record(key=1 value=[1] Hello Kafka 428 => 2021-09-22T08:19:49.412417) meta(partition=4, offset=428) time=2
[1] sent record(key=1 value=[1] Hello Kafka 429 => 2021-09-22T08:19:49.519555) meta(partition=4, offset=429) time=4
[1] sent record(key=1 value=[1] Hello Kafka 430 => 2021-09-22T08:19:49.625616) meta(partition=4, offset=430) time=5
[1] sent record(key=1 value=[1] Hello Kafka 431 => 2021-09-22T08:19:49.732601) meta(partition=4, offset=431) time=6
[1] sent record(key=1 value=[1] Hello Kafka 432 => 2021-09-22T08:19:49.843735) meta(partition=4, offset=432) time=3
[1] sent record(key=1 value=[1] Hello Kafka 433 => 2021-09-22T08:19:49.951001) meta(partition=4, offset=433) time=3
[1] sent record(key=1 value=[1] Hello Kafka 434 => 2021-09-22T08:19:50.057701) meta(partition=4, offset=434) time=5
[1] sent record(key=1 value=[1] Hello Kafka 435 => 2021-09-22T08:19:50.166302) meta(partition=4, offset=435) time=2
[1] sent record(key=1 value=[1] Hello Kafka 436 => 2021-09-22T08:19:50.273466) meta(partition=4, offset=436) time=3
[1] sent record(key=1 value=[1] Hello Kafka 437 => 2021-09-22T08:19:50.378912) meta(partition=4, offset=437) time=3
```

We wil be using `2021-09-22T08:19:50.057701` later to reset to. So we expect the offset after the reset will be `434`.


First let's consume all thes messages once:

```bash
mvn exec:java@consumer -Dexec.args="0"
```

To change or reset the offsets of a consumer group, the `kafka-consumer-groups` utility has been created. You need specify the topic, consumer group and use the `--reset-offsets`  flag to change the offset.

We can also find it in any of the borkers, similar to the `kafka-topics` CLI. Executing

```bash
docker exec -ti kafka-1 kafka-consumer-groups
```

will display the help page with all the avaialble options

```
$ docker exec -ti kafka-1 kafka-consumer-groups

Missing required argument "[bootstrap-server]"
Option                                  Description                            
------                                  -----------                            
--all-groups                            Apply to all consumer groups.          
--all-topics                            Consider all topics assigned to a      
                                          group in the `reset-offsets` process.
--bootstrap-server <String: server to   REQUIRED: The server(s) to connect to. 
  connect to>                                                                  
--by-duration <String: duration>        Reset offsets to offset by duration    
                                          from current timestamp. Format:      
                                          'PnDTnHnMnS'                         
--command-config <String: command       Property file containing configs to be 
  config property file>                   passed to Admin Client and Consumer. 
--delete                                Pass in groups to delete topic         
                                          partition offsets and ownership      
                                          information over the entire consumer 
                                          group. For instance --group g1 --    
                                          group g2                             
--delete-offsets                        Delete offsets of consumer group.      
                                          Supports one consumer group at the   
                                          time, and multiple topics.           
--describe                              Describe consumer group and list       
                                          offset lag (number of messages not   
                                          yet processed) related to given      
                                          group.                               
--dry-run                               Only show results without executing    
                                          changes on Consumer Groups.          
                                          Supported operations: reset-offsets. 
--execute                               Execute operation. Supported           
                                          operations: reset-offsets.           
--export                                Export operation execution to a CSV    
                                          file. Supported operations: reset-   
                                          offsets.                             
--from-file <String: path to CSV file>  Reset offsets to values defined in CSV 
                                          file.                                
--group <String: consumer group>        The consumer group we wish to act on.  
--help                                  Print usage information.               
--list                                  List all consumer groups.              
--members                               Describe members of the group. This    
                                          option may be used with '--describe' 
                                          and '--bootstrap-server' options     
                                          only.                                
                                        Example: --bootstrap-server localhost: 
                                          9092 --describe --group group1 --    
                                          members                              
--offsets                               Describe the group and list all topic  
                                          partitions in the group along with   
                                          their offset lag. This is the        
                                          default sub-action of and may be     
                                          used with '--describe' and '--       
                                          bootstrap-server' options only.      
                                        Example: --bootstrap-server localhost: 
                                          9092 --describe --group group1 --    
                                          offsets                              
--reset-offsets                         Reset offsets of consumer group.       
                                          Supports one consumer group at the   
                                          time, and instances should be        
                                          inactive                             
                                        Has 2 execution options: --dry-run     
                                          (the default) to plan which offsets  
                                          to reset, and --execute to update    
                                          the offsets. Additionally, the --    
                                          export option is used to export the  
                                          results to a CSV format.             
                                        You must choose one of the following   
                                          reset specifications: --to-datetime, 
                                          --by-period, --to-earliest, --to-    
                                          latest, --shift-by, --from-file, --  
                                          to-current.                          
                                        To define the scope use --all-topics   
                                          or --topic. One scope must be        
                                          specified unless you use '--from-    
                                          file'.                               
--shift-by <Long: number-of-offsets>    Reset offsets shifting current offset  
                                          by 'n', where 'n' can be positive or 
                                          negative.                            
--state [String]                        When specified with '--describe',      
                                          includes the state of the group.     
                                        Example: --bootstrap-server localhost: 
                                          9092 --describe --group group1 --    
                                          state                                
                                        When specified with '--list', it       
                                          displays the state of all groups. It 
                                          can also be used to list groups with 
                                          specific states.                     
                                        Example: --bootstrap-server localhost: 
                                          9092 --list --state stable,empty     
                                        This option may be used with '--       
                                          describe', '--list' and '--bootstrap-
                                          server' options only.                
--timeout <Long: timeout (ms)>          The timeout that can be set for some   
                                          use cases. For example, it can be    
                                          used when describing the group to    
                                          specify the maximum amount of time   
                                          in milliseconds to wait before the   
                                          group stabilizes (when the group is  
                                          just created, or is going through    
                                          some changes). (default: 5000)       
--to-current                            Reset offsets to current offset.       
--to-datetime <String: datetime>        Reset offsets to offset from datetime. 
                                          Format: 'YYYY-MM-DDTHH:mm:SS.sss'    
--to-earliest                           Reset offsets to earliest offset.      
--to-latest                             Reset offsets to latest offset.        
--to-offset <Long: offset>              Reset offsets to a specific offset.    
--topic <String: topic>                 The topic whose consumer group         
                                          information should be deleted or     
                                          topic whose should be included in    
                                          the reset offset process. In `reset- 
                                          offsets` case, partitions can be     
                                          specified using this format: `topic1:
                                          0,1,2`, where 0,1,2 are the          
                                          partition to be included in the      
                                          process. Reset-offsets also supports 
                                          multiple topic inputs.               
--verbose                               Provide additional information, if     
                                          any, when describing the group. This 
                                          option may be used with '--          
                                          offsets'/'--members'/'--state' and   
                                          '--bootstrap-server' options only.   
                                        Example: --bootstrap-server localhost: 
                                          9092 --describe --group group1 --    
                                          members --verbose                    
--version                               Display Kafka version.                 
```

### How to find the current consumer offset?

Use the kafka-consumer-groups along with the consumer group id followed by a describe

```bash
docker exec -ti kafka-1 kafka-consumer-groups --bootstrap-server kafka-2:19093 --group "KafkaConsumerAuto" --verbose --describe
```

You will see 2 entries related to offsets – CURRENT-OFFSET and  LOG-END-OFFSET for the partitions in the topic for that consumer group. CURRENT-OFFSET is the current offset for the partition in the consumer group.

```
$ docker exec -ti kafka-1 kafka-consumer-groups --bootstrap-server kafka-1:19093 --group "KafkaConsumerAuto" --verbose --describe

Consumer group 'KafkaConsumerAuto' has no active members.

GROUP             TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID                                                       HOST            CLIENT-ID
KafkaConsumerAuto test-java-topic 6          0               0               0               consumer-KafkaConsumerAuto-1-e74b46d4-2c43-4278-a006-ae51c779a1e2 /192.168.142.1  consumer-KafkaConsumerAuto-1
KafkaConsumerAuto test-java-topic 1          0               0               0               consumer-KafkaConsumerAuto-1-e74b46d4-2c43-4278-a006-ae51c779a1e2 /192.168.142.1  consumer-KafkaConsumerAuto-1
KafkaConsumerAuto test-java-topic 7          0               0               0               consumer-KafkaConsumerAuto-1-e74b46d4-2c43-4278-a006-ae51c779a1e2 /192.168.142.1  consumer-KafkaConsumerAuto-1
KafkaConsumerAuto test-java-topic 0          0               0               0               consumer-KafkaConsumerAuto-1-e74b46d4-2c43-4278-a006-ae51c779a1e2 /192.168.142.1  consumer-KafkaConsumerAuto-1
KafkaConsumerAuto test-java-topic 4          1000            1000            0               consumer-KafkaConsumerAuto-1-e74b46d4-2c43-4278-a006-ae51c779a1e2 /192.168.142.1  consumer-KafkaConsumerAuto-1
KafkaConsumerAuto test-java-topic 3          0               0               0               consumer-KafkaConsumerAuto-1-e74b46d4-2c43-4278-a006-ae51c779a1e2 /192.168.142.1  consumer-KafkaConsumerAuto-1
KafkaConsumerAuto test-java-topic 5          0               0               0               consumer-KafkaConsumerAuto-1-e74b46d4-2c43-4278-a006-ae51c779a1e2 /192.168.142.1  consumer-KafkaConsumerAuto-1
KafkaConsumerAuto test-java-topic 2          0               0               0               consumer-KafkaConsumerAuto-1-e74b46d4-2c43-4278-a006-ae51c779a1e2 /192.168.142.1  consumer-KafkaConsumerAuto-1
```

We can see that all messages were sent to partition 4. 

### Reset offsets to a timestamp

Let's reset the offsets to the timestamp `2021-09-22T08:19:50.057701` we noted above, by rounding it down to the minute. You also have to adapt the timestamp to UTC timestamp (currently -2)

```bash
docker exec -ti kafka-1 kafka-consumer-groups --bootstrap-server kafka-1:19092 --group "KafkaConsumerAuto" --reset-offsets --to-datetime 2021-09-22T06:19:50.00Z --topic test-java-topic --execute
```

You should see the `NEW-OFFST` changed to `434`

```bash
docker exec -ti kafka-1 kafka-consumer-groups --bootstrap-server kafka-1:19092 --group "KafkaConsumerAuto" --reset-offsets --to-datetime 2021-09-22T06:19:50.00Z --topic test-java-topic --execute

GROUP                          TOPIC                          PARTITION  NEW-OFFSET     
KafkaConsumerAuto              test-java-topic                6          0              
KafkaConsumerAuto              test-java-topic                1          0              
KafkaConsumerAuto              test-java-topic                7          0              
KafkaConsumerAuto              test-java-topic                0          0              
KafkaConsumerAuto              test-java-topic                4          434            
KafkaConsumerAuto              test-java-topic                3          0              
KafkaConsumerAuto              test-java-topic                5          0              
KafkaConsumerAuto              test-java-topic                2          0 
```

Now lets rerun the consumer and you should see the first message being consumed to be the following one

```
500 - Consumer Record:(Key: 1, Value: [1] Hello Kafka 434 => 2021-09-22T08:19:50.057701, Partition: 4, Offset: 434)
5
```

### Reset offsets to offset to earliest

This command resets the offsets to earliest (oldest) offset available in the topic.

```bash
docker exec -ti kafka-1 kafka-consumer-groups --bootstrap-server kafka-1:19092 --group "KafkaConsumerAuto" --reset-offsets --to-earliest --topic test-java-topic --execute
```

Rerun the consumer to check that again all messages are consumed.



There are many other resetting options, run kafka-consumer-groups for details

```
    --shift-by
    --to-current
    --to-latest
    --to-offset
    --by-duration
```

## Changing the Value Serialization/Deserialization to use JSON

So far we have used the `StringSerializer`/`StringDeserializer` to serialize and deserialize a `java.lang.String` for the value of the message. This of course is not really useful in practice, as we want to send complex objects over Kafka. One way to do that is serializing a complex object as JSON on the producer side, send it and doing the opposite on the consumer side. This can be done with or without a schema. In this workshop we show the schema-less version with, in [Workshop Working with Avro and Java](../04a-working-with-avro-and-java) we will see how to work with Schema-based messages together with a schema registry.

### Add Maven dependency

For the JSON serialization we will be using the Jackson library. So let's add that as a dependency to the `pom.xml`

```xml
<!-- https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.12.4</version>
</dependency>
```

### Create a Notification class

Create a new Java class which will hold our notification in 3 different fields. We annotate these 3 fields with `@JsonProperty` so that they will be serialized later.

```java
package com.trivadis.kafkaws;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Notification {
    @JsonProperty
    private long id;
    @JsonProperty
    private String message;
    @JsonProperty
    private String createdAt;

    public Notification() {};

    public Notification(long id, String message, String createdAt) {
        this.id = id;
        this.message = message;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", message='" + message + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }
}
```

### Create the custom JsonSerializer and JsonDeserializer implemenations

Next we have to create the custom serializer/deseralizer handling the JSON serialization and deserialization.

First create a new package `serde`.

Create a new class for the serializer in the `serde` package

```java
package com.trivadis.kafkaws.serde;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trivadis.kafkaws.Notification;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.util.Map;

public class JsonSerializer<T> implements Serializer<T> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JsonSerializer() {
        //Nothing to do
    }

    @Override
    public void configure(Map<String, ?> config, boolean isKey) {
        //Nothing to Configure
    }

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error serializing JSON message", e);
        }
    }

    @Override
    public void close() {

    }
}
```

And another new class for the deserializer also in the `serde` package


```java
package com.trivadis.kafkaws.serde;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.util.Map;

public class JsonDeserializer<T> implements Deserializer<T> {
    private Logger logger = LogManager.getLogger(this.getClass());
    private Class <T> type;

    public JsonDeserializer(Class type) {
        this.type = type;
    }

    @Override
    public void configure(Map map, boolean b) {

    }

    @Override
    public T deserialize(String s, byte[] bytes) {
        ObjectMapper mapper = new ObjectMapper();
        T obj = null;
        try {
            obj = mapper.readValue(bytes, type);
        } catch (Exception e) {

            logger.error(e.getMessage());
        }
        return obj;
    }

    @Override
    public void close() {

    }
}
```

### Create the new Producer using the JsonSerializer

Create a new class `KafkaProducerJson` next to the exiting producer implementation

```java
package com.trivadis.kafkaws.producer;

import com.trivadis.kafkaws.Notification;
import com.trivadis.kafkaws.serde.JsonSerializer;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.LongSerializer;

import java.time.LocalDateTime;
import java.util.Properties;

public class KafkaProducerJson {

    private final static String TOPIC = "test-java-json-topic";
    private final static String BOOTSTRAP_SERVERS
            = "dataplatform:9092,dataplatform:9093";

    private static Producer<Long, Notification> createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "KafkaExampleProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                LongSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class.getName());
        return new KafkaProducer<>(props);
    }

    private static void runProducer(int sendMessageCount, int waitMsInBetween, long id) throws Exception {
        Long key = (id > 0) ? id : null;

        try (Producer<Long, Notification> producer = createProducer()) {
            for (int index = 0; index < sendMessageCount; index++) {
                long time = System.currentTimeMillis();

                Notification notification = new Notification(id, "[" + id + "] Hello Kafka " + index, LocalDateTime.now().toString());

                ProducerRecord<Long, Notification> record
                        = new ProducerRecord<>(TOPIC, key, notification);

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

    public static void main(String... args) throws Exception {
        if (args.length == 0) {
            runProducer(100, 10, 0);
        } else {
            runProducer(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Long.parseLong(args[2]));
        }
    }
}
```

Changes to the previous producer implementation are in the configuration were we use the `JsonSerializer`

```java
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class.getName());
```

and with all the generics, where we use `<Long, Notifcation>` instead of `<Long, String>`. Additionally of course for sending the value of the Kafka record, we create a `Notification` object.

We also use another topic `test-java-json-topic`, so we clearly separate the different messages.

### Creating the new Kafka Topic 

We show a shortcut version here, instead of first "connecting" to the container and then issuing the `kafka-topics` command, we do it in one

```bash
docker exec -ti kafka-1 kafka-topics --create \
    --replication-factor 3 \
    --partitions 8 \
    --topic test-java-topic \
    --zookeeper zookeeper-1:2181
```

### Testing the JsonProducer

To test the producer, let's also add a new section to the `executions` of the `pom.xml`

```xml
                    <execution>
                        <id>producer-json</id>
                        <goals>
                            <goal>java</goal>
                        </goals>
                        <configuration>
                            <mainClass>com.trivadis.kafkaws.producer.KafkaProducerJson</mainClass>
                        </configuration>
                    </execution>
```

Now run it using the `mvn exec:java` command. The arguments again tell to create `100` messages, with a delay of `100ms` inbetween and using `null` for the key:

```bash
mvn clean package -Dmaven.test.skip=true

mvn exec:java@producer-json -Dexec.args="100 100 0"
```

Use `kafkacat` to consume the messages from the topic `test-java-json-topic`.

```bash
kafkacat -b dataplatform -t test-java-json-topic -q
```

you can see the messages are all formated as JSON documents

```bash
{"id":0,"message":"[0] Hello Kafka 750","createdAt":"2021-08-06T18:38:10.679026"}
{"id":0,"message":"[0] Hello Kafka 752","createdAt":"2021-08-06T18:38:10.892763"}
{"id":0,"message":"[0] Hello Kafka 755","createdAt":"2021-08-06T18:38:11.207189"}
{"id":0,"message":"[0] Hello Kafka 758","createdAt":"2021-08-06T18:38:11.527921"}
{"id":0,"message":"[0] Hello Kafka 766","createdAt":"2021-08-06T18:38:12.379010"}
{"id":0,"message":"[0] Hello Kafka 776","createdAt":"2021-08-06T18:38:13.442783"}
{"id":0,"message":"[0] Hello Kafka 781","createdAt":"2021-08-06T18:38:13.973201"}
```

This completes the producer, let's now implement the corresponding consumer.

### Create the Consumer

Create a new class `KafkaConsumerJson` next to the exiting consumer implementation. We use the implementation of the non-auto-commit version and adapt it

```java
package com.trivadis.kafkaws.consumer;

import com.trivadis.kafkaws.Notification;
import com.trivadis.kafkaws.serde.JsonDeserializer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaConsumerJson {

    private final static String TOPIC = "test-java-json-topic";
    private final static String BOOTSTRAP_SERVERS
            = "dataplatform:9092,dataplatform:9093,dataplatform:9094";
    private final static Duration CONSUMER_TIMEOUT = Duration.ofSeconds(1);

    private static Consumer<Long, Notification> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, BOOTSTRAP_SERVERS);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "KafkaConsumerJson");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 10000);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());

        // Create the consumer using props.
        Consumer<Long, Notification> consumer = new KafkaConsumer<>(props, new LongDeserializer(), new JsonDeserializer<Notification>(Notification.class));

        // Subscribe to the topic.
        consumer.subscribe(Collections.singletonList(TOPIC));
        return consumer;
    }

    private static void runConsumer(int waitMsInBetween) throws InterruptedException {
        final int giveUp = 100;

        try (Consumer<Long, Notification> consumer = createConsumer()) {
            int noRecordsCount = 0;

            while (true) {
                ConsumerRecords<Long, Notification> consumerRecords = consumer.poll(CONSUMER_TIMEOUT);

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

                consumer.commitAsync();                
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

Changes to the previous consumer implementation are again in the configuration were we use the `JsonDeserializer`

```java
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class.getName());
```

Additionally when we create the `KafkaConsumer`, we have to initialze an instance of `JsonDeserializer` and pass it as an argument to the constructor

```java
        Consumer<Long, Notification> consumer = new KafkaConsumer<>(props, new LongDeserializer(), new JsonDeserializer<Notification>(Notification.class));
```

And again we change all the generics, where we use `<Long, Notifcation>` instead of `<Long, String>`. 

### Testing the JsonProducer

To test the consumer, let's also add a new section to the `executions` of the `pom.xml`

```xml
                    <execution>
                        <id>consumer-json</id>
                        <goals>
                            <goal>java</goal>
                        </goals>
                        <configuration>
                            <mainClass>com.trivadis.kafkaws.consumer.KafkaConsumerJson</mainClass>
                        </configuration>
                    </execution>
```

Now run it using the `mvn exec:java` command

```bash
mvn clean package -Dmaven.test.skip=true

mvn exec:java@consumer-json -Dexec.args="0"
```

and you should see the `toString()` output of the `Notification` instances, similar to the one shown below

```bash
190 - Consumer Record:(Key: null, Value: Notification{id=0, message='[0] Hello Kafka 742', createdAt='2021-08-06T18:38:09.825359'}, Partition: 1, Offset: 239)
190 - Consumer Record:(Key: null, Value: Notification{id=0, message='[0] Hello Kafka 745', createdAt='2021-08-06T18:38:10.145740'}, Partition: 1, Offset: 240)
190 - Consumer Record:(Key: null, Value: Notification{id=0, message='[0] Hello Kafka 753', createdAt='2021-08-06T18:38:10.999308'}, Partition: 1, Offset: 241)
190 - Consumer Record:(Key: null, Value: Notification{id=0, message='[0] Hello Kafka 761', createdAt='2021-08-06T18:38:11.847942'}, Partition: 1, Offset: 242)
190 - Consumer Record:(Key: null, Value: Notification{id=0, message='[0] Hello Kafka 768', createdAt='2021-08-06T18:38:12.591090'}, Partition: 1, Offset: 243)
190 - Consumer Record:(Key: null, Value: Notification{id=0, message='[0] Hello Kafka 774', createdAt='2021-08-06T18:38:13.233265'}, Partition: 1, Offset: 244)
```


