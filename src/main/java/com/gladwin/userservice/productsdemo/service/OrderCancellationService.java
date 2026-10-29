package com.gladwin.userservice.productsdemo.service;

import com.gladwin.userservice.productsdemo.Model.Order;
import com.gladwin.userservice.productsdemo.Model.OrderItem;
import com.gladwin.userservice.productsdemo.Model.Product;
import com.gladwin.userservice.productsdemo.Repository.OrderItemRepository;
import com.gladwin.userservice.productsdemo.Repository.OrderRepository;
import com.gladwin.userservice.productsdemo.Repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderCancellationService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public OrderCancellationService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public void cancelOrderAndRestoreStock(
            Long orderId, String paymentStatus, String stripeRefundId) {

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found with ID: " + orderId));

        // Idempotent check
        if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            return;
        }

        // Validate allowed states
        if (!"PLACED".equalsIgnoreCase(order.getOrderStatus())
                && !"PROCESSING".equalsIgnoreCase(order.getOrderStatus())) {
            throw new IllegalStateException("Order status '" + order.getOrderStatus() + "' cannot be cancelled.");
        }

        // Restore inventory stock
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : orderItems) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        // Update order status and payment details
        order.setOrderStatus("CANCELLED");

        if (paymentStatus != null) {
            order.setPaymentStatus(paymentStatus);
        }
        if (stripeRefundId != null) {
            order.setStripeRefundId(stripeRefundId);
        }

        orderRepository.save(order);

        // TODO: Trigger async email notification if needed
    }
}