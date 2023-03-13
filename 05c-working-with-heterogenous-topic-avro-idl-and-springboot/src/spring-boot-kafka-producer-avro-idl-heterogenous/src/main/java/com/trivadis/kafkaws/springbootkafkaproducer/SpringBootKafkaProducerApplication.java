package com.trivadis.kafkaws.springbootkafkaproducer;

import com.trivadis.kafkaws.avro.v1.Alert;
import com.trivadis.kafkaws.avro.v1.AlertSentEvent;
import com.trivadis.kafkaws.avro.v1.Notification;
import com.trivadis.kafkaws.avro.v1.NotificationSentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootApplication
public class SpringBootKafkaProducerApplication implements CommandLineRunner {

	private static Logger LOG = LoggerFactory.getLogger(SpringBootKafkaProducerApplication.class);

	@Autowired
	private KafkaEventProducer kafkaEventProducer;

	public static void main(String[] args) {
		SpringApplication.run(SpringBootKafkaProducerApplication.class, args);
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
			NotificationSentEvent notificationSentEvent = NotificationSentEvent.newBuilder()
					.setNotification(
							Notification.newBuilder()
									.setId(id)
									.setMessage("[" + id + "] Hello Kafka " + index)
									.setCreatedAt(Instant.now()).build()
					).build();

			kafkaEventProducer.produce(index, key, notificationSentEvent);

			// Simulate slow processing
			Thread.sleep(waitMsInBetween);
		}

		for (int index = 0; index < sendMessageCount; index++) {
			AlertSentEvent alertSentEvent = AlertSentEvent.newBuilder()
					.setAlert(
							Alert.newBuilder()
									.setId(UUID.randomUUID())
									.setMessage("Alert")
									.setSeverity("HIGH")
									.setWhen(Instant.now()).build()
					).build();

			kafkaEventProducer.produce(index, key, alertSentEvent);

			// Simulate slow processing
			Thread.sleep(waitMsInBetween);
		}
	}

}
