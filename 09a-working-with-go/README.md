# Using Kafka from Go

In this workshop we will learn how to use the [confluent-kafka-go](https://github.com/confluentinc/confluent-kafka-go) to produce and consume to/from Kafka from the Go language.

You can find many examples of using Kafka with Go [here](https://github.com/confluentinc/confluent-kafka-go/tree/master/examples) in the GitHub project.

This guide assumes that you already have the Go language tools installed.

## Setup

### Creating the necessary Kafka Topic 

We will use the topic `test-node-topic` in the Producer and Consumer code below. Due to the fact that `auto.topic.create.enable` is set to `false`, we have to manually create the topic. 

Connect to the `kafka-1` container

```
docker exec -ti kafka-1 bash
```

and execute the necessary kafka-topics command. 

```
kafka-topics --create \
    --replication-factor 3 \
    --partitions 8 \
    --topic test-golang-topic \
    --bootstrap-server kafka-1:19092
```

Cross check that the topic has been created.

```
kafka-topics --list \
    --bootstrap-server kafka-1:19092
```

This finishes the setup steps and our new project is ready to be used. Next we will start implementing the **Kafka Producer** which uses Avro for the serialisation. 


## Create a Go Project

Create a new directory anywhere you’d like for this project:

```bash
mkdir golang-kafka && cd golang-kafka
```

Initialize the Go module and download the Confluent Go Kafka dependency:

```
go mod init golang-kafka 
go get github.com/confluentinc/confluent-kafka-go/kafka
```

# Create Kafka Producer

Next we are going to create the producer application by pasting the following Go code into a file named `producer.go`.

```go
package main

import (
	"fmt"
	"strconv"

	"github.com/confluentinc/confluent-kafka-go/kafka"
	"os"
	"time"
)

func main() {

	sendMessageCount, err := strconv.Atoi(os.Args[1])
	waitMsInBetween, err := strconv.Atoi(os.Args[2])
	id, err := strconv.Atoi(os.Args[3])

	p, err := kafka.NewProducer(&kafka.ConfigMap{"bootstrap.servers": "dataplatform:9092",
		"acks": "all"})

	if err != nil {
		fmt.Printf("Failed to create producer: %s\n", err)
		os.Exit(1)
	}

	fmt.Printf("Created Producer %v\n", p)

	// Listen to all the events on the default events channel
	go func() {
		for e := range p.Events() {
			switch ev := e.(type) {
			case *kafka.Message:
				// The message delivery report, indicating success or
				// permanent failure after retries have been exhausted.
				// Application level retries won't help since the client
				// is already configured to do that.
				m := ev
				if m.TopicPartition.Error != nil {
					fmt.Printf("Delivery failed: %v\n", m.TopicPartition.Error)
				} else {
					fmt.Printf("Delivered message to topic %s [%d] at offset %v\n",
						*m.TopicPartition.Topic, m.TopicPartition.Partition, m.TopicPartition.Offset)
				}
			case kafka.Error:
				// Generic client instance-level errors, such as
				// broker connection failures, authentication issues, etc.
				//
				// These errors should generally be considered informational
				// as the underlying client will automatically try to
				// recover from any errors encountered, the application
				// does not need to take action on them.
				fmt.Printf("Error: %v\n", ev)
			default:
				fmt.Printf("Ignored event: %s\n", ev)
			}
		}
	}()

	topic := "test-golang-topic"

	for i := 1; i <= sendMessageCount; i++ {
		value := fmt.Sprintf("[%d] Hello Kafka #%d => %s", i, i, time.Now().Format(time.RFC3339))

		if id == 0 {
			err = p.Produce(&kafka.Message{
				TopicPartition: kafka.TopicPartition{Topic: &topic, Partition: kafka.PartitionAny},
				Value:          []byte(value),
				Headers:        []kafka.Header{{Key: "myTestHeader", Value: []byte("header values are binary")}},
			}, nil)
		} else {
			err = p.Produce(&kafka.Message{
				TopicPartition: kafka.TopicPartition{Topic: &topic, Partition: kafka.PartitionAny},
				Key:            []byte(strconv.Itoa(id)),
				Value:          []byte(value),
				Headers:        []kafka.Header{{Key: "myTestHeader", Value: []byte("header values are binary")}},
			}, nil)
		}

		if err != nil {
			if err.(kafka.Error).Code() == kafka.ErrQueueFull {
				// Producer queue is full, wait 1s for messages
				// to be delivered then try again.
				time.Sleep(time.Second)
				continue
			}
			fmt.Printf("Failed to produce message: %v\n", err)
		}

		// Slow down processing
		time.Sleep(time.Duration(waitMsInBetween) * time.Millisecond)
	}

	// Flush and close the producer and the events channel
	for p.Flush(10000) > 0 {
		fmt.Print("Still waiting to flush outstanding messages\n", err)
	}
	p.Close()
}
```

# Kafka Consumer

Next let's create the consumer by pasting the following code into a file named `consumer.go`.

```go
package main

import (
	"fmt"
	"github.com/confluentinc/confluent-kafka-go/kafka"
	"os"
	"os/signal"
	"strconv"
	"syscall"
	"time"
)

func main() {
	if len(os.Args) < 1 {
		fmt.Fprintf(os.Stderr, "Usage: %s <wait-ms-in-between>\n",
			os.Args[0])
		os.Exit(1)
	}

	waitMsInBetween, err := strconv.Atoi(os.Args[1])

	sigchan := make(chan os.Signal, 1)
	signal.Notify(sigchan, syscall.SIGINT, syscall.SIGTERM)

	c, err := kafka.NewConsumer(&kafka.ConfigMap{
		"bootstrap.servers": "dataplatform:9092",
		// Avoid connecting to IPv6 brokers:
		// This is needed for the ErrAllBrokersDown show-case below
		// when using localhost brokers on OSX, since the OSX resolver
		// will return the IPv6 addresses first.
		// You typically don't need to specify this configuration property.
		"broker.address.family":    "v4",
		"group.id":                 "GoConsumer",
		"session.timeout.ms":       6000,
		"auto.offset.reset":        "earliest",
		"enable.auto.offset.store": false,
	})

	if err != nil {
		fmt.Fprintf(os.Stderr, "Failed to create consumer: %s\n", err)
		os.Exit(1)
	}

	fmt.Printf("Created Consumer %v\n", c)

	topics := []string{"test-golang-topic"}
	err = c.SubscribeTopics(topics, nil)

	run := true

	for run {
		select {
		case sig := <-sigchan:
			fmt.Printf("Caught signal %v: terminating\n", sig)
			run = false
		default:
			ev := c.Poll(100)
			if ev == nil {
				continue
			}

			switch e := ev.(type) {
			case *kafka.Message:
				fmt.Printf("%% Message on %s:\n%s\n",
					e.TopicPartition, string(e.Value))
				if e.Headers != nil {
					fmt.Printf("%% Headers: %v\n", e.Headers)
				}
				_, err := c.StoreMessage(e)
				if err != nil {
					fmt.Fprintf(os.Stderr, "%% Error storing offset after message %s:\n",
						e.TopicPartition)
				}
			case kafka.Error:
				// Errors should generally be considered
				// informational, the client will try to
				// automatically recover.
				// But in this example we choose to terminate
				// the application if all brokers are down.
				fmt.Fprintf(os.Stderr, "%% Error: %v: %v\n", e.Code(), e)
				if e.Code() == kafka.ErrAllBrokersDown {
					run = false
				}
			default:
				fmt.Printf("Ignored %v\n", e)
			}

			// Slow down processing
			time.Sleep(time.Duration(waitMsInBetween) * time.Millisecond)
		}
	}

	fmt.Printf("Closing consumer\n")
	c.Close()
}
```
