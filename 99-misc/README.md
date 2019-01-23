# Miscellaneous

### Creating Kafka Topics upon Docker startup 

```
  kafka-client:
    image: confluentinc/cp-enterprise-kafka:4.1.0
    hostname: kafka-client
    container_name: kafka-client
    depends_on:
      - kafka1
    volumes:
      - $PWD/scripts/security:/etc/kafka/secrets
    # We defined a dependency on "kafka", but `depends_on` will NOT wait for the
    # dependencies to be "ready" before starting the "kafka-client"
    # container;  it waits only until the dependencies have started.  Hence we
    # must control startup order more explicitly.
    # See https://docs.docker.com/compose/startup-order/
    command: "bash -c -a 'echo Waiting for Kafka to be ready... && \
                       /etc/confluent/docker/configure && \
                       cub kafka-ready -b kafka1:9091 1 60 --config /etc/kafka/kafka.properties && \
                       sleep 5 && \
                       kafka-topics --zookeeper zookeeper-1:2181 --topic users --create --replication-factor 2 --partitions 2 && \
                       kafka-topics --zookeeper zookeeper-1:2181 --topic wikipedia.parsed --create --replication-factor 2 --partitions 2 && \
                       kafka-topics --zookeeper zookeeper-1:2181 --topic wikipedia.failed --create --replication-factor 2 --partitions 2 && \
                       kafka-topics --zookeeper zookeeper-1:2181 --topic WIKIPEDIABOT --create --replication-factor 2 --partitions 2 && \
                       kafka-topics --zookeeper zookeeper-1:2181 --topic WIKIPEDIANOBOT --create --replication-factor 2 --partitions 2 && \
                       kafka-topics --zookeeper zookeeper-1:2181 --topic EN_WIKIPEDIA_GT_1 --create --replication-factor 2 --partitions 2 && \
                       kafka-topics --zookeeper zookeeper-1:2181 --topic EN_WIKIPEDIA_GT_1_COUNTS --create --replication-factor 2 --partitions 2'"
    environment:
      # The following settings are listed here only to satisfy the image's requirements.
      # We override the image's `command` anyways, hence this container will not start a broker.
      KAFKA_BROKER_ID: ignored
      KAFKA_ZOOKEEPER_CONNECT: ignored
      KAFKA_ADVERTISED_LISTENERS: ignored
      KAFKA_SECURITY_PROTOCOL: SASL_SSL
      KAFKA_SASL_JAAS_CONFIG: "org.apache.kafka.common.security.plain.PlainLoginModule required \
        username=\"client\" \
        password=\"client-secret\";"
      KAFKA_SASL_MECHANISM: PLAIN
      KAFKA_SSL_TRUSTSTORE_LOCATION: /etc/kafka/secrets/kafka.client.truststore.jks
      KAFKA_SSL_TRUSTSTORE_PASSWORD: confluent
      KAFKA_SSL_KEYSTORE_LOCATION: /etc/kafka/secrets/kafka.client.keystore.jks
      KAFKA_SSL_KEYSTORE_PASSWORD: confluent
      KAFKA_SSL_KEY_PASSWORD: confluent
      KAFKA_ZOOKEEPER_SET_ACL: "true"
      KAFKA_OPTS: -Djava.security.auth.login.config=/etc/kafka/secrets/broker_jaas.conf
    ports:
- "7073:7073"
```