# Streaming Platform in Docker
The environment for this course is completly based on docker containers. 

Either use the Virtual Machine image made available in the course, containing a docker installation or provisiong your own Docker environemnt including Docker Compose. Find the information for installing [Docker](https://docs.docker.com/install/#supported-platforms) and [Docker Compose](https://docs.docker.com/compose/install/).
 
[Here](../00-setup/README.md) you can find the installation of an Virtual Machine in Azure. 

In order to simplify the provisioning, a single docker-compose configuration is used. All the necessary software will be provisioned using Docker. 

## What is the Streaming Platform?

The following services are provisioned as part of the Streaming Platform: 

 * Apache Zookeeper
 * Kafka Broker 1-3
 * Confluent Schema Registry
 * Kafka Connect 1 -2 
 * Kafka Rest Proxy
 * KSQL Server
 * Schema registry UI
 * Kafka Connect UI
 * Zoonavigator
 * Kafka Manager
 * Streamsets Data Collector
 * Apache NiFi

For Kafka we will be using the Docker images provided by Confluent and available under the following GitHub project: <https://github.com/confluentinc/cp-docker-images>. In this project, there is an example folder with a few different docker compose configuration for various Confluent Platform configurations. 

## Prepare the Docker Compose configuration

On your Docker Machine, create a folder `streamingplatform`. 

```
mkdir streamingplatform
cd streamingplatform
```

Download the code of the [docker-compose.yml](docker-compose.yml) file from GitHub using wget or curl, i.e. 

```
wget https://raw.githubusercontent.com/gschmutz/kafka-workshop/master/01-environment/docker-compose.yml`
```

## Prepare the environment

Before we can start the environment, we have to set the environment variable `DOCKER_HOST_IP` to contain the IP-address of the Docker Host and another environment variable `PUBLIC_IP` to the public IP address (not the same if you are using Azure). 
You can find the IP-Address of the Docker host using the `ifconfig` config from the linux shell. 

![Alt Image Text](./images/get-ipaddress-dockerhost.png "Schema Registry UI")

The public IP address of the VM in Azure can be found in the Azure portal.

![Alt Image Text](./images/get-ipaddress-azure.png "Schema Registry UI")

In a terminal window, execute the follwoing two commands, setting the two addresses with the values you got in the previous steps:

```
export PUBLIC_IP=40.91.195.92
export DOCKER_HOST_IP=10.0.1.4
```

If you are not on azure, you can set the `PUBLIC_IP` environment variable to the same value as the `DOCKER_HOST_IP`.

You have to repeat that, whever you open a new terminal window and want to perform some commands in the docker-compose environment. 

## Start the environment

Now let's run all the container specified by the Docker Compose configuration. In the terminal window, where you have exported the two environment variables, perform the following command:

```
docker-compose up -d
```

The first time it will take a while, as it has to download many Docker images.

After it has started, you can get the log output of all services by executing
 
```
docker-compose logs -f
```

if you want to see the services running for our environment, perform the following command:

```
docker ps

CONTAINER ID        IMAGE                                            COMMAND                  CREATED             STATUS                           PORTS                                          NAMES
d01fb522accd        apache/nifi:1.6.0                                "/bin/sh -c ${NIFI_B…"   6 hours ago         Up 6 hours                       8443/tcp, 10000/tcp, 0.0.0.0:38080->8080/tcp   stream-processing_nifi_1
9ecf9a3ab42c        trivadisbds/streamsets-kafka-nosql               "/docker-entrypoint.…"   6 hours ago         Up 6 hours                       0.0.0.0:18630->18630/tcp                       stream-processing_streamsets_1
5e96deeff9cc        confluentinc/ksql-cli:5.0.0-beta1                "perl -e 'while(1){ …"   7 hours ago         Up 7 hours                                                                      stream-processing_ksql-cli_1
cf4893312d40        confluentinc/cp-kafka-rest:5.0.0-beta1-1         "/etc/confluent/dock…"   7 hours ago         Up 7 hours                       8082/tcp, 0.0.0.0:8084->8084/tcp               stream-processing_rest-proxy_1
a36ed06fa71f        landoop/schema-registry-ui                       "/run.sh"                7 hours ago         Up 7 hours                       0.0.0.0:8002->8000/tcp                         stream-processing_schema-registry-ui_1
382358f81cf8        confluentinc/cp-enterprise-kafka:5.0.0-beta1-1   "/etc/confluent/dock…"   7 hours ago         Up 7 hours                       9092/tcp, 0.0.0.0:9093->9093/tcp               stream-processing_broker-2_1
e813de06f7cd        confluentinc/cp-enterprise-kafka:5.0.0-beta1-1   "/etc/confluent/dock…"   7 hours ago         Up 7 hours                       0.0.0.0:9092->9092/tcp                         stream-processing_broker-1_1
c2210b42db6e        confluentinc/cp-enterprise-kafka:5.0.0-beta1-1   "/etc/confluent/dock…"   7 hours ago         Up 7 hours                       9092/tcp, 0.0.0.0:9094->9094/tcp               stream-processing_broker-3_1
db2033adfd2a        trivadisbds/kafka-manager                        "./km.sh"                7 hours ago         Restarting (255) 7 seconds ago                                                  stream-processing_kafka-manager_1
676e4f734b09        confluentinc/cp-zookeeper:5.0.0-beta1-1          "/etc/confluent/dock…"   7 hours ago         Up 7 hours                       2888/tcp, 0.0.0.0:2181->2181/tcp, 3888/tcp     stream-processing_zookeeper_1
```

### Add inbound rules for the follwing ports
If you are using a VM on Azure, make sure to add inbound rules for the following ports:

Service | Url
------- | -------------
StreamSets Data Collector | 18630
Apache NiFi | 28080
Schema Registry UI  | 8002
Schema Registry Rest API  | 8081
Kafka Connect UI  | 8003
Kafka Manager  | 39000
Kafdrop  | 9020
Confluent Control Center | 9021


### Add entry to local /etc/hosts File
To simplify working with the Streaming Platform, add the following entry to your local `/etc/hosts` file. 

```
40.91.195.92	streamingplatform
```

## Services accessible on Streaming Platform
The following service are available as part of the platform:

Type | Service | Url
------|------- | -------------
Development | StreamSets Data Collector | <http://streamingplatform:18630>
Development | Apache NiFi | <http://streamingplatform:28080/nifi>
Governance | Schema Registry UI  | <http://streamingplatform:8002>
Governance | Schema Registry Rest API  | <http://streamingplatform:8081>
Management | Kafka Connect UI | <http://streamingplatform:8003>
Management | Kafka Manager  | <http://streamingplatform:39000>
Management | Kafdrop  | <http://streamingplatform:9020>
Management | Zoonavigator  | <http://streamingplatform:8010>
Management | Confluent Control Center | <http://streamingplatform:9021>


## Stop the environment
To stop the environment, execute the following command:

```
docker-compose stop
```

after that it can be re-started using `docker-compose start`.

To stop and remove all running container, execute the following command:

```
docker-compose down
```

