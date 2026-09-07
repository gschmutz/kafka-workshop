package com.trivadis.kafkaws.orders;

import com.trivadis.kafkaws.entity.Order;
import com.trivadis.kafkaws.entity.Order.OrderStatus;
import com.trivadis.kafkaws.orders.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer orderEventProducer;

    /**
     * Both the Order insert and the Outbox insert happen inside this single transaction.
     * If either fails the whole transaction rolls back — no phantom messages, no lost events.
     */
    @Transactional
    public String placeOrder(String customerId, double amount, String currency) {
        String orderId = UUID.randomUUID().toString();

        Order order = Order.builder()
            .id(orderId)
            .customerId(customerId)
            .amount(amount)
            .currency(currency)
            .status(OrderStatus.PENDING)
            .build();

        orderRepository.saveAndFlush(order);

        // produce event to Outbox
        orderEventProducer.send(order);

        return orderId;
    }
}
