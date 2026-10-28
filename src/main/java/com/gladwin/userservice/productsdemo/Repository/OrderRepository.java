package com.gladwin.userservice.productsdemo.Repository;

import com.gladwin.userservice.productsdemo.Model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Paginated query for all customer orders
    Page<Order> findByCustomerIdOrderByOrderDateDesc(Long customerId, Pageable pageable);

    // Paginated query filtered by date after a specific timestamp
    Page<Order> findByCustomerIdAndOrderDateAfterOrderByOrderDateDesc(Long customerId, LocalDateTime startDate, Pageable pageable);

    boolean existsByStripeSessionId(String stripeSessionId);

    java.util.Optional<Order> findByStripeSessionId(String stripeSessionId);
}