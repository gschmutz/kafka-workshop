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
                            <sourceDirectory>${project.basedir}/src/main/avdl/</sourceDirectory>
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
                            <inputAvdlDirectory>${basedir}/src/main/avdl</inputAvdlDirectory>
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
 
Create the folder `src/main/avdl` where the AVDL files will be stored. 

## Create the schema in AVDL

Create a file `src/main/avdl/customer/Customer.avdl` and add

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

create a file `src/main/avdl/customer/CustomerState-v1.avdl` and add

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

