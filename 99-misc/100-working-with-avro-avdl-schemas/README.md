# How to use Avro avdl to define Schemas to use with Kafka

## Create the configuration in Maven

In the project where you want to use the Avro AVDL language to create te schemas, add the following plugin repository to `pluginRepositories` in the `pom.xml`:

```xml
    <pluginRepositories>
        <pluginRepository>
            <id>Trivadis Maven Repo</id>
            <url>https://github.com/TrivadisPF/avdl2avsc-maven-plugin/raw/mvn-repo/</url>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>always</updatePolicy>
            </snapshots>
        </pluginRepository>
    </pluginRepositories>
```

add the `avro-maven-plugin` and `avdl2avsc-maven-plugin` to the `plugins` section in the `pom.xml`

```xml
            <plugin>
                <groupId>org.apache.avro</groupId>
                <artifactId>avro-maven-plugin</artifactId>
                <version>${avro.version}</version>
                <executions>
                    <execution>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>schema</goal>
                            <goal>protocol</goal>
                            <goal>idl-protocol</goal>
                        </goals>
                        <configuration>
                            <stringType>String</stringType>
                            <fieldVisibility>private</fieldVisibility>
                            <sourceDirectory>${project.basedir}/src/main/schema</sourceDirectory>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            
            <plugin>
                <groupId>com.trivadis.plugins</groupId>
                <artifactId>avdl2avsc-maven-plugin</artifactId>
                <version>1.0.1-SNAPSHOT</version>
                <executions>
                    <execution>
                        <phase>generate-sources</phase>
                        <goals>
                            <goal>genschema</goal>
                        </goals>
                        <configuration>
                            <inputAvdlDirectory>${basedir}/src/main/schema/avdl</inputAvdlDirectory>
                            <outputSchemaDirectory>${basedir}/target/generated-sources/avro/schema
                            </outputSchemaDirectory>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
```   

add the Avro dependency to the `pom.xml` 

```xml
        <dependency>
            <groupId>org.apache.avro</groupId>
            <artifactId>avro</artifactId>
            <version>${avro.version}</version>
        </dependency>
```

and add the property to define the Avro version to use
 
```xml
     <properties>
        ...
        <avro.version>1.12.0</avro.version>
    </properties>
```         
 
Create the folder `src/main/schema/avdl` where the AVDL files will be stored. 

## Create the schema in AVDL

Create a file `src/main/schema/avdl/customer/Customer.avdl` and add

```avdl
@namespace("com.acme.customer.avro")
protocol CustomerProtocol {

	record Address {
		long id;
		string street;
		string postalCode;
		string city;
		string gln;
	}

	record Customer {
		string number;
		string firstName;
		string lastName;
		Address address;
	}
}
```

create a file `src/main/schema/avdl/customer/CustomerState-v1.avdl` and add

```
@namespace("com.acme.customer.avro")
protocol CustomerStateProtocol {
	import idl "Customer.avdl";

	record CustomerState {
		Customer customer;
	}
}
```

Now let's run the convert from AVDL to AVSC and the generation of the Java classes.

```bash
mvn clean package
```

You should find the generated artefacts in the `target/generated-sources/avro` folder.
 
## Register schemas to Schema registry
 
### Using Maven
 
Add the confluent registry to the plugin repositories in the `pom.xml`
 
```xml
         <pluginRepository>
            <id>confluent</id>
            <url>https://packages.confluent.io/maven/</url>
        </pluginRepository>
```       
 
Add `kafka-schema-registry-maven-plugin` to the `pom.xml`
 
```xml
             <plugin>
                <groupId>io.confluent</groupId>
                <artifactId>kafka-schema-registry-maven-plugin</artifactId>
                <version>${confluent.version}</version>
                <configuration>
                    <schemaRegistryUrls>
                        <param>http://${env.DATAPLATFORM_IP}:8081</param>
                    </schemaRegistryUrls>
                    <subjects>
                        <customer.state.v1-value>target/generated-sources/avro/schema/customer/CustomerState.avsc</customer.state.v1-value>
                    </subjects>
                </configuration>
                <goals>
                    <goal>register</goal>
                    <goal>test-compatibility</goal>
                </goals>
            </plugin>
```

