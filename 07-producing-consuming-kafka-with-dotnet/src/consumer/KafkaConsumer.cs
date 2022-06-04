using System.Threading;
using Confluent.Kafka;

class KafkaConsumer
{
    const string brokerList = "dataplatform:9092,dataplatform:9093";
    const string topicName = "test-dotnet-topic";
    const string groupId = "KafkaConsumerAuto";

    static void Main(string[] args)
    {
        if (args.Length == 0)
        {
            runConsumerAuto(10);
        }
        else
        {
            runConsumerAuto(int.Parse(args[0]));
        }
    }

    static void runConsumerAuto(int waitMsInBetween)
    {
        var config = new ConsumerConfig
        {
            BootstrapServers = brokerList,
            GroupId = groupId,
            AutoOffsetReset = AutoOffsetReset.Earliest,
            EnableAutoCommit = true,
            AutoCommitIntervalMs = 5000
        };

        bool cancelled = false;

        using (var consumer = new ConsumerBuilder<Ignore, string>(config).Build())
        {
            consumer.Subscribe(topicName);
            var cancelToken = new CancellationTokenSource();

            while (!cancelled)
            {
                var consumeResult = consumer.Consume(cancelToken.Token);
                // handle message
                Console.WriteLine($"Consumer Record:(Key: {consumeResult.Message.Key}, Value: {consumeResult.Message.Value} Partition: {consumeResult.TopicPartition.Partition} Offset: {consumeResult.TopicPartitionOffset.Offset}");
                Thread.Sleep(waitMsInBetween);
            }
            consumer.Close();
        }
    }

    static void runConsumerManual(int waitMsInBetween)
    {
        var config = new ConsumerConfig
        {
            BootstrapServers = brokerList,
            GroupId = groupId,
            EnableAutoCommit = false
        };

        bool cancelled = false;
        int noRecordsCount = 0;

        using (var consumer = new ConsumerBuilder<Ignore, string>(config).Build())
        {
            consumer.Subscribe(topicName);
            var cancelToken = new CancellationTokenSource();
            ConsumeResult<Ignore, string> consumeResult = null;

            while (!cancelled)
            {
                consumeResult = consumer.Consume(cancelToken.Token);
                noRecordsCount++;

                // handle message
                Console.WriteLine($"Consumer Record:(Key: {consumeResult.Message.Key}, Value: {consumeResult.Message.Value} Partition: {consumeResult.TopicPartition.Partition} Offset: {consumeResult.TopicPartitionOffset.Offset}");
                Thread.Sleep(waitMsInBetween);

                if (consumeResult.Offset % 50 == 0)
                {
                    consumer.Commit(consumeResult);
                }
            }

            // commit the rest
            consumer.Commit(consumeResult);

            consumer.Close();
        }
    }
}

