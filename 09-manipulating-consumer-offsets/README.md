# Manipulating Consumer Offsets

In this workshop we will learn how to manipulate the consumer offsets to reset a consumer to a certain place. 


We will be using the `kafka-consumer-groups` CLI.

## Resetting offsets

Let's first remove and recreate the topic, so the offsets will again start at 0. 

```bash
docker exec -ti kafka-1 kafka-topics --delete --topic test-java-topic --bootstrap-server kafka-1:19092,kafka-2:19093

docker exec kafka-1 kafka-topics --create \
    --replication-factor 3 \
    --partitions 8 \
    --topic test-java-topic \
    --bootstrap-server kafka-1:19092,kafka-2:19093
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
