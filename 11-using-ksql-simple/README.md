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

## Project columns from Stream

Let's see how we can only show the columns we want to see

```ksql
SELECT email_address, amount
FROM transaction_s
EMIT CHANGES;
```

In the 2nd terminal, insert a new transaction

```ksql
INSERT INTO transaction_s (email_address, tx_id, card_number, timestamp, amount) 
VALUES ('tim.gallagher@acme.com', '8227233d-0b66-4e69-b49b-3cd08b21ab7d', '99989898909808', FROM_UNIXTIME(UNIX_TIMESTAMP()), 55.65);
```

And you should see

```
+---------------------------------------------------------------------------------------------------------------------------------------------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------+
|EMAIL_ADDRESS                                                                                                                                            |AMOUNT                                                                                                                                                   |
+---------------------------------------------------------------------------------------------------------------------------------------------------------+---------------------------------------------------------------------------------------------------------------------------------------------------------+
|tim.gallagher@acme.com                                                                                                                                   |55.65                                                                                                                                                    |
```

## Counting Values

Now let's see how we can do some stateful processing. Let's start with a simple count of values. 

In the first terminal perform

```sql
SELECT email_address, COUNT(*)
FROM transaction_s
GROUP BY email_address
EMIT CHANGES;
```

Now in the 2nd terminal add some new transactions

```sql  
INSERT INTO transaction_s (email_address, tx_id, card_number, timestamp, amount) 
VALUES ('peter.muster@acme.com', '05ed7709-8a18-4e0d-9608-27894750bd43', '352642227248344', FROM_UNIXTIME(UNIX_TIMESTAMP()), 99.95);
```

We can see that the count starts with 0 and will only increase from now. How can we make the result a bit more meaningful? By adding a time window to the query.

## Counting Values over a Time Window

Let's see the number of transactions within 30 seconds. We use a **tumbling*** window (Fixed-duration, non-overlapping, gap-less windows) first

```sql
SELECT FROM_UNIXTIME(WINDOWSTART)		as from_ts
, FROM_UNIXTIMESTAMP(WINDOWEND)		as to_ts
, email_address
, COUNT(*) as count
FROM transaction_s
WINDOW TUMBLING (SIZE 30 SECONDS)
GROUP BY email_address
EMIT CHANGES;
```

we can easily switch to a **hopping** window (Fixed-duration, overlapping windows) with 10 seconds overlap

```sql
SELECT FROM_UNIXTIME(WINDOWSTART)		as from_ts
, 	FROM_UNIXTIMESTAMP(WINDOWEND)		as to_ts
, email_address, COUNT(*) as count
FROM transaction_s
WINDOW HOPPING (SIZE 30 SECONDS, ADVANCE BY 10 SECONDS)
GROUP BY email_address
EMIT CHANGES;
```

## Join Stream with a Table 

Now let's see how we can enrich the stream of data (transactions) with some static data (customer). We want to do a join between the stream and a table holding all the customer data.

Create the table `customer_t` and put some data in it

```sql
DROP TABLE customer_t;
CREATE TABLE customer_t (email_address VARCHAR PRIMARY KEY, first_name VARCHAR, last_name VARCHAR, category VARCHAR)
	WITH (KAFKA_TOPIC='customer', 
			VALUE_FORMAT='AVRO', 
			PARTITIONS=8, 
			REPLICAS=3);
```

The table is backed by the a topic called `customer`. This topic is automatically created using the settings you provide. If it should be a compacted log topic (which could make sense), you have to create it manually first, before creating the table. 

Now let's add some data to the KSQL table:

```sql
INSERT INTO customer_t (email_address, first_name, last_name, category) VALUES ('peter.muster@acme.com', 'Peter', 'Muster', 'A');
INSERT INTO customer_t (email_address, first_name, last_name, category) VALUES ('barbara.sample@acme.com', 'Barbara', 'Sample', 'B');
```

And join the table to the transaction stream. We create another push query, which will continuously run.

```sql
SELECT * FROM transaction_s AS tra
LEFT JOIN customer_t AS cus
ON (tra.email_address = cus.email_address)
EMIT CHANGES;
```

In the 2nd terminal, insert a new message into the `transaction_s` stream

```sql  
INSERT INTO transaction_s (email_address, tx_id, card_number, timestamp, amount) 
VALUES ('peter.muster@acme.com', '05ed7709-8a18-4e0d-9608-27894750bd43', '352642227248344', FROM_UNIXTIME(UNIX_TIMESTAMP()), 99.95);
```
and you should see the result of the join in the 1st terminal windw. 

If we are happy with the result, we can materialized it by a CSAS (CREATE STREAM AS SELECT) Statement. 

```sql
CREATE STREAM transaction_customer_s
WITH (
    KAFKA_TOPIC = 'transaction_customer',
    VALUE_FORMAT = 'AVRO',
    PARTITIONS = 8,
    REPLICAS = 3
) 
AS
SELECT * FROM transaction_s AS tra
LEFT JOIN customer_t AS cus
ON (tra.email_address = cus.email_address)
EMIT CHANGES;
```

If you describe the stream, then you will see that KSQL has derived the column names so that they are unique (using the aliases)

