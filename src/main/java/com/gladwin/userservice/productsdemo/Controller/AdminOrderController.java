package com.gladwin.userservice.productsdemo.Controller;

import com.gladwin.userservice.productsdemo.Model.Order;
import com.gladwin.userservice.productsdemo.Repository.OrderRepository;
import com.gladwin.userservice.productsdemo.service.OrderSseService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

@Controller
public class AdminOrderController {

    private final OrderRepository orderRepository;
    private final OrderSseService orderSseService; // 1. Declare SSE service

    // 2. Inject OrderSseService via constructor
    public AdminOrderController(OrderRepository orderRepository, OrderSseService orderSseService) {
        this.orderRepository = orderRepository;
        this.orderSseService = orderSseService;
    }

    // Check that the logged-in user is an admin
    private void checkAdmin(Authentication authentication) {
        boolean isAdmin = authentication != null &&
                authentication.getAuthorities().stream()
                        .anyMatch(authority ->
                                authority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    // Display all customer orders
    @GetMapping("/admin/orders")
    public String viewAllOrders(
            Authentication authentication,
            Model model) {

        checkAdmin(authentication);

        List<Order> orders = orderRepository.findAll();

        model.addAttribute("orders", orders);

        return "admin-orders";
    }

    // Update an order's status
    @PostMapping("/admin/orders/update-status")
    public String updateOrderStatus(
            @RequestParam Long orderId,
            @RequestParam String status,
            Authentication authentication) {

        checkAdmin(authentication);

        List<String> allowedStatuses = Arrays.asList(
                "PLACED",
                "PROCESSING",
                "SHIPPED",
                "DELIVERED",
                "CANCELLED"
        );

        if (!allowedStatuses.contains(status)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order status"
            );
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Order not found"
                        ));


        // Lock orders that are already delivered or cancelled
        String currentStatus = order.getOrderStatus();

        if ("DELIVERED".equalsIgnoreCase(currentStatus) ||
                "CANCELLED".equalsIgnoreCase(currentStatus)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This order is locked and its status cannot be changed"
            );
        }

        // Update the status only if the order is not locked
        order.setOrderStatus(status);
        orderRepository.save(order);

        // 3. 🚀 NOTIFY THE CUSTOMER VIA SSE INSTANTLY IN REAL TIME
        if (order.getCustomer() != null) {
            orderSseService.notifyOrderStatus(
                    order.getCustomer().getId(),
                    order.getId(),
                    order.getOrderStatus(),
                    order.getPaymentStatus()
            );
        }

        return "redirect:/admin/orders";
    }

    // Mark a COD order as paid after receiving cash
    @PostMapping("/admin/orders/mark-paid")
    public String markOrderAsPaid(
            @RequestParam Long orderId,
            Authentication authentication) {

        checkAdmin(authentication);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Order not found"
                        ));

        if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cancelled orders cannot be marked as paid"
            );
        }

        if (order.getPaymentMethod() == null ||
                !order.getPaymentMethod().equalsIgnoreCase("COD")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only COD orders can be marked as paid here"
            );
        }

        if ("PAID".equalsIgnoreCase(order.getPaymentStatus())) {
            return "redirect:/admin/orders";
        }

        // Mark payment as received
        order.setPaymentStatus("PAID");
        orderRepository.save(order);

        // 🚀 BROADCAST PAYMENT STATUS UPDATE VIA SSE INSTANTLY
        if (order.getCustomer() != null) {
            orderSseService.notifyOrderStatus(
                    order.getCustomer().getId(),
                    order.getId(),
                    order.getOrderStatus(),
                    order.getPaymentStatus()
            );
        }

        return "redirect:/admin/orders";
    }
}