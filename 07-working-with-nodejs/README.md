# Using Kafka from Node.js

In this workshop we will learn how to use the [node-rdkafka](https://github.com/Blizzard/node-rdkafka/blob/master/examples/consumer-flow.md) and [kafka-avro](https://github.com/waldophotos/kafka-avro) libraries for working with Kafka and optionally with Avro (with support for the schema registry).

## Installing the Node.JS Client


### Install Node.JS

```
sudo apt install nodejs
```

### Install NPM

```
sudo apt install npm
```

### Install node-rdkafka

```
npm install node-rdkafka
```

### Creating the necessary Kafka Topic 
We will use the topic `test-node-topic` in the Producer and Consumer code below. Due to the fact that `auto.topic.create.enable` is set to `false`, we have to manually create the topic. 

Connect to the `broker-1` container

```
docker exec -ti broker-1 bash
```

and execute the necessary kafka-topics command. 

```
kafka-topics --create \
    --replication-factor 3 \
    --partitions 8 \
    --topic test-node-topic \
    --zookeeper zookeeper-1:2181
```

Cross check that the topic has been created.

```
kafka-topics --list \
    --zookeeper zookeeper-1:2181
```

This finishes the setup steps and our new project is ready to be used. Next we will start implementing the **Kafka Producer** which uses Avro for the serialization. 

# Kafka Producer

```
const Kafka = require('node-rdkafka');
console.log(Kafka.features);
console.log(Kafka.librdkafkaVersion);

var producer = new Kafka.Producer({
  'metadata.broker.list': 'dataplatform:9092, dataplatform:9093'
});

// Connect to the broker manually
producer.connect();

// Wait for the ready event before proceeding
producer.on('ready', function() {
  try {
    producer.produce(
      // Topic to send the message to
      'test-node-topic',
      // optionally we can manually specify a partition for the message
      // this defaults to -1 - which will use librdkafka's default partitioner (consistent random for keyed messages, random for unkeyed messages)
      null,
      // Message to send. Must be a buffer
      Buffer.from('this is the first message'),
      // for keyed messages, we also specify the key - note that this field is optional
      'Stormwind',
      // you can send a timestamp here. If your broker version supports it,
      // it will get added. Otherwise, we default to 0
      Date.now(),
      // you can send an opaque token here, which gets passed along
      // to your delivery reports
    );
  } catch (err) {
    console.error('A problem occurred when sending our message');
    console.error(err);
  }
});

// Any errors we encounter, including connection errors
producer.on('event.error', function(err) {
  console.error('Error from producer');
  console.error(err);
})

```

# Kafka Consumer

```
var Kafka = require('node-rdkafka');

var consumer = new Kafka.KafkaConsumer({
  'group.id': 'kafka',
  'metadata.broker.list': 'dataplatform:9092',
  'enable.auto.commit': false
}, {});

// Flowing mode
consumer.connect();

consumer
  .on('ready', function() {
    consumer.subscribe(['test-node-topic']);

    // Consume from the test-node-topic topic. This is what determines
    // the mode we are running in. By not specifying a callback (or specifying
    // only a callback) we get messages as soon as they are available.
    consumer.consume();
  })
  .on('data', function(data) {
    // Output the actual message contents
    console.log(data);
    console.log(data.value.toString());
    
    // commit each message (if a high volume of messages needs to be processed
    // then commiting each single message is not a good idea). Commit in blocks instead.
    consumer.commit();
  });

consumer
  .on('disconnected', function(arg) {
    logger.info('consumer disconnected. ' + JSON.stringify(arg));
  });
```
