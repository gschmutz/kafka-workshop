# Using AsyncAPI with Kafka

In this workshop we will learn how to produce and consume messages using the [Kafka Java API](https://kafka.apache.org/documentation/#api) using Avro for serializing and deserializing messages.

```
project/
├── asyncapi.yaml
└── schemas/
    ├── user-signup.avsc
    ├── user-updated.avsc
    └── common/
        └── address.avsc
```

`schemas/common/address.avsc`

```
{
  "type": "record",
  "name": "Address",
  "namespace": "com.example.common",
  "fields": [
    { "name": "street",  "type": "string" },
    { "name": "city",    "type": "string" },
    { "name": "country", "type": "string" },
    { "name": "zip",     "type": ["null", "string"], "default": null }
  ]
}
```


`schemas/user-signup.avsc`

```
{
  "type": "record",
  "name": "UserSignup",
  "namespace": "com.example.events.user",
  "doc": "Fired when a new user completes registration",
  "fields": [
    { "name": "userId",    "type": "string",  "doc": "UUID of the new user" },
    { "name": "email",     "type": "string" },
    { "name": "fullName",  "type": "string" },
    {
      "name": "address",
      "type": ["null", "com.example.common.Address"],
      "default": null
    },
    {
      "name": "signupTimestamp",
      "type": { "type": "long", "logicalType": "timestamp-millis" }
    },
    {
      "name": "channel",
      "type": {
        "type": "enum",
        "name": "SignupChannel",
        "symbols": ["WEB", "MOBILE", "API"]
      }
    }
  ]
}
```

`schemas/user-updated.avsc`

```
{
  "type": "record",
  "name": "UserUpdated",
  "namespace": "com.example.events.user",
  "doc": "Fired when a user profile is updated",
  "fields": [
    { "name": "userId",     "type": "string" },
    { "name": "changeType", "type": "string", "doc": "e.g. EMAIL_CHANGE, ADDRESS_CHANGE" },
    {
      "name": "updatedAt",
      "type": { "type": "long", "logicalType": "timestamp-millis" }
    },
    {
      "name": "newAddress",
      "type": ["null", "com.example.common.Address"],
      "default": null
    }
  ]
}
```


`asyncapi.yaml`

```yml
asyncapi: '3.0.0'

info:
  title: User Domain Events
  version: '1.2.0'
  description: |
    User lifecycle events published to Kafka.
    All message values are Avro-encoded using Confluent Schema Registry
    wire format (magic byte + 4-byte schema ID + payload).
  contact:
    name: Platform Team
    email: platform@example.com

# ── Servers ──────────────────────────────────────────────────────────────────
servers:

  development:
    host: localhost:9092
    protocol: kafka
    description: Local development broker
    bindings:
      kafka:
        schemaRegistryUrl: 'http://localhost:8081'
        schemaRegistryVendor: 'confluent'

  production:
    host: kafka.example.com:9092
    protocol: kafka
    description: Production Confluent Platform cluster
    security:
      - $ref: '#/components/securitySchemes/saslScram'
    bindings:
      kafka:
        schemaRegistryUrl: 'https://schema-registry.example.com'
        schemaRegistryVendor: 'confluent'

# ── Channels ─────────────────────────────────────────────────────────────────
# In 3.x channels are pure addressing — no operations here
channels:

  userSignup:
    address: user-signup-events        # the actual Kafka topic name
    description: New user registration events
    servers:
      - $ref: '#/servers/development'
      - $ref: '#/servers/production'
    bindings:
      kafka:
        topic: user-signup-events
        partitions: 6
        replicas: 3
        topicConfiguration:
          cleanup.policy: ['delete']
          retention.ms: 2592000000     # 30 days
          max.message.bytes: 1048588
        bindingVersion: '0.5.0'
    messages:
      userSignupMessage:
        $ref: '#/components/messages/UserSignupMessage'

  userUpdated:
    address: user-updated-events
    description: User profile update events
    servers:
      - $ref: '#/servers/development'
      - $ref: '#/servers/production'
    bindings:
      kafka:
        topic: user-updated-events
        partitions: 6
        replicas: 3
        topicConfiguration:
          cleanup.policy: ['delete']
          retention.ms: 2592000000
        bindingVersion: '0.5.0'
    messages:
      userUpdatedMessage:
        $ref: '#/components/messages/UserUpdatedMessage'

# ── Operations ───────────────────────────────────────────────────────────────
# 3.x: operations are top-level, linked to channels via $ref
# send   = this application sends to the channel
# receive = this application receives from the channel
operations:

  publishUserSignup:
    action: send                       # user-service sends this
    channel:
      $ref: '#/channels/userSignup'
    bindings:
      kafka:
        clientId:
          type: string
          enum: ['user-service']
        bindingVersion: '0.5.0'
    messages:
      - $ref: '#/channels/userSignup/messages/userSignupMessage'

  consumeUserSignup:
    action: receive                    # downstream consumers receive this
    channel:
      $ref: '#/channels/userSignup'
    bindings:
      kafka:
        groupId:
          type: string
          description: Consumer group ID e.g. notification-service
        bindingVersion: '0.5.0'
    messages:
      - $ref: '#/channels/userSignup/messages/userSignupMessage'

  publishUserUpdated:
    action: send
    channel:
      $ref: '#/channels/userUpdated'
    bindings:
      kafka:
        clientId:
          type: string
          enum: ['user-service']
        bindingVersion: '0.5.0'
    messages:
      - $ref: '#/channels/userUpdated/messages/userUpdatedMessage'

  consumeUserUpdated:
    action: receive
    channel:
      $ref: '#/channels/userUpdated'
    bindings:
      kafka:
        groupId:
          type: string
        bindingVersion: '0.5.0'
    messages:
      - $ref: '#/channels/userUpdated/messages/userUpdatedMessage'

# ── Components ───────────────────────────────────────────────────────────────
components:

  securitySchemes:
    saslScram:
      type: scramSha512
      description: SASL/SCRAM-SHA-512 for production cluster auth

  # ── Reusable traits ────────────────────────────────────────────────────────
  messageTraits:

    cloudEventsHeaders:
      bindings:
        kafka:
          headers:
            type: object
            required:
              - ce_specversion
              - ce_id
              - ce_source
              - ce_type
              - ce_time
            properties:
              ce_specversion:
                type: string
                enum: ['1.0']
              ce_id:
                type: string
                format: uuid
                description: Unique event ID — use UUIDv4
              ce_source:
                type: string
                format: uri
                example: /services/user-service
              ce_type:
                type: string
                description: Reverse-DNS event type
                example: com.example.user.signup
              ce_time:
                type: string
                format: date-time
              ce_datacontenttype:
                type: string
                enum: ['application/avro']

    observabilityHeaders:
      bindings:
        kafka:
          headers:
            type: object
            properties:
              correlationId:
                type: string
                format: uuid
                description: Propagated across service boundaries
              traceId:
                type: string
                description: OpenTelemetry W3C trace ID
              spanId:
                type: string
                description: OpenTelemetry span ID

  # ── Reusable messages ──────────────────────────────────────────────────────
  messages:

    UserSignupMessage:
      name: UserSignup
      title: User Signup Event
      summary: A new user has completed registration
      contentType: application/avro
      schemaFormat: 'application/vnd.apache.avro+json;version=1.9.0'
      traits:
        - $ref: '#/components/messageTraits/cloudEventsHeaders'
        - $ref: '#/components/messageTraits/observabilityHeaders'
      headers:
        type: object
        properties:
          ce_type:
            type: string
            const: 'com.example.user.signup'
          ce_source:
            type: string
            const: '/services/user-service'
      payload:
        schemaFormat: 'application/vnd.apache.avro+json;version=1.9.0'
        schema:
          $ref: './schemas/user-signup.avsc'
      bindings:
        kafka:
          key:
            type: string
            format: uuid
            description: userId — ensures partition affinity per user

    UserUpdatedMessage:
      name: UserUpdated
      title: User Updated Event
      summary: A user profile has been modified
      contentType: application/avro
      schemaFormat: 'application/vnd.apache.avro+json;version=1.9.0'
      traits:
        - $ref: '#/components/messageTraits/cloudEventsHeaders'
        - $ref: '#/components/messageTraits/observabilityHeaders'
      headers:
        type: object
        properties:
          ce_type:
            type: string
            const: 'com.example.user.updated'
          ce_source:
            type: string
            const: '/services/user-service'
      payload:
        schemaFormat: 'application/vnd.apache.avro+json;version=1.9.0'
        schema:
          $ref: './schemas/user-updated.avsc'
      bindings:
        kafka:
          key:
            type: string
            format: uuid
            description: userId
```


```
docker run --rm -it \
-v ${PWD}/kafka.yml:/app/asyncapi.yml \
-v ${PWD}/output:/app/output \
asyncapi/generator -o /app/output /app/asyncapi.yml @asyncapi/java-spring-template --force-write
```


	
