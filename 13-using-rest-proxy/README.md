# Rest Proxy

These APIs are available both on Confluent Server (as a part of Confluent Enterprise) and REST Proxy. When using the API in Confluent Server, all paths should be prefixed with `/kafka`. For example, the path to list clusters is:

  * **Confluent Server:** `/kafka/v3/clusters`
  * **REST Proxy:** `/v3/clusters`

All the folloing exampe

## List and desribe cluster

```bash
curl --silent -X GET http://dataplatform:18086/v3/clusters/ | jq
```

Result:

```
{
  "kind": "KafkaClusterList",
  "metadata": {
    "self": "http://kafka-rest-1:8086/v3/clusters",
    "next": null
  },
  "data": [
    {
      "kind": "KafkaCluster",
      "metadata": {
        "self": "http://kafka-rest-1:8086/v3/clusters/fE05MX_MQLWvruHKXUQeRg",
        "resource_name": "crn:///kafka=fE05MX_MQLWvruHKXUQeRg"
      },
      "cluster_id": "fE05MX_MQLWvruHKXUQeRg",
      "controller": {
        "related": "http://kafka-rest-1:8086/v3/clusters/fE05MX_MQLWvruHKXUQeRg/brokers/2"
      },
      "acls": {
        "related": "http://kafka-rest-1:8086/v3/clusters/fE05MX_MQLWvruHKXUQeRg/acls"
      },
      "brokers": {
        "related": "http://kafka-rest-1:8086/v3/clusters/fE05MX_MQLWvruHKXUQeRg/brokers"
      },
      "broker_configs": {
        "related": "http://kafka-rest-1:8086/v3/clusters/fE05MX_MQLWvruHKXUQeRg/broker-configs"
      },
      "consumer_groups": {
        "related": "http://kafka-rest-1:8086/v3/clusters/fE05MX_MQLWvruHKXUQeRg/consumer-groups"
      },
      "topics": {
        "related": "http://kafka-rest-1:8086/v3/clusters/fE05MX_MQLWvruHKXUQeRg/topics"
      },
      "partition_reassignments": {
        "related": "http://kafka-rest-1:8086/v3/clusters/fE05MX_MQLWvruHKXUQeRg/topics/-/partitions/-/reassignment"
      }
    }
  ]
}
```

Extract the cluster id from the response and set it as environment variable `CLUSTER_ID`:

```bash
export CLUSTER_ID=$(curl -X GET "http://dataplatform:18086/v3/clusters/" | jq -r ".data[0].cluster_id")
```

## Create a topic

To create a topic, use the topics endpoint POST `/clusters/{cluster_id}/topics` as shown below.

```bash
curl --silent -X POST -H "Content-Type: application/json" \
     --data '{"topic_name": "rest-proxy-topic", "replication_factor":"3", "partitions_count": "8"}' \
     http://dataplatform:18086/v3/clusters/${CLUSTER_ID}/topics | jq
```

Result:

```bash
{
  "kind": "KafkaTopic",
  "metadata": {
    "self": "http://kafka-rest-1:8086/v3/clusters/fE05MX_MQLWvruHKXUQeRg/topics/rest-proxy-topic",
    "resource_name": "crn:///kafka=fE05MX_MQLWvruHKXUQeRg/topic=rest-proxy-topic"
  },
  "cluster_id": "fE05MX_MQLWvruHKXUQeRg",
  "topic_name": "rest-proxy-topic",
  "is_internal": false,
  "replication_factor": 3,
  "partitions_count": 0,
  "partitions": {
    "related": "http://kafka-rest-1:8086/v3/clusters/fE05MX_MQLWvruHKXUQeRg/topics/rest-proxy-topic/partitions"
  },
  "configs": {
    "related": "http://kafka-rest-1:8086/v3/clusters/fE05MX_MQLWvruHKXUQeRg/topics/rest-proxy-topic/configs"
  },
  "partition_reassignments": {
    "related": "http://kafka-rest-1:8086/v3/clusters/fE05MX_MQLWvruHKXUQeRg/topics/rest-proxy-topic/partitions/-/reassignment"
  }
}
```

## List topics

To list detailed descriptions of all topics (internal and user created topics), use the topics endpoint GET `/clusters/{cluster_id}/topics/`

```bash     
curl --silent -X GET http://dataplatform:18086/v3/clusters/${CLUSTER_ID}/topics | jq
```     

to show only the topic names, use a grep on the output of `jq`

```bash     
curl --silent -X GET http://dataplatform:18086/v3/clusters/${CLUSTER_ID}/topics | jq | grep '.topic_name'
```     

## Describe a topic

To get a full description of a specified topic, use the topics endpoint GET `/clusters/{cluster_id}/topics/{topic_name}`

```bash     
curl --silent -X GET http://dataplatform:18086/v3/clusters/${CLUSTER_ID}/topics/rest-proxy-topic | jq
```     
     
## Working with JSON data    
     
### Produce JSON

```bash
curl --silent -X POST 'http://dataplatform:18086/topics/rest-proxy-topic' \
--header 'Content-Type: application/vnd.kafka.json.v2+json' \
--data-raw '{
  "records": [
    {
      "key": "1",
      "value": {
        "id": 1,
        "customerCode": 1,
        "telephone": "888582158",
        "email": "supplier1@test.com",
        "language": "EN"        
      }
    }
  ]
}'
```

