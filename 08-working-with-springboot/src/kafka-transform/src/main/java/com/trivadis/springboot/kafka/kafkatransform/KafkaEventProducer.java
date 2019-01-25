package com.trivadis.springboot.kafka.kafkatransform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventProducer {
	private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventProducer.class);
	
	@Autowired
	private KafkaTemplate<Long, String> kafkaTemplate;
	
	@Value("${kafka.topic.out}")
	String kafkaTopic;
	
	public void produce(Long key, String value) {
		kafkaTemplate.send(kafkaTopic, key, value);
	}
}
