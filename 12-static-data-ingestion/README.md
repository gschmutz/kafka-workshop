# Static Data Ingestion into Kafka

Let's simulate some rather static data about the drivers of our truck fleet.

![Alt Image Text](./images/joining-static-data-with-ksql-overview.png "Schema Registry UI")

## Create a Postgresql instance

For that let's add a Postgresql database to our streaming platform. Add the following two service definition to the `docker-compose.override.yml` file. 

```
  adminer:
    image: adminer
    ports:
      - 18080:8080

  postgresql:
    image: mujz/pagila
    environment:
      - POSTGRES_PASSWORD=sample
      - POSTGRES_USER=sample
      - POSTGRES_DB=sample
```

Re-Sync the Streaming Platform with the definition in the docker compose file.
 
```
docker-compose up -d
```

The images for PostgreSQL as well as a simple Admin tool will be downloaded and started. 

## Create the driver table and load some data

Now let's connect to the database as user `sample`. 

```
docker exec -ti postgresql bash
psql -d sample -U sample
```

On the command prompt, create the table `driver`.

```
DROP TABLE driver;

CREATE TABLE driver (id BIGINT, first_name CHARACTER VARYING(45), last_name CHARACTER VARYING(45), available CHARACTER VARYING(1), birthdate DATE, last_update TIMESTAMP);

ALTER TABLE driver ADD CONSTRAINT driver_pk PRIMARY KEY (id);
```

Let's add some initial data to the newly created table. 

```
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (10,'Diann', 'Butler', 'Y', '10-JUN-68', CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (11,'Micky', 'Isaacson', 'Y', '31-AUG-72' ,CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (12,'Laurence', 'Lindsey', 'Y', '19-MAY-78' ,CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (13,'Pam', 'Harrington', 'Y','10-JUN-68' ,CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (14,'Brooke', 'Ferguson', 'Y','10-DEC-66' ,CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (15,'Clint','Hudson', 'Y','5-JUN-75' ,CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (16,'Ben','Simpson', 'Y','11-SEP-74' ,CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (17,'Frank','Bishop', 'Y','3-OCT-60' ,CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (18,'Trevor','Hines', 'Y','23-FEB-78' ,CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (19,'Christy','Stephens', 'Y','11-JAN-73' ,CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (20,'Clarence','Lamb', 'Y','15-NOV-77' ,CURRENT_TIMESTAMP);
```

Keep this window open and connected to PostgeSQL, we will need it again later.
 
## Create a new topic truck_driver

Now let's create a new topic `truck_driver`, a compacted topic which will hold the latest view of all our drivers. 

First connect to one of the Kafka brokers.

```
docker exec -ti streamingplatform_broker-1_1 bash
```

And then perform the `kafka-topics --create` command:

```
kafka-topics --zookeeper zookeeper-1:2181 --create --topic truck_driver --partitions 8 --replication-factor 2 --config cleanup.policy=compact --config segment.ms=100 --config delete.retention.ms=100 --config min.cleanable.dirty.ratio=0.001
```

Eventhough we have no messages yet, let's create a consumer wich reads the new topic from the beginning. 

```
kafka-console-consumer --bootstrap-server kafka-1:19092 --topic truck_driver --from-beginning
```

Keep it running, we will come back to it in a minute!

## Create the Kafka Connector to pull driver from Postgresql

No it's time to start a Kafka JDBC connector, which gets all the data from the `driver` table and publishes into the `truck_driver` topic. 

Similar to the way we have configured and created the MQTT connect, create a new file `configure-connect-jdbc.sh` in the `scripts` folder.
 
```
cd scripts
nano configure-connect-jdbc.sh
```

Add the following script logic to the file. 

```
#!/bin/bash

echo "removing JDBC Source Connector"

curl -X "DELETE" "http://$DOCKER_HOST_IP:8083/connectors/jdbc-driver-source"

echo "creating JDBC Source Connector"

## Request
curl -X "POST" "$DOCKER_HOST_IP:8083/connectors" \
     -H "Content-Type: application/json" \
     -d $'{
  "name": "jdbc-driver-source",
  "config": {
    "connector.class": "JdbcSourceConnector",
    "tasks.max": "1",
    "connection.url":"jdbc:postgresql://db/sample?user=sample&password=sample",
    "mode": "timestamp",
    "timestamp.column.name":"last_update",
    "table.whitelist":"driver",
    "validate.non.null":"false",
    "topic.prefix":"truck_",
    "key.converter":"org.apache.kafka.connect.storage.StringConverter",
    "key.converter.schemas.enable": "false",
    "value.converter":"org.apache.kafka.connect.json.JsonConverter",
    "value.converter.schemas.enable": "false",
    "name": "jdbc-driver-source",
     "transforms":"createKey,extractInt",
     "transforms.createKey.type":"org.apache.kafka.connect.transforms.ValueToKey",
     "transforms.createKey.fields":"id",
     "transforms.extractInt.type":"org.apache.kafka.connect.transforms.ExtractField$Key",
     "transforms.extractInt.field":"id"
  }
}'
```

