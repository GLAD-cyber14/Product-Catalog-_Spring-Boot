package com.gladwin.userservice.productsdemo.Controller;

import com.gladwin.userservice.productsdemo.Model.AppUser;
import com.gladwin.userservice.productsdemo.Model.Order;

import com.gladwin.userservice.productsdemo.Repository.AppUserRepository;
import com.gladwin.userservice.productsdemo.Repository.OrderRepository;
import com.gladwin.userservice.productsdemo.Repository.OrderItemRepository;

import com.gladwin.userservice.productsdemo.service.OrderSseService;
import com.gladwin.userservice.productsdemo.service.InvoicePdfService;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

import com.gladwin.userservice.productsdemo.Model.OrderItem;
import com.gladwin.userservice.productsdemo.Model.Product;
import com.gladwin.userservice.productsdemo.service.OrderCancellationService;
import com.gladwin.userservice.productsdemo.service.StripeRefundService;
import com.gladwin.userservice.productsdemo.service.EmailService;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.gladwin.userservice.productsdemo.Repository.ProductRepository;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderDetailsController {

    private final OrderCancellationService orderCancellationService;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AppUserRepository appUserRepository;
    private final ProductRepository productRepository;
    private final StripeRefundService stripeRefundService;
    private final EmailService emailService;
    private final InvoicePdfService invoicePdfService;
    private final OrderSseService orderSseService;

    public OrderDetailsController(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            AppUserRepository appUserRepository,
            ProductRepository productRepository,
            StripeRefundService stripeRefundService,
            EmailService emailService,
            OrderCancellationService orderCancellationService,
            InvoicePdfService invoicePdfService,
            OrderSseService orderSseService) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderCancellationService = orderCancellationService;
        this.appUserRepository = appUserRepository;
        this.productRepository = productRepository;
        this.stripeRefundService = stripeRefundService;
        this.emailService = emailService;
        this.invoicePdfService = invoicePdfService;
        this.orderSseService = orderSseService;
    }


    @GetMapping("/{orderId}")
    public String viewOrderDetails(
            @PathVariable Long orderId,
            Authentication authentication,
            Model model) {

        AppUser user = appUserRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

// Admin can view any order.
// Regular users can view only their own orders.
        if (!isAdmin &&
                (order.getCustomer() == null ||
                        !order.getCustomer().getId().equals(user.getId()))) {


            throw new ResponseStatusException(HttpStatus.NOT_FOUND);


        }


        List<OrderItem> orderItems =
                orderItemRepository.findByOrderId(orderId);

        model.addAttribute("order", order);
        model.addAttribute("orderItems", orderItems);

        return "order-details";
    }

    @GetMapping("/{orderId}/invoice")
    public ResponseEntity<byte[]> downloadInvoice(
            @PathVariable Long orderId,
            Authentication authentication) {

        // Find the logged-in user
        AppUser user = appUserRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Find the order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Allow customers to download only their own invoices.
        // Admins can download invoices for any order.
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin &&
                (order.getCustomer() == null ||
                        !order.getCustomer().getId().equals(user.getId()))) {

            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // Retrieve the products belonging to this order
        List<OrderItem> orderItems =
                orderItemRepository.findByOrderId(orderId);

        // Generate the PDF
        byte[] pdf = invoicePdfService.generateInvoice(order, orderItems);

        // Return the PDF as a downloadable file
        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"invoice-" + orderId + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
    @PostMapping("/{orderId}/cancel")
    public String cancelOrder(
            @PathVariable Long orderId,
            Authentication authentication) {

        AppUser user = appUserRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND));

        // Users can cancel only their own orders.
        if (!isAdmin &&
                (order.getCustomer() == null ||
                        !order.getCustomer().getId().equals(user.getId()))) {

            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        // Prevent cancelling an already cancelled order.
        if ("CANCELLED".equalsIgnoreCase(order.getOrderStatus())) {
            return "redirect:/orders/" + orderId;
        }

        // Cancellation is allowed only before shipment.
        if (!"PLACED".equalsIgnoreCase(order.getOrderStatus()) &&
                !"PROCESSING".equalsIgnoreCase(order.getOrderStatus())) {

            return "redirect:/orders/" + orderId;
        }

        // Process Stripe refunds before cancelling Stripe-paid orders.
        if ("STRIPE".equalsIgnoreCase(order.getPaymentMethod())) {

            // Do not refund an order that is already marked as refunded.
            if ("REFUNDED".equalsIgnoreCase(order.getPaymentStatus())) {
                return "redirect:/orders/" + orderId;
            }

            if (!"PAID".equalsIgnoreCase(order.getPaymentStatus())) {
                return "redirect:/orders/" + orderId
                        + "?error=payment_not_refundable";
            }

            try {
                // Request the refund from Stripe.
                Refund refund = stripeRefundService.refundOrder(order);

                if (refund == null ||
                        "failed".equalsIgnoreCase(refund.getStatus()) ||
                        "canceled".equalsIgnoreCase(refund.getStatus())) {

                    return "redirect:/orders/" + orderId
                            + "?error=refund_failed";
                }

                // Record the refund status returned by Stripe.
                if ("succeeded".equalsIgnoreCase(refund.getStatus())) {
                    order.setPaymentStatus("REFUNDED");
                } else {
                    order.setPaymentStatus("REFUND_PENDING");
                }

            } catch (StripeException | IllegalStateException ex) {

                // Do not cancel the order or restore stock if the
                // refund request fails.
                return "redirect:/orders/" + orderId
                        + "?error=refund_failed";
            }
        }

        // Restore stock only after the cancellation checks succeed.
        String paymentStatus = order.getPaymentStatus();

        try {
            orderCancellationService.cancelOrderAndRestoreStock(
                    orderId,
                    paymentStatus,
                    order.getStripeRefundId()
            );
        } catch (RuntimeException ex) {
            System.err.println(
                    "Order cancellation failed for order "
                            + orderId + ": " + ex.getMessage()
            );

            return "redirect:/orders/" + orderId
                    + "?error=cancellation_failed";
        }

        // Notify the customer that the order was cancelled.
        if (order.getCustomer() != null) {
            orderSseService.notifyOrderStatus(
                    order.getCustomer().getId(),
                    order.getId(),
                    order.getOrderStatus(),
                    order.getPaymentStatus()
            );
        }

        // Send an email only for Stripe orders whose refund was accepted.
        if ("STRIPE".equalsIgnoreCase(order.getPaymentMethod())
                && order.getPaymentStatus() != null
                && ("REFUNDED".equalsIgnoreCase(order.getPaymentStatus())
                || "REFUND_PENDING".equalsIgnoreCase(
                order.getPaymentStatus()))) {

            AppUser customer = order.getCustomer();

            if (customer != null &&
                    customer.getEmail() != null &&
                    !customer.getEmail().isBlank()) {

                try {
                    emailService.sendRefundConfirmation(
                            customer.getEmail(),
                            order.getId(),
                            order.getTotalAmount(),
                            "REFUNDED".equalsIgnoreCase(order.getPaymentStatus())
                    );
                } catch (RuntimeException ex) {
                    // An email failure must not trigger another refund.
                    System.err.println(
                            "Refund email could not be sent for order "
                                    + orderId + ": " + ex.getMessage()
                    );
                }
            }
        }

        return "redirect:/orders/" + orderId;
    }
}