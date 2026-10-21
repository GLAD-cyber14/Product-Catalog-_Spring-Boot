package com.gladwin.userservice.productsdemo.Repository;

import com.gladwin.userservice.productsdemo.Model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByCustomerIdOrderByOrderDateDesc(Long customerId);

    boolean existsByStripeSessionId(String stripeSessionId);

    // Find orders associated with a Stripe Checkout Session.
    java.util.Optional<Order> findByStripeSessionId(String stripeSessionId);
}
