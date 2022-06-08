# Working with Kafka Connect and Change Data Capture (CDC)

In this workshop we will see various CDC solutions in action

1. Polling-based CDC using Kafka Connect
2. Log-based CDC using Debezium and Kafka Connect
3. Transactional Outbox Pattern using Debezium and Kafka Connect

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

## Polling-based CDC using Kafka Connect

To use the Polling-based CDC with Kafka Connect, we have to install the [JDBC Source Connector](https://www.confluent.io/hub/confluentinc/kafka-connect-jdbc) on our Kafka Connect cluster. 

On the workshop environment, this has already been done. You can check the available connectors either from [Kafka Connect UI](http://dataplatform:28103/#/cluster/kafka-connect-1/select-connector) or by using the REST API `curl -XGET http://$DOCKER_HOST_IP:8083/connector-plugins | jq`

#### Create the necessary topics

Let's create a topic for the change records of the two tables 'person` and `address`: 

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

#### Create some initial data in Postgresql

Let's add a first person and address as a sample. In a terminal connect to Postgresql

```bash
docker exec -ti postgresql psql -d postgres -U postgres
``` 

execute the following SQL INSERT statements

```sql
INSERT INTO person.person (id, title, first_name, last_name, email)
VALUES (1, 'Mr', 'Peter', 'Muster', 'peter.muster@somecorp.com');

INSERT INTO person.address (id, person_id, street, zip_code, city)
VALUES (1, 1, 'Somestreet 10', '9999', 'Somewhere');
```

#### Create a JDBC Source connector instance

Now let's create and start the JDBC Source connector

```bash
curl -X "POST" "$DOCKER_HOST_IP:8083/connectors" \
     -H "Content-Type: application/json" \
     -d $'{
  "name": "person.jdbcsrc.cdc",
  "config": {
    "connector.class": "io.confluent.connect.jdbc.JdbcSourceConnector",
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

You can also monitor the connector in the Kafka Connect UI: <http://dataplatform:28103/>

#### Configuration options

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

#### Add more data

Let's add some more data to the tables and check how quick the data will appear in the topics. 

In a new terminal window, again connect to postgresql

```bash
docker exec -ti postgresql psql -d postgres -U postgres
``` 

Add a new `address` to `person` with `id=1`.

```sql
INSERT INTO person.address (id, person_id, street, zip_code, city)
VALUES (2, 1, 'Holiday 10', '1999', 'Ocean Somewhere');
```

Now let's add a new `person`, first without an `address`

```sql
INSERT INTO person.person (id, title, first_name, last_name, email)
VALUES (2, 'Ms', 'Karen', 'Muster', 'karen.muster@somecorp.com');
```

and now also add the `address`

```sql
INSERT INTO person.address (id, person_id, street, zip_code, city)
VALUES (3, 2, 'Somestreet 10', '9999', 'Somewhere');
```

#### Remove the connector

We have successfully tested the query-based CDC using the JDBC Source connector. So let's remove the connector.

```bash
curl -X "DELETE" "http://$DOCKER_HOST_IP:8083/connectors/person.jdbcsrc.cdc"
```

## Log-based CDC using Debezium and Kafka Connect

In this section we will see how we can use [Debezium](https://debezium.io/) and it's Postgresql connector to perform log-based CDC on the two tables.


#### Download the Debezium connector

The [Debezium connector](https://www.confluent.io/hub/debezium/debezium-connector-postgresql) is not installed as part of the stack. We can either add it when generating the `docker-compose.yml` using `platys` or we manually do it using the Confluent Hub CLI, which is part of the `kafka-connect-1` docker container. Install the connector by executing the following statement:

```bash
docker exec -ti kafka-connect-1 confluent-hub install debezium/debezium-connector-postgresql:1.9.2 --no-prompt --component-dir /etc/kafka-connect/cflthub-plugins --verbose
```

and then restart both `kafka-connect-1` and `kafka-connect-2` so that the Kafka Connect cluster picks up the newly installed connector:

```bash
docker restart kafka-connect-1 
docker restart kafka-connect-2
```

#### Create some initial data in Postgresql

Let's add a first person and address as a sample. In a terminal connect to Postgresql

```bash
docker exec -ti postgresql psql -d postgres -U postgres
``` 

and execute the following SQL TRUNCATE followed by INSERT statements

```sql
TRUNCATE person.person CASCADE;

INSERT INTO person.person (id, title, first_name, last_name, email)
VALUES (1, 'Mr', 'Peter', 'Muster', 'peter.muster@somecorp.com');

INSERT INTO person.address (id, person_id, street, zip_code, city)
VALUES (1, 1, 'Somestreet 10', '9999', 'Somewhere');
```

#### Create the Debezium connector

Now with the connector installed and the Kafka Connect cluster restarted, let's create and start the connector:

```bash
curl -X PUT \
  "http://$DOCKER_HOST_IP:8083/connectors/person.dbzsrc.cdc/config" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
  "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
  "tasks.max": "1",
  "database.server.name": "postgresql",
  "database.port": "5432",
  "database.user": "postgres",
  "database.password": "abc123!",  
  "database.dbname": "postgres",
  "schema.include.list": "person",
  "table.include.list": "person.person,person.address",
  "plugin.name": "pgoutput",
  "tombstones.on.delete": "false",
  "database.hostname": "postgresql",
  "topic.creation.default.replication.factor": 3,
  "topic.creation.default.partitions": 8,
  "topic.creation.default.cleanup.policy": "compact"
}'
```

#### Check the data in the two Kafka topics

Now let's start two consumers, one on each topic, in separate terminal windows. This time the data is in Avro, as we did not overwrite the serializers. By default the topics are formatted using this naming convention: `<db-host>.<schema>.<table>`

```bash
kcat -b kafka-1:19092 -t postgresql.person.person -f '[%p] %k: %s\n' -q -s avro -r http://schema-registry-1:8081
```

and you should get the change record for `person` table

```json
[0] {"id": 1}: {"before": null, "after": {"Value": {"id": 1, "title": {"string": "Mr"}, "first_name": {"string": "Peter"}, "last_name": {"string": "Muster"}, "email": {"string": "peter.muster@somecorp.com"}, "modifieddate": 1654534797865686}}, "source": {"version": "1.8.1.Final", "connector": "postgresql", "name": "postgresql", "ts_ms": 1654534797866, "snapshot": {"string": "false"}, "db": "postgres", "sequence": {"string": "[\"23426176\",\"23426176\"]"}, "schema": "person", "table": "person", "txId": {"long": 556}, "lsn": {"long": 23426176}, "xmin": null}, "op": "c", "ts_ms": {"long": 1654534798250}, "transaction": null}
```

We can see that the key is also Avro-serialized (`{"id": 1}`).

Now do the same for the `postgresql.person.address` topic

```bash
kcat -b kafka-1:19092 -t postgresql.person.address -f '[%p] %k: %s\n' -q -s avro -r http://schema-registry-1:8081 
```

and you should get the change record for the `address` table.

Let's use `jq` to format the value only (by removing the `-f` parameter). 

```
kcat -b kafka-1:9092 -t postgresql.person.person -f '%s\n' -q -s avro -r http://schema-registry-1:8081 -u | jq
```

we can see that the change record is wrapped inside the `after` field. The `op` field show the operation, which shows `c` for create/insert.

```json
{
  "before": null,
  "after": {
    "Value": {
      "id": 1,
      "title": {
        "string": "Mr"
      },
      "first_name": {
        "string": "Peter"
      },
      "last_name": {
        "string": "Muster"
      },
      "email": {
        "string": "peter.muster@somecorp.com"
      },
      "modifieddate": 1654534797865686
    }
  },
  "source": {
    "version": "1.8.1.Final",
    "connector": "postgresql",
    "name": "postgresql",
    "ts_ms": 1654534797866,
    "snapshot": {
      "string": "false"
    },
    "db": "postgres",
    "sequence": {
      "string": "[\"23426176\",\"23426176\"]"
    },
    "schema": "person",
    "table": "person",
    "txId": {
      "long": 556
    },
    "lsn": {
      "long": 23426176
    },
    "xmin": null
  },
  "op": "c",
  "ts_ms": {
    "long": 1654534798250
  },
  "transaction": null
}
```

Now let's do an update of the `person` table (keep the `kcat` running). 

```sql
UPDATE person.person SET first_name = UPPER(first_name);
```

and you should get the new change record

```json
{
  "before": null,
  "after": {
    "Value": {
      "id": 1,
      "title": {
        "string": "Mr"
      },
      "first_name": {
        "string": "PETER"
      },
      "last_name": {
        "string": "Muster"
      },
      "email": {
        "string": "peter.muster@somecorp.com"
      },
      "modifieddate": 1654534797865686
    }
  },
  "source": {
    "version": "1.8.1.Final",
    "connector": "postgresql",
    "name": "postgresql",
    "ts_ms": 1654535300862,
    "snapshot": {
      "string": "false"
    },
    "db": "postgres",
    "sequence": {
      "string": "[\"23426936\",\"23427224\"]"
    },
    "schema": "person",
    "table": "person",
    "txId": {
      "long": 558
    },
    "lsn": {
      "long": 23427224
    },
    "xmin": null
  },
  "op": "u",
  "ts_ms": {
    "long": 1654535301178
  },
  "transaction": null
}
```

#### Also produce the before image of an Update

You can see that there is `before` field but it is `null`. By default the Postgresql [`REPLICA IDENTITY`](https://www.postgresql.org/docs/9.6/sql-altertable.html#SQL-CREATETABLE-REPLICA-IDENTITY) will not produce the before image.

Let's change it on the `person` table to see the difference

```sql
alter table person.person REPLICA IDENTITY FULL;
```

and then once more update the `person` table (keep the `kcat` running).

```sql
UPDATE person.person SET first_name = LOWER(first_name);
```

we can now see both the data `before` and `after` the UPDATE:

```json
{
  "before": {
    "Value": {
      "id": 1,
      "title": {
        "string": "Mr"
      },
      "first_name": {
        "string": "PETER"
      },
      "last_name": {
        "string": "Muster"
      },
      "email": {
        "string": "peter.muster@somecorp.com"
      },
      "modifieddate": 1654534797865686
    }
  },
  "after": {
    "Value": {
      "id": 1,
      "title": {
        "string": "Mr"
      },
      "first_name": {
        "string": "peter"
      },
      "last_name": {
        "string": "Muster"
      },
      "email": {
        "string": "peter.muster@somecorp.com"
      },
      "modifieddate": 1654534797865686
    }
  },
  "source": {
    "version": "1.8.1.Final",
    "connector": "postgresql",
    "name": "postgresql",
    "ts_ms": 1654535554791,
    "snapshot": {
      "string": "false"
    },
    "db": "postgres",
    "sequence": {
      "string": "[\"23435472\",\"23435528\"]"
    },
    "schema": "person",
    "table": "person",
    "txId": {
      "long": 560
    },
    "lsn": {
      "long": 23435528
    },
    "xmin": null
  },
  "op": "u",
  "ts_ms": {
    "long": 1654535555154
  },
  "transaction": null
}
```

#### Check the latency from the UPDATE until you consume the message from the topic

Let's do an update on the `address` table and make sure that you see the `kcat` in another terminal window

```sql
UPDATE person.address SET street = UPPER(street);
```

```bash
kcat -b kafka-1:19092 -t postgresql.person.address -f '[%p] %k: %s\n' -q -s avro -r http://schema-registry-1:8081 
```

You should see a very minimal latency between doing the update and the message in Kafka.

#### Change the name of the Kafka Topic

The topic name might not fit with your naming conventions. You can change the name using a [Single Message Transforms (SMT)](https://docs.confluent.io/platform/current/connect/transforms/overview.html) as we have already seen in the query-based CDC workshop above. 

Let's remove the connector and delete the two topics

```bash
curl -X "DELETE" "http://$DOCKER_HOST_IP:8083/connectors/person.dbzsrc.cdc"

docker exec -ti kafka-1 kafka-topics --delete --bootstrap-server kafka-1:19092 --topic postgresql.person.person
docker exec -ti kafka-1 kafka-topics --delete --bootstrap-server kafka-1:19092 --topic postgresql.person.address
```

Now recreate the connector, this time adding the necessary `RegexRouter` SMT:


```bash
curl -X PUT \
  "http://$DOCKER_HOST_IP:8083/connectors/person.dbzsrc.cdc/config" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
  "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
  "tasks.max": "1",
  "database.server.name": "postgresql",
  "database.port": "5432",
  "database.user": "postgres",
  "database.password": "abc123!",  
  "database.dbname": "postgres",
  "schema.include.list": "person",
  "table.include.list": "person.person,person.address",
  "plugin.name": "pgoutput",
  "tombstones.on.delete": "false",
  "database.hostname": "postgresql",
  "transforms":"dropPrefix",  
  "transforms.dropPrefix.type": "org.apache.kafka.connect.transforms.RegexRouter",  
  "transforms.dropPrefix.regex": "postgresql.person.(.*)",  
  "transforms.dropPrefix.replacement": "priv.$1.cdc.v2",
  "topic.creation.default.replication.factor": 3,
  "topic.creation.default.partitions": 8,
  "topic.creation.default.cleanup.policy": "compact"
}'
```

Now let's do an update on `person` table once more 

```sql
UPDATE person.person SET first_name = UPPER(first_name);

```
to see the new topic `priv.person.cdc.v2` created

```bash
kcat -b kafka-1:19092 -t priv.person.cdc.v2 -f '[%p] %k: %s\n' -q -s avro -r http://schema-registry-1:8081 
```

#### Remove the connector

We have successfully tested the log-based CDC connector. So let's remove the connector.

```bash
curl -X "DELETE" "http://$DOCKER_HOST_IP:8083/connectors/person.dbzsrc.cdc"
```

## Transactional Outbox Pattern using Debezium and Kafka Connect

In this section we will see how we can use [Debezium](https://debezium.io/) and it's Postgresql connector to perform log-based CDC on an special `outbox` table, implementing the [Transactional Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html).

#### Download the Debezium connector (skip this, if you have done it before)

The [Debezium connector](https://www.confluent.io/hub/debezium/debezium-connector-postgresql) is not installed as part of the stack. We can either add it when generating the `docker-compose.yml` using `platys` or we manually do it using the Confluent Hub CLI, which is part of the `kafka-connect-1` docker container. Install the connector by executing the following statement:

```bash
docker exec -ti kafka-connect-1 confluent-hub install debezium/debezium-connector-postgresql:1.9.2 --no-prompt --component-dir /etc/kafka-connect/cflthub-plugins --verbose
```

and then restart both `kafka-connect-1` and `kafka-connect-2` so that the Kafka Connect cluster picks up the newly installed connector:

```bash
docker restart kafka-connect-1 
docker restart kafka-connect-2
```

#### Create the `outbox` table

In a new terminal window, connect to Postgresql

```bash
docker exec -ti postgresql psql -d postgres -U postgres
```

Now create a new table `outbox`

```sql
SET search_path TO person;

DROP TABLE IF EXISTS "outbox";
CREATE TABLE outbox (
    "id" uuid NOT NULL,
    "aggregate_id" bigint,
    "created_at" timestamp,
    "event_type" character varying(255),
    "payload_json" character varying(5000),
    CONSTRAINT "outbox_pk" PRIMARY KEY ("id")
);
```

#### Create the Debezium connector

```bash
curl -X PUT \
  "http://$DOCKER_HOST_IP:8083/connectors/person.dbzsrc.outbox/config" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
  "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
  "tasks.max": "1",
  "database.server.name": "postgresql",
  "database.port": "5432",
  "database.user": "postgres",
  "database.password": "abc123!",  
  "database.dbname": "postgres",
  "schema.include.list": "person",
  "table.include.list": "person.outbox",
  "plugin.name": "pgoutput",
  "tombstones.on.delete": "false",
  "database.hostname": "postgresql",
  "transforms": "outbox",  
  "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
  "transforms.outbox.table.field.event.id": "id",
  "transforms.outbox.table.field.event.key": "aggregate_id",
  "transforms.outbox.table.field.event.payload": "payload_json",
  "transforms.outbox.table.field.event.timestamp": "created_at",
  "transforms.outbox.route.by.field": "event_type",
  "transforms.outbox.route.topic.replacement": "priv.${routedByValue}.event.v1",
  "topic.creation.default.replication.factor": 3,
  "topic.creation.default.partitions": 8
}'
```

#### Add data to `outbox` table

Let' first simulate a `CustomerCreated` event:

```sql
INSERT INTO person.outbox (id, aggregate_id, created_at, event_type, payload_json)
VALUES (gen_random_uuid(), 13256, current_timestamp, 'CustomerCreated', '{"id":13256,"personType":"IN","nameStyle":"0","firstName":"Carson","middleName":null,"lastName":"Washington","emailPromotion":1,"addresses":[{"addressTypeId":2,"id":22326,"addressLine1":"3809 Lancelot Dr.","addressLine2":null,"city":"Glendale","stateProvinceId":9,"postalCode":"91203","country":{"isoCode2":"US","isoCode3":"USA","numericCode":840,"shortName":"United States of America"},"lastChangeTimestamp":"2022-05-09T20:45:11.798376"}],"phones":[{"phoneNumber":"518-555-0192","phoneNumberTypeId":1,"phoneNumberType":"Cell"}],"emailAddresses":[{"id":12451,"emailAddress":"carson12@adventure-works.com"}]}');
```

A new Kafka topic `priv.CustomerCreated.event.v1` will get created with the message. 

```bash
kcat -b kafka-1:19092 -t priv.CustomerCreated.event.v1 -f '[%p] %k: %s\n' -q -s avro -r http://schema-registry-1:8081 
```

```bash
[4] {"long": 13256}: {"string": "{\"id\":13256,\"personType\":\"IN\",\"nameStyle\":\"0\",\"firstName\":\"Carson\",\"middleName\":null,\"lastName\":\"Washington\",\"emailPromotion\":1,\"addresses\":[{\"addressTypeId\":2,\"id\":22326,\"addressLine1\":\"3809 Lancelot Dr.\",\"addressLine2\":null,\"city\":\"Glendale\",\"stateProvinceId\":9,\"postalCode\":\"91203\",\"country\":{\"isoCode2\":\"US\",\"isoCode3\":\"USA\",\"numericCode\":840,\"shortName\":\"United States of America\"},\"lastChangeTimestamp\":\"2022-05-09T20:45:11.798376\"}],\"phones\":[{\"phoneNumber\":\"518-555-0192\",\"phoneNumberTypeId\":1,\"phoneNumberType\":\"Cell\"}],\"emailAddresses\":[{\"id\":12451,\"emailAddress\":\"carson12@adventure-works.com\"}]}"}
```

Now let's also simulate another event, an `CustomerMoved` event

```sql
INSERT INTO person.outbox (id, aggregate_id, created_at, event_type, payload_json)
VALUES (gen_random_uuid(), 13256, current_timestamp, 'CustomerMoved', '{"customerId": 13256, "address": {"addressTypeId":2,"id":22326,"addressLine1":"3809 Lancelot Dr.","addressLine2":null,"city":"Glendale","stateProvinceId":9,"postalCode":"91203","country":{"isoCode2":"US","isoCode3":"USA","numericCode":840,"shortName":"United States of America"},"lastChangeTimestamp":"2022-05-09T20:45:11.798376"}}');
```

this will end up in a new topic named `priv.CustomerMoved.event.v1`

```bash
kcat -b kafka-1:19092 -t priv.CustomerMoved.event.v1 -f '[%p] %k: %s\n' -q -s avro -r http://schema-registry-1:8081 
```

```bash
[4] {"long": 13256}: {"string": "{\"customerId\": 13256, \"address\": {\"addressTypeId\":2,\"id\":22326,\"addressLine1\":\"3809 Lancelot Dr.\",\"addressLine2\":null,\"city\":\"Glendale\",\"stateProvinceId\":9,\"postalCode\":\"91203\",\"country\":{\"isoCode2\":\"US\",\"isoCode3\":\"USA\",\"numericCode\":840,\"shortName\":\"United States of America\"},\"lastChangeTimestamp\":\"2022-05-09T20:45:11.798376\"}}"}
```

#### Remove the connector

We have successfully tested the transactional outbox using Debezium connector. So let's remove the connector.

```bash
curl -X "DELETE" "http://$DOCKER_HOST_IP:8083/connectors/person.dbzsrc.outbox"
```