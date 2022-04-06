package com.trivadis.kafkaws.springbootkafkaconsumer;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.ContainerStoppingErrorHandler;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@SpringBootApplication
public class SpringBootKafkaConsumerApplication {
	public static void main(String[] args) {
		SpringApplication.run(SpringBootKafkaConsumerApplication.class, args);
	}

	@Value(value = "${topic.retryable-topic-name}")
	public String retryableTopicName;

	@Value(value = "${topic.stopping-topic-name}")
	public String stoppingTopicName;

	@Bean
	TaskScheduler scheduler() {
		return new ThreadPoolTaskScheduler();
	}

	@Bean
	public NewTopic retryableTopic() {
		return (TopicBuilder.name(retryableTopicName).partitions(8).replicas(1).build());
	}

	@Bean
	public NewTopic stoppingTopic() {
		return (TopicBuilder.name(stoppingTopicName).partitions(8).replicas(1).build());
	}
}


