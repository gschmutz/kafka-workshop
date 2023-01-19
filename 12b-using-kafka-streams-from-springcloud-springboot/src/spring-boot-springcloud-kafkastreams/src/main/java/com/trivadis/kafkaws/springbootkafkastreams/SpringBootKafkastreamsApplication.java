package com.trivadis.kafkaws.springbootkafkastreams;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.StreamsBuilderFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SpringBootKafkastreamsApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringBootKafkastreamsApplication.class, args);
	}

	@Bean
	public StreamsBuilderFactoryBeanCustomizer streamsBuilderFactoryBeanCustomizer() {
		return sfb -> sfb.setStateListener((newState, oldState) -> {
			System.out.println("New State : " + newState);
		});
	}

}
