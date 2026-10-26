package com.gladwin.userservice.productsdemo.Controller;

import com.gladwin.userservice.productsdemo.Model.AppUser;
import com.gladwin.userservice.productsdemo.Model.CartItem;
import com.gladwin.userservice.productsdemo.Model.Order;
import com.gladwin.userservice.productsdemo.Model.OrderItem;
import com.gladwin.userservice.productsdemo.Model.Product;

import com.gladwin.userservice.productsdemo.Repository.AppUserRepository;
import com.gladwin.userservice.productsdemo.Repository.CartItemRepository;
import com.gladwin.userservice.productsdemo.Repository.OrderRepository;
import com.gladwin.userservice.productsdemo.Repository.OrderItemRepository;
import com.gladwin.userservice.productsdemo.Repository.ProductRepository;

import com.gladwin.userservice.productsdemo.service.StripeCheckoutService;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/stripe")
public class StripeController {

    @Value("${stripe.success.url}")
    private String successUrl;

    @Value("${stripe.cancel.url}")
    private String cancelUrl;

    private final StripeCheckoutService stripeCheckoutService;
    private final CartItemRepository cartItemRepository;
    private final AppUserRepository appUserRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public StripeController(
            StripeCheckoutService stripeCheckoutService,
            CartItemRepository cartItemRepository,
            AppUserRepository appUserRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository) {

        this.stripeCheckoutService = stripeCheckoutService;
        this.cartItemRepository = cartItemRepository;
        this.appUserRepository = appUserRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @GetMapping("/checkout")
    public String checkout(Authentication authentication)
            throws StripeException {

        AppUser user = appUserRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException("User not found"));

        List<CartItem> cartItems =
                cartItemRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        String checkoutUrl =
                stripeCheckoutService.createCheckoutSession(
                        cartItems,
                        user,
                        successUrl,
                        cancelUrl
                );

        return "redirect:" + checkoutUrl;
    }

    @GetMapping("/success")
    @Transactional
    public String success(
            @RequestParam("session_id") String sessionId,
            Authentication authentication)
            throws StripeException {

        AppUser user = appUserRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException("User not found"));

        // Prevent processing the same Stripe session twice.
        Order existingOrder =
                orderRepository.findByStripeSessionId(sessionId)
                        .orElse(null);

        if (existingOrder != null) {
            if (!existingOrder.getCustomer().getId()
                    .equals(user.getId())) {
                return "redirect:/cart?payment=verification_failed";
            }

            return "redirect:/orders/" + existingOrder.getId();
        }

        // Verify the session directly with Stripe.
        Session session =
                stripeCheckoutService.verifyCheckoutSession(sessionId);

        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())
                || session.getClientReferenceId() == null
                || !session.getClientReferenceId()
                .equals(user.getId().toString())) {

            return "redirect:/cart?payment=verification_failed";
        }

        List<CartItem> cartItems =
                cartItemRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            return "redirect:/orders";
        }

        // Check all stock before changing anything.
        for (CartItem item : cartItems) {
            Product product = item.getProduct();

            if (item.getQuantity() <= 0
                    || item.getQuantity() > product.getStock()) {
                return "redirect:/cart?payment=stock_error";
            }
        }

        // Verify that the cart total matches the Stripe payment.
        long cartTotalInPaise = 0;

        for (CartItem item : cartItems) {
            cartTotalInPaise += Math.round(
                    item.getProduct().getPrice()
                            * 100 * item.getQuantity()
            );
        }

        if (session.getAmountTotal() == null
                || session.getAmountTotal() != cartTotalInPaise) {
            return "redirect:/cart?payment=verification_failed";
        }


// Create the order.
        Order order = new Order();

        order.setCustomer(user);
        order.setTotalAmount(cartTotalInPaise / 100.0);
        order.setPaymentMethod("STRIPE");
        order.setPaymentStatus("PAID");
        order.setOrderStatus("PLACED");
        order.setOrderDate(LocalDateTime.now());
        order.setStripeSessionId(sessionId);

// Copy the saved shipping address from the user.
        order.setPhoneNumber(user.getPhoneNumber());
        order.setAddressLine(user.getAddressLine());
        order.setCity(user.getCity());
        order.setState(user.getState());
        order.setPostalCode(user.getPostalCode());


        order = orderRepository.save(order);

        // Create order items and update stock.
        for (CartItem item : cartItems) {
            Product product = item.getProduct();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(product.getPrice());

            orderItemRepository.save(orderItem);

            product.setStock(
                    product.getStock() - item.getQuantity()
            );

            productRepository.save(product);
        }

        // Clear the cart only after the order has been saved.
        cartItemRepository.deleteAll(cartItems);

        return "redirect:/orders/" + order.getId();
    }

    @GetMapping("/cancel")
    public String cancel() {
        return "redirect:/cart?payment=cancelled";
    }
}