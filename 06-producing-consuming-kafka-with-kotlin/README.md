# Working with Kafka from Kotlin

In this workshop we will learn how to produce and consume messages using the [Kafka Java API](https://kafka.apache.org/documentation/#api).

## Create the project in your favourite IDE

Create a new Maven Project (using the functionality of your favourite IDE) and use `com.trivadis.kafkaws` for the **Group Id** and `kotlin-simple-kafka` for the **Artifact Id**.

Navigate to the **pom.xml** and double-click on it. The POM Editor will be displayed.

You can either use the GUI to edit your pom.xml or click on the last tab **pom.xml** to switch to the "code view". Let's do that.

You will see the still rather empty definition.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>com.trivadis.kafkaws</groupId>
  <artifactId>kotlin-simple-kafka</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</project>
```

Let's add some initial dependencies for our project. We will add some more dependencies to the POM throughout this workshop.

Copy the following block right after the <version> tag, before the closing </project> tag.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xmlns="http://maven.apache.org/POM/4.0.0"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.trivadis.kafkaws</groupId>
    <artifactId>kotlin-simple-kafka</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <packaging>jar</packaging>

    <name>Kotlin Kafka application</name>

    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <kotlin.code.style>official</kotlin.code.style>
        <kotlin.compiler.jvmTarget>1.8</kotlin.compiler.jvmTarget>
        <kotlin.version>1.6.21</kotlin.version>
        <kafka.version>2.8.0</kafka.version>        
    </properties>

    <repositories>
        <repository>
            <id>mavenCentral</id>
            <url>https://repo1.maven.org/maven2/</url>
        </repository>
    </repositories>

    <build>
        <sourceDirectory>src/main/kotlin</sourceDirectory>
        <testSourceDirectory>src/test/kotlin</testSourceDirectory>
        <plugins>
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
                <version>${kotlin.version}</version>
                <executions>
                    <execution>
                        <id>compile</id>
                        <phase>compile</phase>
                        <goals>
                            <goal>compile</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>test-compile</id>
                        <phase>test-compile</phase>
                        <goals>
                            <goal>test-compile</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>2.22.2</version>
            </plugin>
            <plugin>
                <artifactId>maven-failsafe-plugin</artifactId>
                <version>2.22.2</version>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <source>${java.version}</source>
                    <target>${java.version}</target>
                </configuration>
            </plugin>            
        </plugins>
    </build>

    <dependencies>
        <dependency>
            <groupId>org.apache.kafka</groupId>
            <artifactId>kafka-clients</artifactId>
            <version>${kafka.version}</version>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-stdlib</artifactId>
            <version>${kotlin.version}</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>1.7.6</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-log4j12</artifactId>
            <version>1.7.6</version>
        </dependency>
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-test-junit5</artifactId>
            <version>1.6.20</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <version>5.6.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
```

## Creating the necessary Kafka Topic

We will use the topic `test-kotlin-topic` in the Producer and Consumer code below. Due to the fact that `auto.topic.create.enable` is set to `false`, we have to manually create the topic.

Connect to the `kafka-1` container

```bash
docker exec -ti kafka-1 bash
```

and execute the necessary kafka-topics command.

```bash
kafka-topics --create \
    --replication-factor 3 \
    --partitions 12 \
    --topic test-kotlin-topic \
    --bootstrap-server kafka-1:19092
```

Cross check that the topic has been created.

```bash
kafka-topics --list --bootstrap-server kafka-1:19092
```

This finishes the setup steps and our new project is ready to be used. Next we will start implementing a **Kafka Producer**.

## Create a Kafka Producer

Let's first create a producer in synchronous mode.

### Using the Synchronous mode

First create a new Package `com.trivadis.kafkaws.producer` in the folder **src/main/kotlin**.

Create a new Kotlin File `KafkaProducerSync` in the package `com.trivadis.kafkaws.producer` just created.

Add the following code to the empty file to start with the Kafka Producer, specifying the bootstrap server and topic constants

```kotlin
@file:JvmName("KafkaProducerSync")
package com.trivadis.kafkaws.producer

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.*
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.StringSerializer
import java.time.LocalDateTime
import java.util.*

private val TOPIC = "test-kotlin-topic"
private val BOOTSTRAP_SERVERS = "dataplatform:9092,dataplatform:9093"

```

Kafka provides a synchronous send method to send a record to a topic. Let’s use this method to send a number of messages to the Kafka topic we created earlier

```kotlin
fun runProducer(sendMessageCount: Int, waitMsInBetween: Long, id: Long) {
    // Define properties.
    val props = Properties()
    props[BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    props[ACKS_CONFIG] = "all"
    props[KEY_SERIALIZER_CLASS_CONFIG] = LongSerializer::class.qualifiedName
    props[VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.qualifiedName

    KafkaProducer<Long, String>(props).use { producer ->
        repeat(sendMessageCount) { index ->
            val time = System.currentTimeMillis();

            val value = "[" + id + "] Hello Kafka " + index + " => " + LocalDateTime.now()

            val m = producer.send(ProducerRecord(TOPIC, value)).get()

            val elapsedTime = System.currentTimeMillis() - time;
            println("Produced record to topic ${m.topic()} partition [${m.partition()}] @ offset ${m.offset()} time=${elapsedTime}")

            // Simulate slow processing
            Thread.sleep(waitMsInBetween);
        }

        producer.flush()

    }
}
```

Next you define the main method.

```kotlin
fun main(args: Array<String>) {
    if (args.size == 0) {
        runProducer(100, 10, 0)
    } else {
        runProducer(args[0].toInt(), args[1].toLong(), args[2].toLong())
    }
}
```

The `main()` method accepts 3 parameters, the number of messages to produce, the time in ms to wait in-between sending each message and the ID of the producer.

Add the `exec-maven-plugin` to the maven plugins

```xml
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
```

Now run it using the `mvn exec:java` command. It will generate 1000 messages, waiting 100ms in-between sending each message and use 0 for the ID, which will set the key to `null`.

```bash
mvn clean package -Dmaven.test.skip=true

mvn exec:java@producer -Dexec.args="1000 100 0"
```

**Note**: if your maven version does not support profiles, then you have to specify the main class as well: `mvn exec:java -Dexec.mainClass="com.trivadis.kafkaws.producer.KafkaProducerSync" -Dexec.args="1000 100 0"`

Use `kcat` or `kafka-console-consumer` to consume the messages from the topic `test-kotlin-topic`.

```bash
kcat -b dataplatform -t test-kotlin-topic -f 'Part-%p => %k:%s\n' -q
```

```bash
Part-0 => :[0] Hello Kafka 11 => 2022-06-04T09:59:54.746099
Part-0 => :[0] Hello Kafka 19 => 2022-06-04T09:59:55.714062
Part-0 => :[0] Hello Kafka 22 => 2022-06-04T09:59:56.075383
Part-0 => :[0] Hello Kafka 28 => 2022-06-04T09:59:56.796708
Part-0 => :[0] Hello Kafka 30 => 2022-06-04T09:59:57.059571
Part-0 => :[0] Hello Kafka 36 => 2022-06-04T09:59:57.788802
Part-0 => :[0] Hello Kafka 42 => 2022-06-04T09:59:58.505882
Part-0 => :[0] Hello Kafka 44 => 2022-06-04T09:59:58.749233
Part-0 => :[0] Hello Kafka 51 => 2022-06-04T09:59:59.618871
Part-0 => :[0] Hello Kafka 53 => 2022-06-04T09:59:59.861744
Part-0 => :[0] Hello Kafka 58 => 2022-06-04T10:00:00.495284
Part-0 => :[0] Hello Kafka 63 => 2022-06-04T10:00:01.091603
Part-0 => :[0] Hello Kafka 74 => 2022-06-04T10:00:02.458033
Part-0 => :[0] Hello Kafka 86 => 2022-06-04T10:00:03.933116
Part-2 => :[0] Hello Kafka 1 => 2022-06-04T09:59:53.509753
Part-2 => :[0] Hello Kafka 5 => 2022-06-04T09:59:54.025464
Part-2 => :[0] Hello Kafka 12 => 2022-06-04T09:59:54.874822
Part-2 => :[0] Hello Kafka 20 => 2022-06-04T09:59:55.829419
Part-2 => :[0] Hello Kafka 26 => 2022-06-04T09:59:56.559343
Part-2 => :[0] Hello Kafka 31 => 2022-06-04T09:59:57.180112
Part-2 => :[0] Hello Kafka 35 => 2022-06-04T09:59:57.671466
Part-2 => :[0] Hello Kafka 49 => 2022-06-04T09:59:59.372685
Part-2 => :[0] Hello Kafka 61 => 2022-06-04T10:00:00.840579
Part-2 => :[0] Hello Kafka 69 => 2022-06-04T10:00:01.821051
Part-2 => :[0] Hello Kafka 96 => 2022-06-04T10:00:05.141390
Part-1 => :[0] Hello Kafka 2 => 2022-06-04T09:59:53.639559
Part-1 => :[0] Hello Kafka 14 => 2022-06-04T09:59:55.119045
Part-1 => :[0] Hello Kafka 34 => 2022-06-04T09:59:57.553149
Part-1 => :[0] Hello Kafka 41 => 2022-06-04T09:59:58.382445
Part-1 => :[0] Hello Kafka 45 => 2022-06-04T09:59:58.871874
Part-1 => :[0] Hello Kafka 47 => 2022-06-04T09:59:59.121389
Part-1 => :[0] Hello Kafka 57 => 2022-06-04T10:00:00.378376
Part-1 => :[0] Hello Kafka 60 => 2022-06-04T10:00:00.726158
Part-1 => :[0] Hello Kafka 64 => 2022-06-04T10:00:01.211135
Part-1 => :[0] Hello Kafka 77 => 2022-06-04T10:00:02.842668
Part-1 => :[0] Hello Kafka 80 => 2022-06-04T10:00:03.202738
...
```

### Using the Id field as the key

If we specify an id <> `0` when runnning the producer, the id is used as the key

```kotlin
fun runProducer(sendMessageCount: Int, waitMsInBetween: Int, id: Long) {
    // Define properties.
    val props = Properties()
    props[BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    props[ACKS_CONFIG] = "all"
    props[KEY_SERIALIZER_CLASS_CONFIG] = LongSerializer::class.qualifiedName
    props[VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.qualifiedName

    val key = if (id > 0) id else null

    KafkaProducer<Long, String>(props).use { producer ->
        repeat(sendMessageCount) { index ->
            val time = System.currentTimeMillis();

            val value = "[" + id + "] Hello Kafka " + index + " => " + LocalDateTime.now()

            val m = producer.send(ProducerRecord(TOPIC, key, value)).get()
```

So let's run it for id=`10`

```bash
mvn exec:java@producer -Dexec.args="1000 100 10"
```

you should see that now all messages will be sent to the same partition. 

### Changing to Asynchronous mode

The following kotlin file shows the same logic but this time using the asynchronous way for sending records to Kafka. The difference can be seen in the `runProducerAsync` method.

```kotlin
@file:JvmName("KafkaProducerAsync")
package com.trivadis.kafkaws.producer

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.*
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.apache.kafka.common.serialization.LongSerializer
import org.apache.kafka.common.serialization.StringSerializer
import java.time.LocalDateTime
import java.util.*

private val TOPIC = "test-kotlin-topic"
private val BOOTSTRAP_SERVERS = "dataplatform:9092,dataplatform:9093"

fun runProducerAsync(sendMessageCount: Int, waitMsInBetween: Long, id: Long) {
    // Define properties.
    val props = Properties()
    props[BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    props[ACKS_CONFIG] = "all"
    props[KEY_SERIALIZER_CLASS_CONFIG] = LongSerializer::class.qualifiedName
    props[VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.qualifiedName

    val key = if (id > 0) id else null

    KafkaProducer<Long, String>(props).use { producer ->
        repeat(sendMessageCount) { index ->
            val time = System.currentTimeMillis();

            val value = "[" + id + "] Hello Kafka " + index + " => " + LocalDateTime.now()

            producer.send(ProducerRecord(TOPIC, key, value)) { m: RecordMetadata, e: Exception? ->
                when (e) {
                    // no exception, good to go!
                    null -> {
                        val elapsedTime = System.currentTimeMillis() - time;
                        println("Produced record to topic ${m.topic()} partition [${m.partition()}] @ offset ${m.offset()} time=${elapsedTime}")
                    }
                    // print stacktrace in case of exception
                    else -> e.printStackTrace()
                }
            }
        }

        producer.flush()

    }
}

fun main(args: Array<String>) {
    if (args.size == 0) {
        runProducerAsync(100, 10, 0)
    } else {
        runProducerAsync(args[0].toInt(), args[1].toLong(), args[2].toLong())
    }
}

```

add a new profile to the `pom.xml`

```xml
                    <execution>
                        <id>producer-async</id>
                        <goals>
                            <goal>java</goal>
                        </goals>
                        <configuration>
                            <mainClass>com.trivadis.kafkaws.producer.KafkaProducerAsync</mainClass>
                        </configuration>
                    </execution>
```

And now build and run it for id=`10`

```bash
mvn clean package

mvn exec:java@producer-async -Dexec.args="1000 100 10"
```


## Create a Kafka Consumer

Just like we did with the producer, you need to specify bootstrap servers. You also need to define a `group.id` that identifies which consumer group this consumer belongs. Then you need to designate a Kafka record key deserializer and a record value deserializer. Then you need to subscribe the consumer to the topic you created in the producer tutorial.

### Kafka Consumer with Automatic Offset Committing

First create a new Package `com.trivadis.kafkaws.consumer` in the folder **src/main/kotlin**.

Create a new Kotlin file `KafkaConsumerAuto` in the package `com.trivadis.kafkaws.consumer` just created.

Add the following code to the empty class.

Now, that we imported the Kafka classes and defined some constants, let’s create a Kafka producer.

First imported the Kafka classes, define some constants and create the Kafka consumer.

```kotlin
@file:JvmName("KafkaConsumerAuto")
package com.trivadis.kafkaws.consumer

import org.apache.kafka.clients.consumer.ConsumerConfig.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.*


private val TOPIC = "test-kotlin-topic"
private val BOOTSTRAP_SERVERS = "dataplatform:9092,dataplatform:9093"

fun runConsumer(waitMsInBetween: Long) {
    // Define properties.
    val props = Properties()
    props[BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    props[GROUP_ID_CONFIG] = "kotlin-simple-consumer"
    props[AUTO_COMMIT_INTERVAL_MS_CONFIG] = 10000
    props[KEY_DESERIALIZER_CLASS_CONFIG] = LongDeserializer::class.qualifiedName
    props[VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.qualifiedName

    val consumer = KafkaConsumer<Long, String>(props).apply {
        subscribe(listOf(TOPIC))
    }

    consumer.use {
        while(true) {
            val messages = consumer.poll(Duration.ofMillis(100))

            messages.forEach {
                println("Consumed record [Key: ${it.key()}, Value: ${it.value()}] @ Partition: ${it.partition()}, Offset:  ${it.offset()}")
            }

            // Simulate slow processing
            Thread.sleep(waitMsInBetween);
        }
    }
}

fun main(args: Array<String>) {
    if (args.size == 0) {
        runConsumer(10)
    } else {
        runConsumer(args[0].toInt())
    }
}
```

We configured the consumer to use the `auto.commit` mode (by not setting it, as it is by default set to `true`) and set the commit interval to `10 seconds`.

Next you define the main method. You can pass the amount of time the consumer spends for processing each record consumed.

```kotlin
fun main(args: Array<String>) {
    if (args.size == 0) {
        runConsumer(10)
    } else {
        runConsumer(args[0].toInt())
    }
}
```

The main method just calls `runConsumer`.

Before we run the consumer, let's add a configuration in `log4j.properties`, by creating that file in `src/main/resources` and add the following lines

```properties
log4j.rootLogger=INFO, out

# CONSOLE appender not used by default
log4j.appender.out=org.apache.log4j.ConsoleAppender
log4j.appender.out.layout=org.apache.log4j.PatternLayout
log4j.appender.out.layout.ConversionPattern=[%30.30t] %-30.30c{1} %-5p %m%n

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

**Note**: if your maven version does not support profiles, then you have to specify the main class as well: `mvn exec:java -Dexec.mainClass="com.trivadis.kafkaws.consumer.KafkaConsumerAuto" -Dexec.args="0"`

on the console you should see an output similar to the one below, with some Debug messages whenever the auto-commit is happening

```bash
Consumed record [Key: 10, Value: [10] Hello Kafka 155 => 2022-06-04T16:17:54.787506] @ Partition: 3, Offset:  12476
Consumed record [Key: 10, Value: [10] Hello Kafka 156 => 2022-06-04T16:17:54.998856] @ Partition: 3, Offset:  12477
Consumed record [Key: 10, Value: [10] Hello Kafka 157 => 2022-06-04T16:17:55.125173] @ Partition: 3, Offset:  12478
Consumed record [Key: 10, Value: [10] Hello Kafka 158 => 2022-06-04T16:17:55.252868] @ Partition: 3, Offset:  12479
Consumed record [Key: 10, Value: [10] Hello Kafka 159 => 2022-06-04T16:17:55.376300] @ Partition: 3, Offset:  12480
Consumed record [Key: 10, Value: [10] Hello Kafka 160 => 2022-06-04T16:17:55.500777] @ Partition: 3, Offset:  12481
Consumed record [Key: 10, Value: [10] Hello Kafka 161 => 2022-06-04T16:17:55.621729] @ Partition: 3, Offset:  12482
Consumed record [Key: 10, Value: [10] Hello Kafka 162 => 2022-06-04T16:17:55.749756] @ Partition: 3, Offset:  12483
Consumed record [Key: 10, Value: [10] Hello Kafka 163 => 2022-06-04T16:17:55.873920] @ Partition: 3, Offset:  12484
Consumed record [Key: 10, Value: [10] Hello Kafka 164 => 2022-06-04T16:17:56.005853] @ Partition: 3, Offset:  12485
[sumer.KafkaConsumerAuto.main()] ConsumerCoordinator            DEBUG [Consumer clientId=consumer-kotlin-simple-consumer-1, groupId=kotlin-simple-consumer] Sending asynchronous auto-commit of offsets {test-kotlin-topic-1=OffsetAndMetadata{offset=323, leaderEpoch=null, metadata=''}, test-kotlin-topic-0=OffsetAndMetadata{offset=305, leaderEpoch=null, metadata=''}, test-kotlin-topic-3=OffsetAndMetadata{offset=12486, leaderEpoch=10, metadata=''}, test-kotlin-topic-2=OffsetAndMetadata{offset=320, leaderEpoch=null, metadata=''}, test-kotlin-topic-5=OffsetAndMetadata{offset=289, leaderEpoch=null, metadata=''}, test-kotlin-topic-4=OffsetAndMetadata{offset=1324, leaderEpoch=null, metadata=''}, test-kotlin-topic-7=OffsetAndMetadata{offset=307, leaderEpoch=null, metadata=''}, test-kotlin-topic-6=OffsetAndMetadata{offset=323, leaderEpoch=null, metadata=''}}
Consumed record [Key: 10, Value: [10] Hello Kafka 165 => 2022-06-04T16:17:56.122556] @ Partition: 3, Offset:  12486
[sumer.KafkaConsumerAuto.main()] ConsumerCoordinator            DEBUG [Consumer clientId=consumer-kotlin-simple-consumer-1, groupId=kotlin-simple-consumer] Committed offset 1324 for partition test-kotlin-topic-4
[sumer.KafkaConsumerAuto.main()] ConsumerCoordinator            DEBUG [Consumer clientId=consumer-kotlin-simple-consumer-1, groupId=kotlin-simple-consumer] Committed offset 305 for partition test-kotlin-topic-0
[sumer.KafkaConsumerAuto.main()] ConsumerCoordinator            DEBUG [Consumer clientId=consumer-kotlin-simple-consumer-1, groupId=kotlin-simple-consumer] Committed offset 323 for partition test-kotlin-topic-1
[sumer.KafkaConsumerAuto.main()] ConsumerCoordinator            DEBUG [Consumer clientId=consumer-kotlin-simple-consumer-1, groupId=kotlin-simple-consumer] Committed offset 289 for partition test-kotlin-topic-5
[sumer.KafkaConsumerAuto.main()] ConsumerCoordinator            DEBUG [Consumer clientId=consumer-kotlin-simple-consumer-1, groupId=kotlin-simple-consumer] Committed offset 323 for partition test-kotlin-topic-6
[sumer.KafkaConsumerAuto.main()] ConsumerCoordinator            DEBUG [Consumer clientId=consumer-kotlin-simple-consumer-1, groupId=kotlin-simple-consumer] Committed offset 12486 for partition test-kotlin-topic-3
[sumer.KafkaConsumerAuto.main()] ConsumerCoordinator            DEBUG [Consumer clientId=consumer-kotlin-simple-consumer-1, groupId=kotlin-simple-consumer] Committed offset 320 for partition test-kotlin-topic-2
[sumer.KafkaConsumerAuto.main()] ConsumerCoordinator            DEBUG [Consumer clientId=consumer-kotlin-simple-consumer-1, groupId=kotlin-simple-consumer] Committed offset 307 for partition test-kotlin-topic-7
[sumer.KafkaConsumerAuto.main()] ConsumerCoordinator            DEBUG [Consumer clientId=consumer-kotlin-simple-consumer-1, groupId=kotlin-simple-consumer] Completed asynchronous auto-commit of offsets {test-kotlin-topic-1=OffsetAndMetadata{offset=323, leaderEpoch=null, metadata=''}, test-kotlin-topic-0=OffsetAndMetadata{offset=305, leaderEpoch=null, metadata=''}, test-kotlin-topic-3=OffsetAndMetadata{offset=12486, leaderEpoch=10, metadata=''}, test-kotlin-topic-2=OffsetAndMetadata{offset=320, leaderEpoch=null, metadata=''}, test-kotlin-topic-5=OffsetAndMetadata{offset=289, leaderEpoch=null, metadata=''}, test-kotlin-topic-4=OffsetAndMetadata{offset=1324, leaderEpoch=null, metadata=''}, test-kotlin-topic-7=OffsetAndMetadata{offset=307, leaderEpoch=null, metadata=''}, test-kotlin-topic-6=OffsetAndMetadata{offset=323, leaderEpoch=null, metadata=''}}
Consumed record [Key: 10, Value: [10] Hello Kafka 166 => 2022-06-04T16:17:56.247076] @ Partition: 3, Offset:  12487
```

### Kafka Consumer with Manual Offset Control

Create a new Kotlin file `KafkaConsumerManual` in the package `com.trivadis.kafkaws.consumer` just created by copy-pasting from the class `KafkaConsumerAuto`.

Replace the implementation with the code below.

```kotlin
@file:JvmName("KafkaConsumerManual")
package com.trivadis.kafkaws.consumer

import org.apache.kafka.clients.consumer.ConsumerConfig.*
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.*


private val TOPIC = "test-kotlin-topic"
private val BOOTSTRAP_SERVERS = "dataplatform:9092,dataplatform:9093"

fun runConsumerManual(waitMsInBetween: Long) {
    // Define properties.
    val props = Properties()
    props[BOOTSTRAP_SERVERS_CONFIG] = BOOTSTRAP_SERVERS
    props[GROUP_ID_CONFIG] = "kotlin-simple-consumer"
    props[ENABLE_AUTO_COMMIT_CONFIG] = false
    props[KEY_DESERIALIZER_CLASS_CONFIG] = LongDeserializer::class.qualifiedName
    props[VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.qualifiedName

    val consumer = KafkaConsumer<Long, String>(props).apply {
        subscribe(listOf(TOPIC))
    }

    consumer.use {
        while(true) {
            val messages = consumer.poll(Duration.ofMillis(100))

            messages.forEach {
                println("Consumed record [Key: ${it.key()}, Value: ${it.value()}] @ Partition: ${it.partition()}, Offset:  ${it.offset()}")
            }

            consumer.commitSync();

            // Simulate slow processing
            Thread.sleep(waitMsInBetween);
        }
    }
}

fun main(args: Array<String>) {
    if (args.size == 0) {
        runConsumerManual(10)
    } else {
        runConsumerManual(args[0].toInt())
    }
}
```

Before we can run it, add the consumer to the `<executions>` section in the `pom.xml`.

```xml
					<execution>
						<id>consumer-manual</id>
						<goals>
							<goal>java</goal>
						</goals>
						<configuration>
							<mainClass>com.trivadis.kafkaws.consumer.KafkaConsumerManual</mainClass>
						</configuration>		
					</execution>
```

Now run it using the `mvn exec:java` command.

```bash
mvn clean package -Dmaven.test.skip=true

mvn exec:java@consumer-manual -Dexec.args="0"
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
