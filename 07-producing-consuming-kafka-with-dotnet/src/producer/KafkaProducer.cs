using System.Threading;
using Confluent.Kafka;

class KafkaProducer
{
    const string brokerList = "dataplatform:9092,dataplatform:9093";
    const string topicName = "test-dotnet-topic";

    static void Main(string[] args)
    {
        long startTime = DateTimeOffset.Now.ToUnixTimeMilliseconds();
        if (args.Length == 0)
        {
            runProducerSync(100, 10, 0);
        }
        else
        {
            runProducerSync(int.Parse(args[0]), int.Parse(args[1]), int.Parse(args[2]));
        }
        long endTime = DateTimeOffset.Now.ToUnixTimeMilliseconds();

        Console.WriteLine("Producing all records took : " + (endTime - startTime) + " ms = (" + (endTime - startTime) / 1000 + " sec)");
    }
    static void runProducerSync(int totalMessages, int waitMsInBetween, int id)
    {
        var config = new ProducerConfig { BootstrapServers = brokerList };

        Func<Task> mthd = async () =>
        {

            // Create the Kafka Producer
            using (var producer = new ProducerBuilder<Null, string>(config).Build())
            {
                for (int index = 0; index < totalMessages; index++)
                {
                    // Construct the message value
                    string value = "[" + index + ":" + id + "] Hello Kafka " + DateTimeOffset.Now;

                    // send the message to Kafka
                    var deliveryReport = await producer.ProduceAsync(topicName, new Message<Null, string> { Value = value });
                    Console.WriteLine($"[{id}] sent record (key={deliveryReport.Key} value={deliveryReport.Value}) meta (partition={deliveryReport.TopicPartition.Partition}, offset={deliveryReport.TopicPartitionOffset.Offset}, time={deliveryReport.Timestamp.UnixTimestampMs})");

                    Thread.Sleep(waitMsInBetween);
                }
            }
        };

        mthd().Wait();
    }

    static void runProducerSyncWithKey(int totalMessages, int waitMsInBetween, int id)
    {
        var config = new ProducerConfig { BootstrapServers = brokerList };

        Func<Task> mthd = async () =>
        {

            // Create the Kafka Producer
            using (var producer = new ProducerBuilder<int, string>(config).Build())
            {
                for (int index = 0; index < totalMessages; index++)
                {
                    long time = DateTimeOffset.Now.ToUnixTimeMilliseconds();

                    // Construct the message value
                    string value = "[" + index + ":" + id + "] Hello Kafka " + DateTimeOffset.Now;

                    // send the message to Kafka
                    var deliveryReport = await producer.ProduceAsync(topicName, new Message<int, string> { Key = id, Value = value });
                    Console.WriteLine($"[{id}] sent record (key={deliveryReport.Key} value={deliveryReport.Value}) meta (partition={deliveryReport.TopicPartition.Partition}, offset={deliveryReport.TopicPartitionOffset.Offset}, time={deliveryReport.Timestamp.UnixTimestampMs})");

                    Thread.Sleep(waitMsInBetween);
                }
            }
        };

        mthd().Wait();
    }


    static void runProducerASync(int totalMessages, int waitMsInBetween, int id)
    {
        var config = new ProducerConfig { BootstrapServers = brokerList };

        // Create the Kafka Producer
        using (var producer = new ProducerBuilder<Null, string>(config).Build())
        {
            for (int index = 0; index < totalMessages; index++)
            {
                long time = DateTimeOffset.Now.ToUnixTimeMilliseconds();

                // Construct the message value
                string value = "[" + index + ":" + id + "] Hello Kafka " + DateTimeOffset.Now;

                // send the message to Kafka
                var deliveryReport = producer.ProduceAsync(topicName, new Message<Null, string> { Value = value });

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

    static void runProducerASync2(int totalMessages, int waitMsInBetween, int id)
    {
        var config = new ProducerConfig { BootstrapServers = brokerList };

        // Create the Kafka Producer
        using (var producer = new ProducerBuilder<Null, string>(config).Build())
        {
            for (int index = 0; index < totalMessages; index++)
            {
                long time = DateTimeOffset.Now.ToUnixTimeMilliseconds();

                // Construct the message value
                string value = "[" + index + ":" + id + "] Hello Kafka " + DateTimeOffset.Now;

                try
                {
                    // send the message to Kafka
                    producer.Produce(topicName, new Message<Null, string> { Value = value },
                        (deliveryReport) =>
                        {
                            if (deliveryReport.Error.Code != ErrorCode.NoError)
                            {
                                Console.WriteLine($"Failed to deliver message: {deliveryReport.Error.Reason}");
                            }
                            else
                            {
                                Console.WriteLine($"[{id}] sent record (key={deliveryReport.Key} value={deliveryReport.Value}) meta (partition={deliveryReport.TopicPartition.Partition}, offset={deliveryReport.TopicPartitionOffset.Offset}, time={deliveryReport.Timestamp.UnixTimestampMs})");
                            }
                        });

                }
                catch (ProduceException<Null, string> e)
                {
                    Console.WriteLine($"failed to deliver message: {e.Message} [{e.Error.Code}]");
                }

                Thread.Sleep(waitMsInBetween);
            }
            producer.Flush(TimeSpan.FromSeconds(10));

        }
    }
}