Adapt the above plugin definition to specify the schemas to register in the `<subjects>` section.

 
```xml
     <properties>
        ...
        <confluent.version>8.0.0</confluent.version>
    </properties>
```         

Make sure the set the environment variable `DATAPLATFORM_IP` to the ip address of the node which hosts the Confluent schema registry. 

Now run the register goal

```
mvn schema-registry:register
```

and the schema should get registered as you can see in the log

```
...
[INFO] 
[INFO] ----------------------< org.example:avdl-schema >-----------------------
[INFO] Building avdl-schema 1.0-SNAPSHOT
[INFO]   from pom.xml
[INFO] --------------------------------[ jar ]---------------------------------
[INFO] 
[INFO] --- schema-registry:8.0.0:register (default-cli) @ avdl-schema ---
[INFO] Registered subject(customer.state.v1-value) with id 2 version 1
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  1.609 s
[INFO] Finished at: 2025-08-30T19:27:08+02:00
[INFO] ------------------------------------------------------------------------
guido.schmutz@AMAXDKFVW0HYY ~/D/G/g/k/9/1/s/avdl-schema (master)> 
```

### Using Jikkou

[Jikkou](https://www.jikkou.io/) is a powerful, flexible open-source framework that enables self-serve resource provisioning. It allows developers and DevOps teams to easily manage, automate, and provision all the resources needed for their Apache Kafka platform.

First let's create a folder `jikkou` in the project root, where the jikkou definitions will be stored. 

Create a file named `./jikkou/config` and add

```
{
  "currentContext" : "dataplatform",
  "dataplatform" : {
    "configFile" : null,
    "configProps" : {
    }
  }
}
```

Create a file named `./jikkou/application.conf` and add

```
jikkou {
  # The paths from which to load extensions
  extension.paths = [${?JIKKOU_EXTENSION_PATH}]

  # Configure Jikkou Proxy Mode
  # proxy {
  #  url = "http://localhost:8080"
  # }

  # Kafka Extension
  kafka {
    # The default Kafka Client configuration
    client {
      bootstrap.servers = "localhost:9092"
      bootstrap.servers = ${?JIKKOU_DEFAULT_KAFKA_BOOTSTRAP_SERVERS}
      security.protocol = "PLAINTEXT"
      security.protocol = ${?JIKKOU_DEFAULT_KAFKA_SECURITY_PROTOCOL}
      sasl.mechanism = "PLAIN"
      sasl.mechanism = ${?JIKKOU_DEFAULT_KAFKA_SASL_MECHANISM}
      sasl.jaas.config = ${?JIKKOU_DEFAULT_KAFKA_SASL_JAAS_CONFIG}
    }
    brokers {
      # If 'True'
      waitForEnabled = true
      waitForEnabled = ${?JIKKOU_KAFKA_BROKERS_WAIT_FOR_ENABLED}
      # The minimal number of brokers that should be alive for the CLI stops waiting.
      waitForMinAvailable = 1
      waitForMinAvailable = ${?JIKKOU_KAFKA_BROKERS_WAIT_FOR_MIN_AVAILABLE}
      # The amount of time to wait before verifying that brokers are available.
      waitForRetryBackoffMs = 1000
      waitForRetryBackoffMs = ${?JIKKOU_KAFKA_BROKERS_WAIT_FOR_RETRY_BACKOFF_MS}
      # Wait until brokers are available or this timeout is reached.
      waitForTimeoutMs = 60000
      waitForTimeoutMs = ${?JIKKOU_KAFKA_BROKERS_WAIT_FOR_TIMEOUT_MS}
    }
  }

  schemaRegistry {
    url = "http://localhost:8081"
    url = ${?JIKKOU_DEFAULT_SCHEMA_REGISTRY_URL}
  }

  # The default custom transformations to apply on any resources.
  transformations = []

  # The default custom validations to apply on any resources.
  validations = [
    {
      name = "topicMustHaveValidName"
      type = io.streamthoughts.jikkou.kafka.validation.TopicNameRegexValidation
      priority = 100
      config = {
        topicNameRegex = "[a-zA-Z0-9\\._\\-]+"
        topicNameRegex = ${?VALIDATION_DEFAULT_TOPIC_NAME_REGEX}
      }
    },
    {
      name = "topicMustHaveParitionsEqualsOrGreaterThanOne"
      type = io.streamthoughts.jikkou.kafka.validation.TopicMinNumPartitionsValidation
      priority = 100
      config = {
        topicMinNumPartitions = 1
        topicMinNumPartitions = ${?VALIDATION_DEFAULT_TOPIC_MIN_NUM_PARTITIONS}
      }
    },
    {
      name = "topicMustHaveReplicasEqualsOrGreaterThanOne"
      type = io.streamthoughts.jikkou.kafka.validation.TopicMinReplicationFactorValidation
      priority = 100
      config = {
        topicMinReplicationFactor = 1
        topicMinReplicationFactor = ${?VALIDATION_DEFAULT_TOPIC_MIN_REPLICATION_FACTOR}
      }
    }
  ]
  
  # The default custom reporters to report applied changes.
  reporters = [
    # Uncomment following lines to enable default kafka reporter
        {
         name = "kafka-reporter"
          type = io.streamthoughts.jikkou.kafka.reporter.KafkaChangeReporter
          config = {
            event.source = "jikkou/cli"
            kafka = {
              topic.creation.enabled = true
              topic.creation.defaultReplicationFactor = 1
              topic.name = "jikkou-resource-change-event"
              client = ${jikkou.kafka.client} {
                client.id = "jikkou-reporter-producer"
              }
            }
          }
        }
  ]
}
```

Create a file `./jikkou/schema-specs.yaml` and add a metadata section for each schema to register. Here we only have one schema:

```yaml
---
apiVersion: "schemaregistry.jikkou.io/v1beta2"
kind: "SchemaRegistrySubjectList"
metadata: {}
items:
  - metadata:
      name: "customer.state.v1-value"
      labels: { }
      annotations:
        schemaregistry.jikkou.io/normalize-schema: true
    spec:
      schemaRegistry:
        vendor: Confluent
      compatibilityLevel: "FULL_TRANSITIVE"
      schemaType: "AVRO"
      schema:
        $ref: /target/generated-sources/avro/schema/customer/state/CustomerState.avsc
```

Now we can run `jikkou` using the Docker image (make sure that you set the `DATAPLATFORM_IP` environment variable first).

In a terminal window, navigate into the `jikkou` folder and run

```bash
docker run -it --rm \
  --net host \
  -v $(pwd)/../target:/target \
  -v $(pwd):/app/yaml \
  -v $(pwd)/application.conf:/appuser/.jikkou/application.conf \
  -v $(pwd)/config:/etc/jikkou/config \
  -e JIKKOU_DEFAULT_KAFKA_BOOTSTRAP_SERVERS=$DATAPLATFORM_IP:9092 \
  -e JIKKOU_DEFAULT_SCHEMA_REGISTRY_URL=http://$DATAPLATFORM_IP:8081 \
  streamthoughts/jikkou:main \
  apply --files /app/yaml
```

and the schema should get registered successfully as shown in the log

```
guido.schmutz@AMAXDKFVW0HYY ~/D/G/g/k/9/1/s/a/jikkou (master)> docker run -it --rm \
                                                                     --net host \
                                                                     -v $(pwd)/../target:/target \
                                                                     -v $(pwd):/app/yaml \
                                                                     -v $(pwd)/application.conf:/appuser/.jikkou/application.conf \
                                                                     -v $(pwd)/config:/etc/jikkou/config \
                                                                     -e JIKKOU_DEFAULT_KAFKA_BOOTSTRAP_SERVERS=$DATAPLATFORM_IP:9092 \
                                                                     -e JIKKOU_DEFAULT_SCHEMA_REGISTRY_URL=http://$DATAPLATFORM_IP:8081 \
                                                                     streamthoughts/jikkou:main \
                                                                     apply --files /app/yaml
TASK [CREATE] Create subject 'customer.state.v1-value' (type=CREATE, compatibilityLevel=FULL_TRANSITIVE) - CHANGED 
{
  "end" : "2025-08-30T17:33:35.604173Z",
  "status" : "CHANGED",
  "description" : "Create subject 'customer.state.v1-value' (type=CREATE, compatibilityLevel=FULL_TRANSITIVE)",
  "change" : {
    "apiVersion" : "schemaregistry.jikkou.io/v1beta2",
    "kind" : "SchemaRegistrySubjectChange",
    "metadata" : {
      "name" : "customer.state.v1-value",
      "labels" : { },
      "annotations" : {
        "schemaregistry.jikkou.io/normalize-schema" : true,
        "jikkou.io/items-count" : 1
      }
    },
    "spec" : {
      "changes" : [ {
        "name" : "schema",
        "op" : "CREATE",
        "after" : "{\"fields\":[{\"name\":\"customer\",\"type\":{\"fields\":[{\"name\":\"number\",\"type\":\"string\"},{\"name\":\"firstName\",\"type\":\"string\"},{\"name\":\"lastName\",\"type\":\"string\"},{\"name\":\"address\",\"type\":{\"fields\":[{\"name\":\"id\",\"type\":\"long\"},{\"name\":\"street\",\"type\":\"string\"},{\"name\":\"postalCode\",\"type\":\"string\"},{\"name\":\"city\",\"type\":\"string\"},{\"name\":\"gln\",\"type\":\"string\"}],\"name\":\"Address\",\"type\":\"record\"}}],\"name\":\"Customer\",\"type\":\"record\"}}],\"name\":\"CustomerState\",\"namespace\":\"com.acme.customer.avro\",\"type\":\"record\"}"
      }, {
        "name" : "schemaType",
        "op" : "CREATE",
        "after" : "AVRO"
      }, {
        "name" : "references",
        "op" : "CREATE",
        "after" : [ ]
      }, {
        "name" : "compatibilityLevel",
        "op" : "CREATE",
        "after" : "FULL_TRANSITIVE"
      } ],
      "op" : "CREATE",
      "data" : {
        "normalizeSchema" : true,
        "permanentDelete" : false
      }
    }
  },
  "changed" : true,
  "failed" : false
}
EXECUTION in 815ms 
ok : 0, created : 1, altered : 0, deleted : 0 failed : 0
```

You can also create the Kafka topics using Jikkou. For that create a new file `jikkou/topics-specs.yaml` folder (the file name can be anything, as long as the extension is yaml)

```yaml
apiVersion: "kafka.jikkou.io/v1beta2"
kind: "KafkaTopicList"
metadata: {}
items:
  - metadata:
      name: 'customer.state.v1'
    spec:
      partitions: 8
      replicas: 3
      configs:
        cleanup.policy: compact
```

No rerun `jikkou` and the topic(s) will be created as well. 

## Download schema from Schema Registry

If you are not the owner of the schema but want to consume from a topic where messages are schema-based, then you can download the schemas to your project from the schema registry using the `kafka-schema-registry-maven-plugin`.

Let's simulate it by registering a schema in the schema registry from another domain

```bash
cat > iso-country.avsc << 'EOF'
{
  "type": "record",
  "name": "CountryCodeStateEvent",
  "namespace": "com.acme.reference.country",
  "doc": "Event representing the state of an ISO Country Code entry",
  "fields": [
    {
      "name": "country_code_alpha2",
      "type": "string",
      "doc": "ISO 3166-1 alpha-2 country code, e.g., 'US'"
    },
    {
      "name": "country_code_alpha3",
      "type": "string",
      "doc": "ISO 3166-1 alpha-3 country code, e.g., 'USA'"
    },
    {
      "name": "country_numeric",
      "type": "string",
      "doc": "ISO 3166-1 numeric code, zero-padded, e.g., '840'"
    },
    {
      "name": "country_name",
      "type": "string",
      "doc": "Official short English name of the country"
    }
  ]
}
EOF
```

```bash
curl -X POST http://localhost:8081/subjects/iso-country.state.v1-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d @<(jq -n --arg schema "$(cat iso-country.avsc | jq -c .)" '{schema: $schema}')
```

To use it, add the confluent registry to the plugin repositories in the `pom.xml`
 
```xml
         <pluginRepository>
            <id>confluent</id>
            <url>https://packages.confluent.io/maven/</url>
        </pluginRepository>
```       
 
Add `kafka-schema-registry-maven-plugin` to the `pom.xml`
 
```xml
            <plugin>
                <groupId>io.confluent</groupId>
                <artifactId>kafka-schema-registry-maven-plugin</artifactId>
                <version>8.0.0</version>
                <configuration>
                    <schemaRegistryUrls>
                        <param>http://${DATAPLATFORM_IP}:8081</param>
                    </schemaRegistryUrls>

                    <outputDirectory>${project.basedir}/src/main/schema/avro</outputDirectory>
                    <subjectPatterns>
                        <param>iso-country.state.v1-value</param>
                    </subjectPatterns>
                    <versions>
                        <param>latest</param>
                    <versions>
                </configuration>
            </plugin>
```

See next section for using it with both `register` and `download`. 

Adapt the above plugin definition to specify the schemas to register in the `<subjects>` section.

 
```xml
     <properties>
        ...
        <confluent.version>8.0.0</confluent.version>
    </properties>
```         

Make sure the set the environment variable `DATAPLATFORM_IP` to the ip address of the node which hosts the Confluent schema registry. 

Now run the register goal

```
mvn schema-registry:download
```

and run  

```bash
mvn package
```

to generate the Java classes.

### Using `kafka-schema-registry-maven-plugin` for register and download

To use the plugin for both `register` my schemas and `download` of other schemas, you can figure it as follows:

```xml
            <plugin>
                <groupId>io.confluent</groupId>
                <artifactId>kafka-schema-registry-maven-plugin</artifactId>
                <version>8.0.0</version>
                <configuration>
                    <schemaRegistryUrls>
                        <param>http://${DATAPLATFORM_IP}:8081</param>
                    </schemaRegistryUrls>
                </configuration>
                <executions>

                    <!-- ✅ Register schemas -->
                    <execution>
                        <id>register</id>
                        <goals>
                            <goal>register</goal>
                        </goals>
                        <configuration>
                            <subjects>
                                <pub.order.purchaseOrder.state.v1-value>target/generated-sources/avro/schema/purchaseorder/state/PurchaseOrderState.avsc</pub.order.purchaseOrder.state.v1-value>
                            </subjects>
                            <schemaTypes>
                                <Flights-value>AVRO</Flights-value>
                            </schemaTypes>
                        </configuration>
                    </execution>

                    <!-- ✅ Download schemas -->
                    <execution>
                        <id>download</id>
                        <goals>
                            <goal>download</goal>
                        </goals>
                        <configuration>
                            <outputDirectory>${project.basedir}/src/main/schema/avro</outputDirectory>
                            <subjectPatterns>
                                <param>pub.warehouse.orderItemsDispatched.delta.v1-value</param>
                            </subjectPatterns>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
```

To run `register` perform

```bash
mvn schema-registry:register@register
```

and to run `download` perform

```bash
mvn schema-registry:download@download
```
