package com.trivadis.kafkaws.orders.repository;

import com.trivadis.kafkaws.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, String> {}
