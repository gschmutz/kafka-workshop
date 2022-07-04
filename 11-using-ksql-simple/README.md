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
show streams;
show tables;
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

This time you can see the historical data which is stored by the stream (the underlying Kafka topic). 

```sql
ksql> SELECT * FROM transaction_s;
+-----------------------------+-----------------------------+-----------------------------+-----------------------------+-----------------------------+
|EMAIL_ADDRESS                |TX_ID                        |CARD_NUMBER                  |TIMESTAMP                    |AMOUNT                       |
+-----------------------------+-----------------------------+-----------------------------+-----------------------------+-----------------------------+
|barbara.sample@acme.com      |9fe397e3-990e-449a-afcc-7b652|999776673238348              |2022-07-03T19:05:37.451      |220.00                       |
|                             |a005c99                      |                             |                             |                             |
|peter.muster@acme.com        |0cf100ca-993c-427f-9ea5-e892e|352642227248344              |2022-07-03T19:05:37.406      |100.00                       |
|                             |f350363                      |                             |                             |                             |
Query Completed
Query terminated
ksql>
```

**Note**: be careful with using a pull-query on a Stream, as it could be very resource intensive. 


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

## Using Functions

ksqlDB provides with a large set of [useful functions](https://docs.ksqldb.io/en/latest/developer-guide/ksqldb-reference/functions/). 

Let's say that we want to mask the credit card number, then we can use either `MASK` to fully mask or `MASK_RIGHT`/`MASK_LEFT` or `MASK_KEEP_LEFT`/`MASK_KEEP_RIGHT` to partially mask a string field.

```ksql
SELECT email_address, MASK(card_number) card_number, MASK_KEEP_RIGHT(card_number, 4, 'X','x','n','-') card_number_masked_keep4, tx_id, amount
FROM transaction_s
EMIT CHANGES;
```

You can also implement your own [User-Defined Functions](https://docs.ksqldb.io/en/latest/reference/user-defined-functions/), by writing Java hooks.

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

We can see that the count starts with 0 and will only increase from now. 

How can we make the result a bit more meaningful? By adding a time window to the query!

## Counting Values over a Time Window

### Tumbling Window

Let's see the number of transactions within 30 seconds. We use a **tumbling*** window (Fixed-duration, non-overlapping, gap-less windows) first

```sql
SELECT FROM_UNIXTIME (windowstart)		as from_ts
, 		FROM_UNIXTIME (windowend)			as to_ts
,	 	email_address
, 		COUNT(*) as count
FROM transaction_s
WINDOW TUMBLING (SIZE 30 SECONDS)
GROUP BY email_address
EMIT CHANGES;
```

Now in a second window, `INSERT` some more messages. 

```sql
+-----------------------------------------------------------------------+-----------------------------------------------------------------------+-----------------------------------------------------------------------+-----------------------------------------------------------------------+
|FROM_TS                                                                |TO_TS                                                                  |EMAIL_ADDRESS                                                          |COUNT                                                                  |
+-----------------------------------------------------------------------+-----------------------------------------------------------------------+-----------------------------------------------------------------------+-----------------------------------------------------------------------+
|2022-07-04T13:36:00.000                                                |2022-07-04T13:36:30.000                                                |peter.muster@acme.com                                                  |1                                                                      |
|2022-07-04T13:36:00.000                                                |2022-07-04T13:36:30.000                                                |barbara.sample@acme.com                                                |1                                                                      |
|2022-07-04T13:36:00.000                                                |2022-07-04T13:36:30.000                                                |peter.muster@acme.com                                                  |2                                                                      |
|2022-07-04T13:36:00.000                                                |2022-07-04T13:36:30.000                                                |barbara.sample@acme.com                                                |2                                                                      |
```

You can see that a result is displayed before the time window is finished. The EMIT clause lets you control the output refinement of your push query. You can switch from `CHANGES` to `FINAL` to tell ksqlDB to wait for the time window to close before a result is returned.

**Note:** `EMIT FINAL` is data driven, i.e., it emit results only if "stream-time" advanced beyond the window close time. "Stream-time" depends on the observed timestamps of your input record, and thus, if you stop sending input records, "stream-time" does not advance further. If you stop sending data, the last window might never be closed, and thus you never see a result for it.

**Note:** There are some memory considerations with the `FINAL` feature that could cause unbounded growth and eventual an OOM exception. The feature is still in development/beta and we'll definitely document it when it's complete!

### Hopping Window

We can easily switch to a **hopping** window (Fixed-duration, overlapping windows) with 10 seconds overlap

```sql
SELECT FROM_UNIXTIME (WINDOWSTART)		as from_ts
, 		FROM_UNIXTIME (WINDOWEND)			as to_ts
, 		email_address
, 		COUNT(*) as count
FROM transaction_s
WINDOW HOPPING (SIZE 30 SECONDS, ADVANCE BY 10 SECONDS)
GROUP BY email_address
EMIT CHANGES;
```

## Create a ksqlDB Table holding Customers

A table is a mutable, partitioned collection that models change over time. In contrast with a stream, which represents a historical sequence of events, a table represents what is true as of "now". 

Tables work by leveraging the keys of each row. If a sequence of rows shares a key, the last row for a given key represents the most up-to-date information for that key's identity. A background process periodically runs and deletes all but the newest rows for each key.

Let's use a table to model customers. 

Create the table `customer_t` and put some data in it

```sql
DROP TABLE customer_t;
CREATE TABLE customer_t (email_address VARCHAR PRIMARY KEY, first_name VARCHAR, last_name VARCHAR, category VARCHAR)
	WITH (KAFKA_TOPIC='customer', 
			VALUE_FORMAT='AVRO', 
			PARTITIONS=8, 
			REPLICAS=3);
```

The table is backed by the topic called `customer`. This topic is automatically created using the settings you provide. If it should be a compacted log topic (which could make sense), you have to create it manually first, before creating the table. 

Now let's add some data to the KSQL table:

```sql
INSERT INTO customer_t (email_address, first_name, last_name, category) VALUES ('peter.muster@acme.com', 'Peter', 'Muster', 'A');
INSERT INTO customer_t (email_address, first_name, last_name, category) VALUES ('barbara.sample@acme.com', 'Barbara', 'Sample', 'B');
```

With that in place, can we use a Pull-Query to retrieve a customer? Let's try that

```sql
SELECT *
FROM customer_t
WHERE email_address = 'peter.muster@acme.com';
```

will give an error

```sql
ksql> SELECT *
>FROM customer_t
>WHERE email_address = 'peter.muster@acme.com';
The `CUSTOMER_T` table isn't queryable. To derive a queryable table, you can do 'CREATE TABLE QUERYABLE_CUSTOMER_T AS SELECT * FROM CUSTOMER_T'. See https://cnfl.io/queries for more info.
Add EMIT CHANGES if you intended to issue a push query.
```

You can provide the `SOURCE` clause to enable running pull queries on the table. The `SOURCE` clause runs an internal query for the table to create a materialized state that's used by pull queries. When you create a `SOURCE` table, the table is created as read-only. For a read-only table, INSERT, DELETE TOPIC, and DROP TABLE statements aren't permitted.

That's why we reuse the topic `customer` from the table created above, just to see a `SOURCE` table in action. In a real-life situation, usually the data behind the table is created directly either by a connector or a stream processing application. 

Let's create a new `SOURCE` table by adding the `SOURCE` keyword:

```sql
DROP TABLE customer_st;
CREATE SOURCE TABLE customer_st (email_address VARCHAR PRIMARY KEY, first_name VARCHAR, last_name VARCHAR, category VARCHAR)
	WITH (KAFKA_TOPIC='customer', 
			VALUE_FORMAT='AVRO', 
			PARTITIONS=8, 
			REPLICAS=3);
```

Now on that table a pull-query works:

```sql
SELECT *
FROM customer_st
WHERE email_address = 'peter.muster@acme.com';
```

Let's drop that table as we no longer need it

```sql
DROP TABLE customer_st;
```

## Join Stream with a Table 

Now let's see how we can enrich the stream of data (transactions) with some static data (customer). We want to do a join between the stream and a table (`customer_t`) holding all the customer data, we just created.

So let's join the table to the transaction stream. We create another push query, which will continuously run.

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
and you should see the result of the join in the 1st terminal window. 

```bash
ksql> SELECT * FROM transaction_s AS tra
>LEFT JOIN customer_t AS cus
>ON (tra.email_address = cus.email_address)
>EMIT CHANGES;
+---------------------+---------------------+---------------------+---------------------+---------------------+---------------------+---------------------+---------------------+---------------------+
|TRA_EMAIL_ADDRESS    |TRA_TX_ID            |TRA_CARD_NUMBER      |TRA_TIMESTAMP        |TRA_AMOUNT           |CUS_EMAIL_ADDRESS    |CUS_FIRST_NAME       |CUS_LAST_NAME        |CUS_CATEGORY         |
+---------------------+---------------------+---------------------+---------------------+---------------------+---------------------+---------------------+---------------------+---------------------+
|peter.muster@acme.com|0cf100ca-993c-427f-9e|352642227248344      |2022-07-04T15:44:14.8|100.00               |peter.muster@acme.com|Peter                |Muster               |A                    |
|                     |a5-e892ef350363      |                     |98                   |                     |                     |                     |                     |                     |
|barbara.sample@acme.c|9fe397e3-990e-449a-af|999776673238348      |2022-07-04T15:44:14.9|220.00               |barbara.sample@acme.c|Barbara              |Sample               |B                    |
|om                   |cc-7b652a005c99      |                     |76                   |                     |om                   |                     |                     |                     |

Press CTRL-C to interrupt
```

If we are happy with the result, we can materialise it by using a CSAS (CREATE STREAM AS SELECT) statement. 

```sql
CREATE STREAM transaction_customer_s
WITH (
    KAFKA_TOPIC = 'transaction_customer',
    VALUE_FORMAT = 'AVRO',
    PARTITIONS = 8,
    REPLICAS = 3
) 
AS
SELECT * 
FROM transaction_s AS tra
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

You can of course also provide your own column aliases by listing the columns in the `SELECT` list, instead of just using the `*`.

Now you can get the result either by querying the new stream `transaction_customer_s` 

```sql
SELECT * 
FROM transaction_customer_s
EMIT CHANGES;
```

or by consuming directly from the underlying topic called `transaction_customer`.

```bash
docker run -it --network=host edenhill/kcat:1.7.1 -b dataplatform -t transaction_customer -s value=avro -r http://dataplatform:8081
```

## Total amount of transaction value by category

Now with the enriched stream of transactions and customers, we can get the total amount of transactions by customer category over a tumbling window of 30 seconds.

```ksql
SELECT cus_category					AS category
, 	SUM(tra_amount) 					AS amount
,  FROM_UNIXTIME (WINDOWSTART)	AS from_ts
,  FROM_UNIXTIME (WINDOWEND)		AS to_ts
FROM transaction_customer_s
WINDOW TUMBLING (SIZE 30 SECONDS, RETENTION 120 SECONDS, GRACE PERIOD 60 SECONDS)
GROUP BY cus_category
EMIT CHANGES;
```

We create a **tumbling window** of 30 seconds size, with a **grace period** of 60 seconds (accepting out-of-order events which are older than 1 minute) and a **window retention** of 120 seconds (the number of windows in the past that ksqlDB retains, so that they are available for Pull-Queries).

**Note:** the specified retention period should be larger than the sum of window size and any grace period.


To materialise the result, we can create another table


```sql
CREATE TABLE total_amount_per_category_t
AS 
SELECT cus_category					AS category
, 	SUM(tra_amount) 					AS amount
,  FROM_UNIXTIME (WINDOWSTART)	AS from_ts
,  FROM_UNIXTIME (WINDOWEND)		AS to_ts
FROM transaction_customer_s
WINDOW TUMBLING (SIZE 30 SECONDS, RETENTION 120 SECONDS, GRACE PERIOD 60 SECONDS)
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
WINDOW TUMBLING ( SIZE 30 SECONDS , RETENTION 120 SECONDS , GRACE PERIOD 60 SECONDS )
GROUP BY TRANSACTION_CUSTOMER_S.CUS_CATEGORY
EMIT CHANGES;

 Field    | Type
-------------------------------------------------------------------
 CATEGORY | VARCHAR(STRING)  (primary key) (Window type: TUMBLING)
 AMOUNT   | DECIMAL(12, 2)
-------------------------------------------------------------------

Queries that write from this TABLE
-----------------------------------
CTAS_TOTAL_AMOUNT_PER_CATEGORY_T_19 (RUNNING) : CREATE TABLE TOTAL_AMOUNT_PER_CATEGORY_T WITH (KAFKA_TOPIC='TOTAL_AMOUNT_PER_CATEGORY_T', PARTITIONS=8, REPLICAS=3) AS SELECT   TRANSACTION_CUSTOMER_S.CUS_CATEGORY CATEGORY,   SUM(TRANSACTION_CUSTOMER_S.TRA_AMOUNT) AMOUNT FROM TRANSACTION_CUSTOMER_S TRANSACTION_CUSTOMER_S WINDOW TUMBLING ( SIZE 30 SECONDS , RETENTION 120 SECONDS , GRACE PERIOD 60 SECONDS )  GROUP BY TRANSACTION_CUSTOMER_S.CUS_CATEGORY EMIT CHANGES;

For query topology and execution plan please run: EXPLAIN <QueryId>

Local runtime statistics
------------------------


(Statistics of the local KSQL server interaction with the Kafka topic TOTAL_AMOUNT_PER_CATEGORY_T)

Consumer Groups summary:

Consumer Group       : _confluent-ksql-ksqldb-clusterquery_CTAS_TOTAL_AMOUNT_PER_CATEGORY_T_19
<no offsets committed by this group yet>
ksql>
```

Push-query on table to get the changes

```sql
SELECT * FROM TOTAL_AMOUNT_PER_CATEGORY_T EMIT CHANGES;
```


Pull-query to get the current state for a category

```sql
SELECT * FROM TOTAL_AMOUNT_PER_CATEGORY_T  WHERE category = 'A';
```

Pull-query to get the current state for category with the `windowstart` and `windowend` converted to a readable timestamp

```sql
SELECT category
,	amount
,	FROM_UNIXTIME (WINDOWSTART)		AS from_ts
, 	FROM_UNIXTIME (WINDOWEND)			AS to_ts
FROM TOTAL_AMOUNT_PER_CATEGORY_T
WHERE category = 'A';
```

Insert a transaction with Event Time (`ROWTIME`) 60 seconds ago:

```sql
INSERT INTO transaction_s (ROWTIME, email_address, tx_id, card_number, timestamp, amount)
VALUES (UNIX_TIMESTAMP() - 60000, 'peter.muster@acme.com', '0cf100ca-993c-427f-9ea5-e892ef350363', '352642227248344', FROM_UNIXTIME(UNIX_TIMESTAMP()), 100.00);
```

It will be counted in the previous window. 

Insert a transaction with Event Time (`ROWTIME`) 3 minutes ago:

```sql
INSERT INTO transaction_s (ROWTIME, email_address, tx_id, card_number, timestamp, amount)
VALUES (UNIX_TIMESTAMP() - 180000, 'peter.muster@acme.com', '0cf100ca-993c-427f-9ea5-e892ef350363', '352642227248344', FROM_UNIXTIME(UNIX_TIMESTAMP()), 100.00);
```

it will not be treated as a late arrival, as if falls out of the grace period. 

## Anomalies

If a single credit card is transacted many times within a short duration, there's probably something suspicious going on. A table is an ideal choice to model this because you want to aggregate events over time and find activity that spans multiple events.

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
    FROM transaction_s
    WINDOW TUMBLING (SIZE 30 SECONDS, RETENTION 1000 DAYS)
    GROUP BY card_number
    HAVING count(*) >= 3
    EMIT CHANGES;
```

Now create 3 transactions for the same credit card number within 30 seconds and it show up as a potential anomaly.

## Rest API

You can execute a ksqlDB `SELECT` directly over the [REST API](https://docs.ksqldb.io/en/latest/developer-guide/api/) using a `POST` on `/query-stream` endpoint

```bash
curl -X POST -H 'Content-Type: application/vnd.ksql.v1+json' -i http://dataplatform:8088/query-stream --data '{
  "sql": "SELECT card_number, amount, tx_id FROM transaction_s EMIT CHANGES;",
  "properties": {
    "ksql.streams.auto.offset.reset": "latest"
  }
}'
```
This way we can directly get the data into a GUI client.
