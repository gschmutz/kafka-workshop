package com.trivadis.kafkaws.springbootkafkaconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;

@SpringBootApplication
@EnableBinding(Processor.class)
public class SpringBootKafkaConsumerApplication {

	/*
	@Autowired
	private ConsumerFactory consumerFactory;

	@Bean
	public ConcurrentKafkaListenerContainerFactory<Long, String> filterContainerFactory() {
		ConcurrentKafkaListenerContainerFactory<Long, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
		factory.setConsumerFactory(consumerFactory);
		factory.setRecordFilterStrategy(
				record -> !record.value().contains("Kafka 5"));
		return factory;
	}
	*/

	public static void main(String[] args) {
		SpringApplication.run(SpringBootKafkaConsumerApplication.class, args);
	}

}
