#!/bin/bash

curl -X PUT \
  "http://${DATAPLATFORM_IP}:8083/connectors/order-outbox-connector/config" \
  -H 'Content-Type: application/json' \
  -H 'Accept: application/json' \
  -d '{
  "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
  "tasks.max": "1",

  "database.hostname": "postgresql",
  "database.port": "5432",
  "database.user": "postgres",
  "database.password": "abc123!",
  "database.dbname": "postgres",
  "topic.prefix": "debezium",
  "schema.include.list": "public",
  "table.include.list": "public.outbox",
  "plugin.name": "pgoutput",
  "publication.name": "debezium",
  "slot.name": "debezium",
  "tombstones.on.delete": "false",

  "transforms": "outbox",
  "transforms.outbox.type": "io.debezium.transforms.outbox.EventRouter",
  "transforms.outbox.table.field.event.id": "ce_id",
  "transforms.outbox.table.field.event.key": "ce_partitionkey",
  "transforms.outbox.table.field.event.type": "ce_type",
  "transforms.outbox.table.field.event.payload": "payload",
  "transforms.outbox.route.by.field": "aggregate_type",
  "transforms.outbox.route.topic.replacement": "outbox.${routedByValue}",
  "transforms.outbox.table.fields.additional.placement": "ce_id:header:ce_id,ce_source:header:ce_source,ce_time:header:ce_time,ce_type:header:ce_type,ce_specversion:header:ce_specversion,ce_datacontenttype:header:content-type,ce_dataschema:header:ce_dataschema,ce_subject:header:ce_subject,ce_partitionkey:header:ce_partitionkey",

  "key.converter": "org.apache.kafka.connect.storage.StringConverter",
  "value.converter": "org.apache.kafka.connect.converters.ByteArrayConverter",
  "topic.creation.default.replication.factor": 1,
  "topic.creation.default.partitions": 8
}'
