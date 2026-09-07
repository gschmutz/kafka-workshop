package com.trivadis.kafkaws.controller;

import com.trivadis.kafkaws.orders.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<String> placeOrder(@RequestBody PlaceOrderRequest req) {
        String orderId = orderService.placeOrder(
            req.customerId(), req.amount(), req.currency());
        return ResponseEntity.ok(orderId);
    }

    public record PlaceOrderRequest(String customerId, double amount, String currency) {}
}