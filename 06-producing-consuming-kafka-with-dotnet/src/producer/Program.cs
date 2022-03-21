using System;
using System.Threading;
using Confluent.Kafka;

namespace producer
{
    class Program
    {
        const string brokerList = "dataplatform:9092,dataplatform:9093";
        const string topicName = "test-dotnet-topic";

        static void Main(string[] args)
        {
            if (args.Length == 0) {
                runProducerWithKey(100, 10, 0);
            } else {
                runProducerWithKey(int.Parse(args[0]), int.Parse(args[1]), int.Parse(args[2]));
            }
        }

        static void runProducer(int totalMessages, int waitMsInBetween, int id)
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
                            var task =  producer.ProduceAsync(topicName, new Message<Null, string> { Value = value});    
                            if (task.IsFaulted) {
                                
                            } else {
                                long elapsedTime = DateTimeOffset.Now.ToUnixTimeMilliseconds() - time;

                                Console.WriteLine($"[{id}] sent record (key={task.Result.Key} value={task.Result.Value}) meta (partition={task.Result.TopicPartition.Partition}, offset={task.Result.TopicPartitionOffset.Offset}, time={elapsedTime})");
                            }
                        }
                        catch (ProduceException<string, string> e)
                        {
                            Console.WriteLine($"failed to deliver message: {e.Message} [{e.Error.Code}]");
                        }

                        Thread.Sleep(waitMsInBetween);
                }
           
            }  
        }

        static void runProducerWithKey(int totalMessages, int waitMsInBetween, int id)
        {
            var config = new ProducerConfig { BootstrapServers = brokerList };

            // Create the Kafka Producer
            using (var producer = new ProducerBuilder<long, string>(config).Build())
            {
                for (int index = 0; index < totalMessages; index++)
                {
                    long time = DateTimeOffset.Now.ToUnixTimeMilliseconds();

                    // Construct the message value
                    string value = "[" + index + ":" + id + "] Hello Kafka " + DateTimeOffset.Now;
                    
                    try
                        {
                            // send the message to Kafka
                            var task =  producer.ProduceAsync(topicName, new Message<long, string> { Key = id, Value = value});    
                            if (task.IsFaulted) {
                                
                            } else {
                                long elapsedTime = DateTimeOffset.Now.ToUnixTimeMilliseconds() - time;

                                Console.WriteLine($"[{id}] sent record (key={task.Result.Key} value={task.Result.Value}) meta (partition={task.Result.TopicPartition.Partition}, offset={task.Result.TopicPartitionOffset.Offset}, time={elapsedTime})");
                            }
                        }
                        catch (ProduceException<string, string> e)
                        {
                            Console.WriteLine($"failed to deliver message: {e.Message} [{e.Error.Code}]");
                        }

                        Thread.Sleep(waitMsInBetween);
                }
           
            }  
        }

    }
}
