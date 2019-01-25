package com.trivadis.springboot.kafka.kafkatransform;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class KafkaEventConsumer {
	private static final Logger LOGGER = LoggerFactory.getLogger(KafkaEventConsumer.class);
	
	@Autowired 
	private KafkaEventProducer kafkaEventProducer;
	
	@KafkaListener(topics = "${kafka.topic.in}")
	public void receive(ConsumerRecord<Long, String> consumerRecord) {
		String value = consumerRecord.value();
		Long key = consumerRecord.key();
		LOGGER.info("received key = '{}' with payload='{}'", key, value);
		
		// convert to upper
		String valueUpper = value.toUpperCase();
		kafkaEventProducer.produce(key, valueUpper);
	}

}