package com.trivadis.kafkaws.orders;

import com.example.orders.avro.OrderCreated;
import com.trivadis.kafkaws.events.CloudEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final CloudEventProducer cloudEventProducer;

    @Value("${app.kafka.topics.orders}")
    private String ordersTopic;

    public String placeOrder(String customerId, double amount, String currency) {
        String orderId = UUID.randomUUID().toString();

        // Build the generated Avro specific record
        OrderCreated event = OrderCreated.newBuilder()
            .setOrderId(orderId)
            .setCustomerId(customerId)
            .setAmount(amount)
            .setCurrency(currency)
            .setCreatedAt(Instant.now().toString())
            .build();

        cloudEventProducer.publish(
            ordersTopic,
            orderId,
            event,
            "com.example.orders.OrderCreated"
        );

        log.info("Order placed: {}", orderId);
        return orderId;
    }
}