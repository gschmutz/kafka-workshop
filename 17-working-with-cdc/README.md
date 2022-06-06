# Working with Change Data Capture (CDC)

In this workshop we will see various CDC solutions in action

1. Polling-based CDC using Kafka Connect
2. Log-based CDC using Debezium and Kafka Connect
3. Outbox Pattern using Debezium and Kafka Connect

## Creating person Postgresql Database

The `driver` table holds the information of the drivers working for us. Let's connect to the postgresql database running as part of the dataplatform and create the table

```bash
docker exec -ti postgresql psql -d postgres -U postgres
``` 

If the table is not available, this is the code to run 

```sql
CREATE SCHEMA IF NOT EXISTS person;

SET search_path TO person;

DROP TABLE IF EXISTS address;
DROP TABLE IF EXISTS person;

CREATE TABLE "person"."person" (
    "id" integer NOT NULL,
    "title" character varying(8),
    "first_name" character varying(50),
    "last_name" character varying(50),
    "email" character varying(50),
    "modifieddate" timestamp DEFAULT now() NOT NULL,
    CONSTRAINT "person_pk" PRIMARY KEY ("id")
);

CREATE TABLE "person"."address" (
    "id" integer NOT NULL,
    "person_id" integer NOT NULL,
    "street" character varying(50),
    "zip_code" character varying(10),
    "city" character varying(50),
    "modifieddate" timestamp DEFAULT now() NOT NULL,
    CONSTRAINT "address_pk" PRIMARY KEY ("id")
);

ALTER TABLE ONLY "person"."address" ADD CONSTRAINT "address_person_fk" FOREIGN KEY (person_id) REFERENCES person(id) NOT DEFERRABLE;
```

Let's add a first person and address as a sample:

```sqk
INSERT INTO person (id, title, first_name, last_name, email)
VALUES (1, 'Mr', 'Peter', 'Muster', 'peter.muster@somecorp.com');

INSERT INTO address (id, person_id, street, zip_code, city)
VALUES (1, 1, 'Somestreet 10', '9999', 'Somewhere');
```

## Polling-based CDC using Kafka Connect

To use the Polling-based CDC with Kafka Connect, we have to install the [JDBC Source Connector](https://www.confluent.io/hub/confluentinc/kafka-connect-jdbc) on our Kafka Connect cluster. 

On the workshop environment, this has already been done. You can check the available connectors either from [Kafka Connect UI](http://192.168.1.162:28103/#/cluster/kafka-connect-1/select-connector) or by using the REST API `curl -XGET http://$DOCKER_HOST_IP:8083/connector-plugins | jq`

Let's create a topic for `person_address` and `person_person`

```bash
docker exec -ti kafka-1 kafka-topics --bootstrap-server kafka-1:19092 --create --topic priv.person.cdc.v1 --partitions 8 --replication-factor 3

docker exec -ti kafka-1 kafka-topics --bootstrap-server kafka-1:19092 --create --topic priv.address.cdc.v1 --partitions 8 --replication-factor 3
```

Now let's start two consumers, one on each topic, in separate terminal windows

```bash
kcat -b kafka-1:19092 -t priv.person.cdc.v1 -f '[%p] %k: %s\n' -q
```

```bash
kcat -b kafka-1:19092 -t priv.address.cdc.v1 -f '[%p] %k: %s\n' -q
```

By using the `-f` option we tell `kcat` to also print the partition number and the key for each message.

Now let's create and start the JDBC Source connector

```bash
curl -X "POST" "$DOCKER_HOST_IP:8083/connectors" \
     -H "Content-Type: application/json" \
     -d $'{
  "name": "jdbc-driver-source",
  "config": {
    "connector.class": "JdbcSourceConnector",
    "tasks.max": "1",
    "connection.url":"jdbc:postgresql://postgresql/postgres?user=postgres&password=abc123!",
    "mode": "timestamp",
    "timestamp.column.name":"modifieddate",
    "poll.interval.ms":"10000",
    "table.whitelist":"person,address",
    "validate.non.null":"false",
    "topic.prefix":"priv.",
    "key.converter":"org.apache.kafka.connect.storage.StringConverter",
    "key.converter.schemas.enable": "false",
    "value.converter":"org.apache.kafka.connect.json.JsonConverter",
    "value.converter.schemas.enable": "false",
    "name": "jdbc-driver-source",
     "transforms":"createKey,extractInt,addSuffix",
     "transforms.createKey.type":"org.apache.kafka.connect.transforms.ValueToKey",
     "transforms.createKey.fields":"id",
     "transforms.extractInt.type":"org.apache.kafka.connect.transforms.ExtractField$Key",
     "transforms.extractInt.field":"id",
     "transforms.addSuffix.type":"org.apache.kafka.connect.transforms.RegexRouter",
     "transforms.addSuffix.regex":".*",
     "transforms.addSuffix.replacement":"$0.cdc.v1"
  }
}'
```

You can find the documentation on the configuration settings [here](https://docs.confluent.io/kafka-connect-jdbc/current/source-connector/source_config_options.html).

After a maximum of 10 seconds (which is the polling interval we set), we should see one message each in the two topic, one for the record in the `person` and one for the record in the `address` table. 

Let's discuss some of the configurations in the connector.

The key get's extracted using two [Single Message Transforms (SMT)](https://docs.confluent.io/platform/current/connect/transforms/overview.html), the `ValueToKey` and the `ExtractField`. They are chained together to set the key for data coming from a JDBC Connector. During the transform, `ValueToKey` copies the message id field into the message key and then `ExtractField` extracts just the integer portion of that field.

```json
     "transforms.createKey.type":"org.apache.kafka.connect.transforms.ValueToKey",
     "transforms.createKey.fields":"id",
     "transforms.extractInt.type":"org.apache.kafka.connect.transforms.ExtractField$Key",
     "transforms.extractInt.field":"id",
```

To also add a suffix to the topic (we have added a prefix using the `topic.prefix` configuration) we can use the `RegexRouter` SMT

```json
     "transforms.addSuffix.type":"org.apache.kafka.connect.transforms.RegexRouter",
     "transforms.addSuffix.regex":".*",
     "transforms.addSuffix.replacement":"$0.cdc.v1"
```

Let's add some more data to the tables and check how quick the data will appear in the topics. 

In a new terminal window, 

```bash
docker exec -ti postgresql psql -d postgres -U postgres
``` 

```sql
INSERT INTO person.address (id, person_id, street, zip_code, city)
VALUES (2, 1, 'Holiday 10', '1999', 'Ocean Somewhere');
```

Now let's add a new person, first without an address

```sql
INSERT INTO person.person (id, title, first_name, last_name, email)
VALUES (2, 'Ms', 'Karen', 'Muster', 'peter.muster@somecorp.com');
```

and now also add the address

```sql
INSERT INTO person.address (id, person_id, street, zip_code, city)
VALUES (3, 2, 'Somestreet 10', '9999', 'Somewhere');
```

