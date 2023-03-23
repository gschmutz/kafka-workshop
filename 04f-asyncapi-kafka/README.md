# Using AsyncAPI with Kafka

In this workshop we will learn how to produce and consume messages using the [Kafka Java API](https://kafka.apache.org/documentation/#api) using Avro for serializing and deserializing messages.

```yml
asyncapi: '2.1.0'
info:
  title: Notification Service
  version: 1.0.0
  description: This service is in charge of notifying users
servers:
  dataplatform-local:
    url: dataplatform:{port}
    protocol: kafka
    variables:
      port: 
        default: '9092'
        enum: 
          - '9092'
          - '9093'
    protocolVersion: 2.8.0
    description: Kafka borker on local virtual machine
    
channels:
  notify:
    description: xxxxx
    publish:
      bindings:
        kafka:
          groupId: my-group
      message:
        schemaFormat: 'application/vnd.apache.avro;version=1.9.0'
        payload:
#          $ref: 'http://dataplatform:8081/subjects/test-java-avro-topic-value/versions/1/schema'     
          type: record
          doc: User information
          fields:
            - name: id
              type: int
            - name: message
              type: string
```


```
docker run --rm -it \
-v ${PWD}/kafka.yml:/app/asyncapi.yml \
-v ${PWD}/output:/app/output \
asyncapi/generator -o /app/output /app/asyncapi.yml @asyncapi/java-spring-template --force-write
```


	
