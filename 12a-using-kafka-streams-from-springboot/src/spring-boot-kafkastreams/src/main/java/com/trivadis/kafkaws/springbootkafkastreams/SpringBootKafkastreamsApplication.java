package com.trivadis.kafkaws.springbootkafkastreams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@SpringBootApplication
@EnableKafkaStreams
public class SpringBootKafkastreamsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootKafkastreamsApplication.class, args);
	}

}