```sql
ksql> describe transaction_customer_s;

Name                 : TRANSACTION_CUSTOMER_S
 Field             | Type                   
--------------------------------------------
 TRA_EMAIL_ADDRESS | VARCHAR(STRING)  (key) 
 TRA_TX_ID         | VARCHAR(STRING)        
 TRA_CARD_NUMBER   | VARCHAR(STRING)        
 TRA_TIMESTAMP     | TIMESTAMP              
 TRA_AMOUNT        | DECIMAL(12, 2)         
 CUS_EMAIL_ADDRESS | VARCHAR(STRING)        
 CUS_FIRST_NAME    | VARCHAR(STRING)        
 CUS_LAST_NAME     | VARCHAR(STRING)        
 CUS_CATEGORY      | VARCHAR(STRING)        
--------------------------------------------
For runtime statistics and query details run: DESCRIBE <Stream,Table> EXTENDED;
```

You can of course also provide your own column aliases by listing the columns in the SELECT list, instead of just using the `*`.

Now you can get the result either by querying the new stream `transaction_customer_s` or by consuming the underlying topic called `transaction_customer`.

```sql
SELECT * 
FROM transaction_customer_s
EMIT CHANGES;
```

## Total amount of transaction value by category

```ksql
SELECT cus_category		category
, 	SUM(tra_amount) 		amount
FROM transaction_customer_s
WINDOW TUMBLING (SIZE 30 SECONDS)
GROUP BY cus_category
EMIT CHANGES;
```

To materialize the result, we can create a table


```sql
CREATE TABLE total_amount_per_category_t
AS 
SELECT cus_category		category
, 	SUM(tra_amount) 		amount
FROM transaction_customer_s
WINDOW TUMBLING (SIZE 30 SECONDS)
GROUP BY cus_category
EMIT CHANGES;
```

We haven't specified an underlying topic, so KSQL will create one with the same name. You can cross-check by doing a DESCRIBE ... EXTENDED

```sql
ksql> describe total_amount_per_category_t extended;

Name                 : TOTAL_AMOUNT_PER_CATEGORY_T
Type                 : TABLE
Timestamp field      : Not set - using <ROWTIME>
Key format           : KAFKA
Value format         : AVRO
Kafka topic          : TOTAL_AMOUNT_PER_CATEGORY_T (partitions: 8, replication: 3)
Statement            : CREATE TABLE TOTAL_AMOUNT_PER_CATEGORY_T WITH (KAFKA_TOPIC='TOTAL_AMOUNT_PER_CATEGORY_T', PARTITIONS=8, REPLICAS=3) AS SELECT
  TRANSACTION_CUSTOMER_S.CUS_CATEGORY CATEGORY,
  SUM(TRANSACTION_CUSTOMER_S.TRA_AMOUNT) AMOUNT
FROM TRANSACTION_CUSTOMER_S TRANSACTION_CUSTOMER_S
WINDOW TUMBLING ( SIZE 30 SECONDS )
GROUP BY TRANSACTION_CUSTOMER_S.CUS_CATEGORY
EMIT CHANGES;

 Field    | Type
-------------------------------------------------------------------
 CATEGORY | VARCHAR(STRING)  (primary key) (Window type: TUMBLING)
 AMOUNT   | DECIMAL(12, 2)
-------------------------------------------------------------------

Queries that write from this TABLE
-----------------------------------
CTAS_TOTAL_AMOUNT_PER_CATEGORY_T_45 (RUNNING) : CREATE TABLE TOTAL_AMOUNT_PER_CATEGORY_T WITH (KAFKA_TOPIC='TOTAL_AMOUNT_PER_CATEGORY_T', PARTITIONS=8, REPLICAS=3) AS SELECT   TRANSACTION_CUSTOMER_S.CUS_CATEGORY CATEGORY,   SUM(TRANSACTION_CUSTOMER_S.TRA_AMOUNT) AMOUNT FROM TRANSACTION_CUSTOMER_S TRANSACTION_CUSTOMER_S WINDOW TUMBLING ( SIZE 30 SECONDS )  GROUP BY TRANSACTION_CUSTOMER_S.CUS_CATEGORY EMIT CHANGES;

For query topology and execution plan please run: EXPLAIN <QueryId>

Local runtime statistics
------------------------


(Statistics of the local KSQL server interaction with the Kafka topic TOTAL_AMOUNT_PER_CATEGORY_T)

Consumer Groups summary:

Consumer Group       : _confluent-ksql-ksqldb-clusterquery_CTAS_TOTAL_AMOUNT_PER_CATEGORY_T_45
<no offsets committed by this group yet>
```

Push-query on table to get the changes

```sql
SELECT * FROM TOTAL_AMOUNT_PER_CATEGORY_T EMIT CHANGES;
```

Pull-query to get the current state for a category

```sql
SELECT * FROM TOTAL_AMOUNT_PER_CATEGORY_T  WHERE category = 'A';
```

## Anomalies (does not yet work!)

```sql
CREATE TABLE possible_anomalies WITH (
    kafka_topic = 'possible_anomalies',
    VALUE_AVRO_SCHEMA_FULL_NAME = 'io.ksqldb.tutorial.PossibleAnomaly'
)   AS
    SELECT card_number AS `card_number_key`,
           as_value(card_number) AS `card_number`,
           latest_by_offset(email_address) AS `email_address`,
           count(*) AS `n_attempts`,
           sum(amount) AS `total_amount`,
           collect_list(tx_id) AS `tx_ids`,
           WINDOWSTART as `start_boundary`,
           WINDOWEND as `end_boundary`
    FROM transactions
    WINDOW TUMBLING (SIZE 30 SECONDS, RETENTION 1000 DAYS)
    GROUP BY card_number
    HAVING count(*) >= 3
    EMIT CHANGES;
```sql