Make sure that the script is executable and execute it. 

```
sudo chmod +x configure-connect-jdbc.sh
./configure-connect-jdbc.sh
```

After a while you should see each record from the `driver` table appearing as an event in the `trucking_driver` topic. 

Go back to the PostgreSQL shell and some additional driver data:

```
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (21,'Lila', 'Page', 'Y', '5-APR-77', CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (22,'Patricia', 'Coleman', 'Y', '11-AUG-80' ,CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (23,'Jeremy', 'Olson', 'Y', '13-JUN-82', CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (24,'Walter', 'Ward', 'Y', '24-JUL-85', CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (25,'Kristen', ' Patterson', 'Y', '14-JUN-73', CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (26,'Jacquelyn', 'Fletcher', 'Y', '24-AUG-85', CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (27,'Walter', '  Leonard', 'Y', '12-SEP-88', CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (28,'Della', ' Mcdonald', 'Y', '24-JUL-79', CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (29,'Leah', 'Sutton', 'Y', '12-JUL-75', CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (30,'Larry', 'Jensen', 'Y', '14-AUG-83', CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (31,'Rosemarie', 'Ruiz', 'Y', '22-SEP-80', CURRENT_TIMESTAMP);
INSERT INTO "driver" ("id", "first_name", "last_name", "available", "birthdate", "last_update") VALUES (32,'Shaun', ' Marshall', 'Y', '22-JAN-85', CURRENT_TIMESTAMP);
```

The new records should also appear as events in the `truck_driver` topic. 

Now also update some existing records: 

```
UPDATE "driver" SET "available" = 'N', "last_update" = CURRENT_TIMESTAMP  WHERE "id" = 21;
UPDATE "driver" SET "available" = 'N', "last_update" = CURRENT_TIMESTAMP  WHERE "id" = 14;
```

Again we should see the updates as new events in the `truck_driver` topic. 

No let's use the driver data to enrich the `dangerous_driving_s` stream from workshop [IoT Data Ingestion through MQTT into Kafka](../06-iot-data-ingestion-over-mqtt/README.md). 

## Create a KSQL table on top of the trucking_driver topic

Connect to KSQL

```
docker run -it confluentinc/cp-ksql-cli:5.0.0-beta180702222458 http://192.168.25.137:8088 
```

Create a KSQL table on top of `truck_driver` topic, giving the topic a structure.

```
set 'commit.interval.ms'='5000';
set 'cache.max.bytes.buffering'='10000000';
set 'auto.offset.reset'='earliest';

DROP TABLE driver_t;
CREATE TABLE driver_t  \
   (id BIGINT,  \
   first_name VARCHAR, \
   last_name VARCHAR, \
   available VARCHAR, \
   birthdate VARCHAR) \
  WITH (kafka_topic='truck_driver', \
        value_format='JSON', \
        KEY = 'id');
```

Let's see what the driver_t table provides. It will show all the data because of the setting of `auto.offset.rest` to `earliest`.

```
SELECT * FROM driver_t;
```

Now let's use the table and join it to the `dangerous_driving_s` stream.

```
DROP STREAM dangerous_driving_and_driver_s;
CREATE STREAM dangerous_driving_and_driver_s  \
  WITH (kafka_topic='dangerous_driving_and_driver_s', \
        value_format='JSON', partitions=8) \
AS SELECT driverid, first_name, last_name, truckId, routeId ,eventType \
FROM dangerous_driving_s \
LEFT JOIN driver_t \
ON dangerous_driving_s.driverId = driver_t.id;
```

Let's see that the new stream provides the correct results.

```
SELECT * FROM dangerous_driving_and_driver_s;
```

Let's see it only for driver 11.


```
SELECT * FROM dangerous_driving_and_driver_s WHERE driverid = 11;
```

Let's use it for some analytics.

```
SELECT first_name, last_name, eventType, count(*) 
FROM dangerous_driving_and_driver_s 
WINDOW TUMBLING (size 20 seconds) 
GROUP BY first_name, last_name, eventType;
```
