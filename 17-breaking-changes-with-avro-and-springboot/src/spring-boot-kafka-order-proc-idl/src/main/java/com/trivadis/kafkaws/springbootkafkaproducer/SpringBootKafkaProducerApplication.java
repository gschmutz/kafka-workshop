package com.trivadis.kafkaws.springbootkafkaproducer;

import com.trivadis.kafkaws.order.avro.v1.Address;
import com.trivadis.kafkaws.order.avro.v1.Order;
import com.trivadis.kafkaws.order.avro.v1.OrderStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.time.Instant;
import java.time.LocalDateTime;

import javax.xml.crypto.OctetStreamData;

@SpringBootApplication
public class SpringBootKafkaProducerApplication implements CommandLineRunner {

	private static Logger LOG = LoggerFactory.getLogger(SpringBootKafkaProducerApplication.class);

	@Autowired
	private KafkaOrderStateEventProducer kafkaOrderStateProducer;

	@Autowired
	private KafkaOrderStateEventProducerV2 kafkaOrderStateProducerV2;

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
			OrderStateEvent orderStateEvent = OrderStateEvent.newBuilder()
					.setOrder(
							Order.newBuilder()
									.setId(id)
									.setUserId(10L)
									.setFirstName("Peter")
									.setName("Muster")
									.setAddress(Address.newBuilder()
											.setStreet("Somestreet 10")
											.setCity("Somewhere")
											.setZipCode("9999")
											.build())
									.setProductId(1000L)
									.setProductName("Some Product")
									.build()
					).build();

			kafkaOrderStateProducer.produce(index, key, orderStateEvent);

			// Simulate slow processing
			Thread.sleep(waitMsInBetween);

		}
	}

	private void runProducerV2(int sendMessageCount, int waitMsInBetween, long id) throws Exception {
		Long key = (id > 0) ? id : null;

		for (int index = 0; index < sendMessageCount; index++) {
			com.trivadis.kafkaws.order.avro.v2.OrderStateEvent orderStateEvent = com.trivadis.kafkaws.order.avro.v2.OrderStateEvent.newBuilder()
					.setOrder(
						com.trivadis.kafkaws.order.avro.v2.Order.newBuilder()
									.setId(id)
									.setUserId(10L)
									.setProductId(1000L)
									.setProductName("Some Product")
									.build()
					).build();

			kafkaOrderStateProducerV2.produce(index, key, orderStateEvent);

			// Simulate slow processing
			Thread.sleep(waitMsInBetween);

		}
	}

}
