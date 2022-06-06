# Using ksqlDB for Stream Analytics

In this workshop we will learn how to process messages using the [ksqlDB](https://ksqldb.io/).

We will see different processing patterns applied.

## Connect to ksqlDB CLI

In order to use KSQL, we need to connect to the KSQL engine using the ksqlDB CLI. An instance of a ksqlDB server has been started with the Data Platform and can be reached on port `8088`.

```bash
docker exec -it ksqldb-cli ksql http://ksqldb-server-1:8088
```

You should see the KSQL command prompt:

```bash

OpenJDK 64-Bit Server VM warning: Option UseConcMarkSweepGC was deprecated in version 9.0 and will likely be removed in a future release.

                  ===========================================
                  =       _              _ ____  ____       =
                  =      | | _____  __ _| |  _ \| __ )      =
                  =      | |/ / __|/ _` | | | | |  _ \      =
                  =      |   <\__ \ (_| | | |_| | |_) |     =
                  =      |_|\_\___/\__, |_|____/|____/      =
                  =                   |_|                   =
                  =        The Database purpose-built       =
                  =        for stream processing apps       =
                  ===========================================

Copyright 2017-2022 Confluent Inc.

CLI v0.26.0, Server v0.26.0 located at http://ksqldb-server-1:8088
Server Status: RUNNING

Having trouble? Type 'help' (case-insensitive) for a rundown of how things work!

ksql>
```

We can use the show command to show topics as well as streams and tables. We have not yet created streams and tables, therefore we won't see anything.

```sql
show topics;
```

## Create a Stream 

Let's create a stream of `transaction`

```sql
CREATE STREAM transaction_s (
    email_address VARCHAR KEY,
    tx_id VARCHAR,
    card_number VARCHAR,
    timestamp TIMESTAMP,
    amount DECIMAL(12, 2)
) WITH (
    kafka_topic = 'transaction',
    partitions = 8,
    replicas = 3,
    value_format = 'avro'
);
```

Now let's view the data in the stream

```sql
SELECT * FROM transaction_s;
```

we have done it as a pull-query (which scans the whole topic behind the stream), and get the following result

```
+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+
|EMAIL_ADDRESS                                               |TX_ID                                                       |CARD_NUMBER                                                 |TIMESTAMP                                                   |AMOUNT                                                      |
+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+
Query Completed
Query terminated
```

You can see that the statement ended with no rows. There is no result, as we have not yet send any data to the topic. 

Let's change the statement to a push-query by adding `EMIT CHANGES` to the end.

```sql
SELECT * FROM transaction_s EMIT CHANGES;
```

Compared to before, the statement does not end and keeps on running. But there is still now data available. 

```
+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+
|EMAIL_ADDRESS                                               |TX_ID                                                       |CARD_NUMBER                                                 |TIMESTAMP                                                   |AMOUNT                                                      |
+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+

Press CTRL-C to interrupt
```

Let's change that and add some data to the stream. We could directly produce data to the underlying Kafka topic `transaction` or use the ksqlDB `INSERT` statement, which we will do here. In a new terminal, start again a ksqlDB CLI

```bash
docker exec -it ksqldb-cli ksql http://ksqldb-server-1:8088
```

and add a few transactions

```sql
INSERT INTO transaction_s (email_address, tx_id, card_number, timestamp, amount) 
VALUES ('peter.muster@acme.com', '0cf100ca-993c-427f-9ea5-e892ef350363', '352642227248344', FROM_UNIXTIME(UNIX_TIMESTAMP()), 100.00);

INSERT INTO transaction_s (email_address, tx_id, card_number, timestamp, amount) 
VALUES ('barbara.sample@acme.com', '9fe397e3-990e-449a-afcc-7b652a005c99', '999776673238348', FROM_UNIXTIME(UNIX_TIMESTAMP()), 220.00);
```

you will immediately see them returned as a result of the push-query

```sql
+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+
|EMAIL_ADDRESS                                               |TX_ID                                                       |CARD_NUMBER                                                 |TIMESTAMP                                                   |AMOUNT                                                      |
+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+
|peter.muster@acme.com                                       |0cf100ca-993c-427f-9ea5-e892ef350363                        |352642227248344                                             |2022-06-06T20:37:27.973                                     |100.00                                                      |
|barbara.sample@acme.com                                     |9fe397e3-990e-449a-afcc-7b652a005c99                        |999776673238348                                             |2022-06-06T20:38:38.343                                     |220.00                                                      |
```

Stop the push-query by entering `CTRL-C`. 

Let's do the pull-query again (the one without the `EMIT CHANGES` clause). 

```sql
SELECT * FROM transaction_s 
```

This time you can see the historical data which is stored by the stream (the underlying Kafka topic). **Note**: be careful with using a pull-query on a Stream, as it could be very resource intensive. 


## Filter on the stream

Let's say we are interested in only the transactions, where the amount is larger than `1000`.

This can easily be done using the `WHERE` clause of the push-query. 

```sql
SELECT * FROM transaction_s
WHERE amount > 1000.00
EMIT CHANGES;
```

Now let's add some more transactions, some with smaller values some with larger values:

```sql
INSERT INTO transaction_s (email_address, tx_id, card_number, timestamp, amount) 
VALUES ('bill.murphy@acme.com', '4c59430a-65a8-4607-bd90-a165a4a9488f', '8878779987897979', FROM_UNIXTIME(UNIX_TIMESTAMP()), 400.20);

INSERT INTO transaction_s (email_address, tx_id, card_number, timestamp, amount) 
VALUES ('peter.muster@acme.com', 'c204eb55-d572-4182-80ab-cfcc0f865861', '352642227248344', FROM_UNIXTIME(UNIX_TIMESTAMP()), 1040.20);

INSERT INTO transaction_s (email_address, tx_id, card_number, timestamp, amount) 
VALUES ('peter.muster@acme.com', 'c204eb55-d572-4182-80ab-cfcc0f865861', '352642227248344', FROM_UNIXTIME(UNIX_TIMESTAMP()), 300.90);

INSERT INTO transaction_s (email_address, tx_id, card_number, timestamp, amount) 
VALUES ('scott.james@acme.com', '6b69ce08-8506-42c8-900b-079b3f918d97', '9989898989098908', FROM_UNIXTIME(UNIX_TIMESTAMP()), 1090.00);
```

From the total of 4 new transactions, you should only see two in the result, the one which are above 1000.

```
+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+
|EMAIL_ADDRESS                                               |TX_ID                                                       |CARD_NUMBER                                                 |TIMESTAMP                                                   |AMOUNT                                                      |
+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+------------------------------------------------------------+
|peter.muster@acme.com                                       |c204eb55-d572-4182-80ab-cfcc0f865861                        |352642227248344                                             |2022-06-06T20:41:27.067                                     |1040.20                                                     |
|scott.james@acme.com                                        |6b69ce08-8506-42c8-900b-079b3f918d97                        |9989898989098908                                            |2022-06-06T20:42:13.069                                     |1090.00                                                     |
```

## Project columns on Stream

```ksql
SELECT email_address, amount
FROM transaction_s
EMIT CHANGES;
```

```ksql
INSERT INTO transaction_s (email_address, tx_id, card_number, timestamp, amount) 
VALUES ('tim.gallagher@acme.com', '8227233d-0b66-4e69-b49b-3cd08b21ab7d', '99989898909808', FROM_UNIXTIME(UNIX_TIMESTAMP()), 55.65);
```

```
+---------------------------------------------------------------------------------------------------------------------------------------------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------+
|EMAIL_ADDRESS                                                                                                                                            |AMOUNT                                                                                                                                                   |
+---------------------------------------------------------------------------------------------------------------------------------------------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------+
|tim.gallagher@acme.com                                                                                                                                   |55.65                                                                                                                                                    |
```

## Join Stream with a Table 

And a table of `customer` and put some data in it

```sql
DROP TABLE customer_t;
CREATE TABLE customer_t (email_address VARCHAR PRIMARY KEY, first_name VARCHAR, last_name VARCHAR, category VARCHAR)
	WITH (KAFKA_TOPIC='customer', 
			VALUE_FORMAT='AVRO', 
			PARTITIONS=8, 
			REPLICAS=3);
```

```sql
INSERT INTO customer_t (email_address, first_name, last_name, category) VALUES ('peter.muster@acme.com', 'Peter', 'Muster', 'A');
INSERT INTO customer_t (email_address, first_name, last_name, category) VALUES ('barbara.sample@acme.com', 'Barbara', 'Sample', 'B');
```

```sql
SELECT * FROM transaction_s AS tra
LEFT JOIN customer_t AS cus
ON (tra.email_address = cus.email_address)
EMIT CHANGES;
```

```sql  
INSERT INTO transaction_s (email_address, tx_id, card_number, timestamp, amount) 
VALUES ('peter.muster@acme.com', '05ed7709-8a18-4e0d-9608-27894750bd43', '352642227248344', FROM_UNIXTIME(UNIX_TIMESTAMP()), 99.95);
```

## Counting Values

```sql
SELECT email_address, COUNT(*)
FROM transaction_s
GROUP BY email_address
EMIT CHANGES;
```


## Counting Values over a Time Window



## Session Window

## Aggregating Values 

Now let's use Kafka Streams to perform antoher stateful operations. We will group the messages by key and aggregate (create a sum of the values) the values of the messages per key over 60 seconds.

