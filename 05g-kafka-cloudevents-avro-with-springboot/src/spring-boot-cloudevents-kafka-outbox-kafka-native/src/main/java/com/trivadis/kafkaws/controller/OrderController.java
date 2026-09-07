package com.trivadis.kafkaws.orders;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<Void> placeOrder(@RequestBody PlaceOrderRequest req) {
        String orderId = orderService.placeOrder(req.customerId(), req.amount(), req.currency());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(orderId).toUri();
        return ResponseEntity.created(location).build();
    }

    public record PlaceOrderRequest(String customerId, double amount, String currency) {}
}
