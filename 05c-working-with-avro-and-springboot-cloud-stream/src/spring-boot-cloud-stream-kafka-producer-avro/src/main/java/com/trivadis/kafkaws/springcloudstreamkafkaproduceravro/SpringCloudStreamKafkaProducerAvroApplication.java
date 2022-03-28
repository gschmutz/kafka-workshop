package com.trivadis.kafkaws.springcloudstreamkafkaproduceravro;

import com.trivadis.kafkaws.avro.v1.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Processor;

import java.time.Instant;

@SpringBootApplication
@EnableBinding(Processor.class)
public class SpringCloudStreamKafkaProducerAvroApplication implements CommandLineRunner {

	private static Logger LOG = LoggerFactory.getLogger(SpringCloudStreamKafkaProducerAvroApplication.class);

	@Autowired
	private KafkaEventProducer kafkaEventProducer;

	public static void main(String[] args) {
		SpringApplication.run(SpringCloudStreamKafkaProducerAvroApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		LOG.info("EXECUTING : command line runner");

		if (args.length == 0) {
			runProducer(100, 10, 0);
		} else {
			runProducer(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Long.parseLong(args[2]));
		}

	}

	private void runProducer(int sendMessageCount, int waitMsInBetween, long id) throws Exception {
		Long key = (id > 0) ? id : null;

		for (int index = 0; index < sendMessageCount; index++) {
			Notification notification = Notification.newBuilder()
														.setId(id)
														.setMessage("[" + id + "] Hello Kafka " + index)
														.setCreatedAt(Instant.now()).build();

			kafkaEventProducer.produce(index, key, notification);

			// Simulate slow processing
			Thread.sleep(waitMsInBetween);

		}
	}

}