### Consume JSON

1. create a consumer `ci1` belonging to consumer group `cg1`. 
Specify `auto.offset.reset` to be `earliest` so it starts at the beginning of the topic.

```bash
curl --silent -X POST \
     -H "Content-Type: application/vnd.kafka.v2+json" \
     --data '{"name": "ci1", "format": "json", "auto.offset.reset": "earliest"}' \
     http://dataplatform:18086/consumers/cg1 | jq .
```

Result: 

```
{
  "instance_id": "ci1",
  "base_uri": "http://kafka-rest-1:8086/consumers/cg1/instances/ci1"
}
```

2. Subscribe the consumer to topic `rest-proxy-topic`

```bash
curl --silent -X POST \
     -H "Content-Type: application/vnd.kafka.v2+json" \
     --data '{"topics":["rest-proxy-topic"]}' \
     http://dataplatform:18086/consumers/cg1/instances/ci1/subscription | jq .
```

3. Consume data using the base URL in the first reponse. 

```bash
curl --silent -X GET \
     -H "Accept: application/vnd.kafka.json.v2+json" \
     http://dataplatform:18086/consumers/cg1/instances/ci1/records | jq .
```

4. Close the consumer with a DELETE to make it leave the group and clean up its resources

```bash
curl --silent -X DELETE \
     -H "Content-Type: application/vnd.kafka.v2+json" \
     http://dataplatform:18086/consumers/cg1/instances/ci1 | jq .
```

## Working with Avro data

### Create a new topic

To create a topic, use the topics endpoint POST `/clusters/{cluster_id}/topics` as shown below.

```bash
curl --silent -X POST -H "Content-Type: application/json" \
     --data '{"topic_name": "rest-proxy-topic-avro", "replication_factor":"3", "partitions_count": "8"}' \
     http://dataplatform:18086/v3/clusters/${CLUSTER_ID}/topics | jq
```

### Register the Avro Schema

Navigate to http://dataplatform:28102 and register the following Avro schema under subject `rest-proxy-topic-avro-value`:

```json
{
  "type": "record",
  "name": "Customer",
  "namespace": "com.trivadis.kafkaws.sample",
  "doc": "This is a sample Avro schema to get you started. Please edit",
  "fields": [
    {
      "name": "id",
      "type": "long"
    },
    {
      "name": "customerCode",
      "type": "long"
    },
    {
      "name": "telephone",
      "type": "string"
    },
    {
      "name": "email",
      "type": "string"
    },
    {
      "name": "language",
      "type": "string"
    }
  ]
}
```

Set the variable `SCHEMA_ID` to the value of the schema ID of the schema registry

```bash
export SCHEMA_ID=$(curl -X GET "http://dataplatform:8081/subjects/rest-proxy-topic-avro-value/versions/latest" | jq '.id')
```

### Produce Avro

Produce three Avro messages to the topic `rest-proxy-topic-avro`

```bash
curl --silent -X POST \
     -H "Content-Type: application/vnd.kafka.avro.v2+json" \
     -H "Accept: application/vnd.kafka.v2+json" \
     --data '{"value_schema_id": '"$SCHEMA_ID"', "records": [{"value": {
        "id": 1,
        "customerCode": 1,
        "telephone": "888582158",
        "email": "supplier1@test.com",
        "language": "EN"        
      } }]}' \
     "http://dataplatform:18086/topics/rest-proxy-topic-avro" | jq .
```

Result:

```
{
  "offsets": [
    {
      "partition": 1,
      "offset": 0,
      "error_code": null,
      "error": null
    }
  ],
  "key_schema_id": null,
  "value_schema_id": 3
}
```

### Consume Avro

1. Create a consumer `ci2` belonging to consumer group `cg2`. Specify `auto.offset.reset` to be earliest so it starts at the beginning of the topic.

```bash
curl --silent -X POST \
     -H "Content-Type: application/vnd.kafka.v2+json" \
     --data '{"name": "ci2", "format": "avro", "auto.offset.reset": "earliest"}' \
     http://dataplatform:18086/consumers/cg2 | jq .
```

Result: 

```
{
  "instance_id": "ci2",
  "base_uri": "http://kafka-rest-1:8086/consumers/cg2/instances/ci2"
}
```

2. Subscribe the consumer to topic `rest-proxy-topic-avro`

```bash
curl --silent -X POST \
     -H "Content-Type: application/vnd.kafka.v2+json" \
     --data '{"topics":["rest-proxy-topic-avro"]}' \
     http://dataplatform:18086/consumers/cg2/instances/ci2/subscription | jq .
```

3. Consume data using the base URL in the first reponse. 

```bash
curl --silent -X GET \
     -H "Accept: application/vnd.kafka.avro.v2+json" \
     http://dataplatform:18086/consumers/cg2/instances/ci2/records | jq .
```

4. Close the consumer with a DELETE to make it leave the group and clean up its resources

```bash
curl --silent -X DELETE \
     -H "Content-Type: application/vnd.kafka.v2+json" \
     http://dataplatform:18086/consumers/cg2/instances/ci2 | jq .
```
