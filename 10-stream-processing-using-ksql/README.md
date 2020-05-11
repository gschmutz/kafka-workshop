# Stream Processing using ksqlDB

With the truck data continuously ingested into the `truck_movement` topic, let's perform some stream processing on the information. There are many possible solutions for performing analytics directly on the event stream. In the Kafka project, we can either use [Kafka Streams](https://kafka.apache.org/documentation/streams/) or [ksqlDB](http://ksqldb.io), a SQL abstraction on top of Kafka Streams. 

For this workshop we will be using ksqlDB. 

![Alt Image Text](./images/stream-processing-with-ksql-overview.png "Schema Registry UI")

## Connect to ksqlDB Engine

In order to use KSQL, we need to connect to the KSQL engine using the KSQL CLI. An instance of a KSQL server has been started with our Streaming Platform and can be reached on port 8088.

```
docker exec -it ksqldb-cli ksql http://ksqldb-server-1:8088
```

You should see the KSQL command prompt:s

```                  
                  ===========================================
                  =       _              _ ____  ____       =
                  =      | | _____  __ _| |  _ \| __ )      =
                  =      | |/ / __|/ _` | | | | |  _ \      =
                  =      |   <\__ \ (_| | | |_| | |_) |     =
                  =      |_|\_\___/\__, |_|____/|____/      =
                  =                   |_|                   =
                  =  Event Streaming Database purpose-built =
                  =        for stream processing apps       =
                  ===========================================

Copyright 2017-2019 Confluent Inc.

CLI v0.6.0, Server v0.6.0 located at http://ksqldb-server-1:8088

Having trouble? Type 'help' (case-insensitive) for a rundown of how things work!

ksql>
```

We can use the show command to show topics as well as streams and tables. We have not yet created streams and tables, therefore we won't see anything. 

```
show topics;
show streams;
show tables;
```

## Working with KSQL in an ad-hoc fashion

### Give the truck_position topic a structure

Before we can use a KSQL SELECT statement, we have to describe the structure of our event in the `truck_position` topic. 

```
DROP STREAM truck_position_s;

CREATE STREAM truck_position_s 
  (ts VARCHAR, 
   truckId VARCHAR, 
   driverId BIGINT, 
   routeId BIGINT,
   eventType VARCHAR,
   latitude DOUBLE,
   longitude DOUBLE,
   correlationId VARCHAR)
  WITH (kafka_topic='truck_position',
        value_format='DELIMITED');
```

Now with the `truck_position_s` in place, let's use the `SELECT` statement to query live messages arriving on the stream. 

### Using KSQL to find abnormal driver behaviour

First let's find out abnormal driver behaviour by selecting all the events where the event type is not `Normal`.
        
```
SELECT * FROM truck_position_s
EMIT CHANGES;
```

This is not really so much different to using the `kafka-console-consumer` or `kafkacat`. But of course with ksqlDB we can do much more. You have the power of an SQL-like language at hand. 

So let's use a WHERE clause to only view the events where the event type is not `Normal`. 

```
SELECT * FROM truck_position_s 
WHERE eventType != 'Normal'
EMIT CHANGES;
```

It  will take much longer until we see a result, so be patient. At the end we only have a few events which are problematic!

This is interesting, but just seeing it in the KSQL terminal is of limited value. We would like to have that information available as a new Kafka topic. 

### Make that information available 

In order to publish the information to a new Kafka topic, we first have to create the Kafka topic. 

As learnt before, connect to one of the broker container instances

```
docker exec -ti docker_broker-1_1 bash
```

And perform the following `kafka-topics` command creating a new `dangerous_driving_ksql` topic in Kafka.

```
kafka-topics --zookeeper zookeeper:2181 --create --topic dangerous_driving_ksql --partitions 8 --replication-factor 2
```

Now let's publish to that topic from ksqlDB. For that we can create a new Stream. Instead of creating it on an existing topic as we have done before, we use the `CREATE STREAM ... AS SELECT ...` variant. 

```
DROP STREAM dangerous_driving_s;

CREATE STREAM dangerous_driving_s \
  WITH (kafka_topic='dangerous_driving_ksql', \
        value_format='DELIMITED', \
        partitions=8) \
AS 
SELECT * 
FROM truck_position_s \
WHERE eventtype != 'Normal';
```

The `SELECT` statement inside is basically the statement we have tested before and we know it will create the right information. 

We can use a `DESCRIBE` command to see metadata of any stream: 

```
DESCRIBE dangerous_driving_s;        
```

We can use this new stream for further processing, just as we have used the `truck_position_s` stream. 

```
SELECT * FROM dangerous_driving_s
EMIT CHANGES;
```

Now let's see that we actually produce data on that new topic by running a `kafka-console-consumer` or alternatively a `kafkacat`.

```
docker exec -ti docker_broker-1_1 bash
```

```
kafka-console-consumer --bootstrap-server kafka-1:19092 \
     --topic dangerous_driving_ksql
```

You should see the abnormal driving behaviour as before in the KSQL shell.        

### How many abnormal events do we get per 20 seconds

```
SELECT eventType, count(*) 
FROM dangerous_driving_s 
WINDOW TUMBLING (size 20 seconds) 
GROUP BY eventType
EMIT CHANGES;
```
