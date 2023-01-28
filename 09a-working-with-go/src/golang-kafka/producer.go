package main

import (
	"fmt"
	"strconv"

	"github.com/confluentinc/confluent-kafka-go/kafka"
	"os"
	"time"
)

func main() {
	if len(os.Args) < 3 {
		fmt.Fprintf(os.Stderr, "Usage: %s <send-message-count> <wait-ms-in-between> <id>\n",
			os.Args[0])
		os.Exit(1)
	}

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
				Headers:        []kafka.Header{{Key: "myTestHeader", Value: []byte("header value")}},
			}, nil)
		} else {
			err = p.Produce(&kafka.Message{
				TopicPartition: kafka.TopicPartition{Topic: &topic, Partition: kafka.PartitionAny},
				Key:            []byte(strconv.Itoa(id)),
				Value:          []byte(value),
				Headers:        []kafka.Header{{Key: "myTestHeader", Value: []byte("header value")}},
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
