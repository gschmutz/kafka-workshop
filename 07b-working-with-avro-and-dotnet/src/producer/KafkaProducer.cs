using System.Threading;
using System;
using Confluent.Kafka;
using Confluent.SchemaRegistry;
using Confluent.SchemaRegistry.Serdes;
using com.trivadis.kafkaws.avro.v1;


class KafkaProducer
{
    const string brokerList = "dataplatform:9092,dataplatform:9093";
    const string topicName = "test-dotnet-avro-topic";

    const string schemaRegistryUrl = "http://dataplatform:8081";

    static void Main(string[] args)
    {
        long startTime = DateTimeOffset.Now.ToUnixTimeMilliseconds();
        if (args.Length == 0)
        {
            runProducerASync(100, 10, 0);
        }
        else
        {
            runProducerASync(int.Parse(args[0]), int.Parse(args[1]), int.Parse(args[2]));
        }
        long endTime = DateTimeOffset.Now.ToUnixTimeMilliseconds();

        Console.WriteLine("Producing all records took : " + (endTime - startTime) + " ms = (" + (endTime - startTime) / 1000 + " sec)" );
    }
    static void runProducerASync(int totalMessages, int waitMsInBetween, int id)
    {
        var config = new ProducerConfig { BootstrapServers = brokerList };

        var schemaRegistryConfig = new SchemaRegistryConfig
            {
                // Note: you can specify more than one schema registry url using the
                // schema.registry.url property for redundancy (comma separated list). 
                // The property name is not plural to follow the convention set by
                // the Java implementation.
                Url = schemaRegistryUrl
            };
        var avroSerializerConfig = new AvroSerializerConfig
            {
                // optional Avro serializer properties:
                BufferBytes = 100
            };    

        using (var schemaRegistry = new CachedSchemaRegistryClient(schemaRegistryConfig))
        // Create the Kafka Producer
        using (var producer = new ProducerBuilder<Null, Notification>(config)
                    .SetValueSerializer(new AvroSerializer<Notification>(schemaRegistry, avroSerializerConfig))
                    .Build())
        {
            for (int index = 0; index < totalMessages; index++)
            {
                long time = DateTimeOffset.Now.ToUnixTimeMilliseconds();

                // Construct the message value
                Notification value = new Notification { id = id, message = "[" + index + ":" + id + "] Hello Kafka " + DateTimeOffset.Now };

                // send the message to Kafka
                var deliveryReport = producer.ProduceAsync(topicName, new Message<Null, Notification> { Value = value });

                deliveryReport.ContinueWith(task =>
                {
                    if (task.IsFaulted)
                    {
                    }
                    else
                    {
                        Console.WriteLine($"[{id}] sent record (key={task.Result.Key} value={task.Result.Value}) meta (partition={task.Result.TopicPartition.Partition}, offset={task.Result.TopicPartitionOffset.Offset}, time={task.Result.Timestamp.UnixTimestampMs})");
                    }

                });
                Thread.Sleep(waitMsInBetween);
            }
            producer.Flush(TimeSpan.FromSeconds(10));
        }
    }
}