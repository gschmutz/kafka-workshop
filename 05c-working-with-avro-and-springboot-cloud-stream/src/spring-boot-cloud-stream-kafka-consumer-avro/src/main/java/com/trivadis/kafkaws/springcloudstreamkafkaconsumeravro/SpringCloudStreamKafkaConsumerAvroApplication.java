package com.trivadis.kafkaws.springcloudstreamkafkaconsumeravro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;

@SpringBootApplication
@EnableBinding(Processor.class)
public class SpringCloudStreamKafkaConsumerAvroApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudStreamKafkaConsumerAvroApplication.class, args);
	}

}
