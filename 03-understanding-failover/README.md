# Understanding Kafka Failover
In this tutorial, we are going to run many Kafka Nodes on our development laptop so that you will need at least 16 GB of RAM for local dev machine. You can run just two servers if you have less memory than 16 GB. We are going to create a replicated topic. We then demonstrate consumer failover and broker failover. We also demonstrate load balancing Kafka consumers. We show how, with many groups, Kafka acts like a Publish/Subscribe. But, when we put all of our consumers in the same group, Kafka will load share the messages to the consumers in the same group (more like a queue than a topic in a traditional MOM sense).

If not already running, then start up ZooKeeper (./run-zookeeper.sh from the first tutorial). Also, shut down Kafka from the first tutorial.

Next, you need to copy server properties for three brokers (detailed instructions to follow). Then we will modify these Kafka server properties to add unique Kafka ports, Kafka log locations, and unique Broker ids. Then we will create three scripts to start these servers up using these properties, and then start the servers. Lastly, we create replicated topic and use it to demonstrate Kafka consumer failover, and Kafka broker failover.
    
### Create Kafka replicated topic my-failsafe-topic 

```
docker exec -ti streamingplatform_broker-1_1 bash
```

```
kafka-topics --create \
    --zookeeper zookeeper:2181 \
    --replication-factor 3 \
    --partitions 8 \
    --topic failsafe-test-topic
```

Notice that the replication factor gets set to 3, and the topic name is `failsafe-test-topic`.

## Start Kafka Consumer that uses Replicated Topic

Start a consumer using `kafkacat` on the topic `failsafe-test-topic`.

```
cas@cas ~> kafkacat -b localhost -t failsafe-test-topic -o end
```

## Start Kafka Producer that uses Replicated Topic

Start the producer using `Kafkacat` and produce 3 messages

```
cas@cas ~> kafkacat -P -b localhost -t failsafe-test-topic
Hello World!
Hello Events!
Hello Kafka!
```

in the consumer console started above you should see the 3 messages

```
cas@cas ~> kafkacat -b localhost -t failsafe-test-topic -o end
Hello World!
Hello Events!
Hello Kafka!
```

## Now Start two more consumers and send more messages

Start two more consumers in their own terminal window and send more messages from the producer.

```
cas@cas ~> kafkacat -P -b localhost -t failsafe-test-topic
Hello World!
Hello Events!
Hello Kafka!
message new 1
message new 2
message new 3
```

#### Consumer Console 1st
```
cas@cas ~> kafkacat -b localhost -t failsafe-test-topic -o end
Hello World!
Hello Events!
Hello Kafka!
message new 1
message new 2
message new 3
```

#### Consumer Console 2nd
```
cas@cas ~> kafkacat -b localhost -t failsafe-test-topic -o end
message new 1
message new 2
message new 3
```

#### Consumer Console 3rd
```
cas@cas ~> kafkacat -b localhost -t failsafe-test-topic -o end
message new 1
message new 2
message new 3
```

Notice that the messages are sent to all of the consumers because each consumer is in a different consumer group.

## Change consumer to be in their own consumer group

Stop the producers and the consumers from before.

Now let’s start the console consumers to use the same consumer group. This way the consumers will share the messages as each consumer in the consumer group will get its share of partitions.

```
kafkacat -b localhost -t failsafe-test-topic -G test-group failsafe-test-topic 
```

### Now send 3 messages from the Kafka producer console.

#### Producer Console

```
cas@cas ~> kafkacat -P -b localhost -t failsafe-test-topic
message new 1
message new 2
message new 3
message new 4
message new 5
```

#### Consumer Console 1st
```
cas@cas ~> kafkacat -b localhost -t failsafe-test-topic -o end
message 2
```

#### Consumer Console 2nd
```
cas@cas ~> kafkacat -b localhost -t failsafe-test-topic -o end
message 1
message 3
```

#### Consumer Console 3rd
```
cas@cas ~> kafkacat -b localhost -t failsafe-test-topic -o end
message 4
message 5
```

Notice that the messages are spread evenly among the consumers.

## Kafka Consumer Failover

Next, let’s demonstrate consumer failover by killing one of the consumers and sending seven more messages. Kafka should divide up the work to the consumers that are running.

First, kill the third consumer (CTRL-C in the consumer terminal does the trick).

Now send seven more messages with the Kafka console-producer.

### Describe Topic

```
kafka-topics --describe \
    --topic failsafe-test-topic \
    --zookeeper zookeeper:2181
```    