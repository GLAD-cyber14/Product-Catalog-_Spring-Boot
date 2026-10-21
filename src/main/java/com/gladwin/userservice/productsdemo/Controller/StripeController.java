package com.gladwin.userservice.productsdemo.Controller;

import com.gladwin.userservice.productsdemo.Model.AppUser;
import com.gladwin.userservice.productsdemo.Model.CartItem;
import com.gladwin.userservice.productsdemo.Model.Order;
import com.gladwin.userservice.productsdemo.Model.OrderItem;
import com.gladwin.userservice.productsdemo.Model.Product;
import com.gladwin.userservice.productsdemo.Model.Purchase;

import com.gladwin.userservice.productsdemo.Repository.AppUserRepository;
import com.gladwin.userservice.productsdemo.Repository.CartItemRepository;
import com.gladwin.userservice.productsdemo.Repository.OrderItemRepository;
import com.gladwin.userservice.productsdemo.Repository.OrderRepository;
import com.gladwin.userservice.productsdemo.Repository.ProductRepository;
import com.gladwin.userservice.productsdemo.Repository.PurchaseRepository;
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
    private final PurchaseRepository purchaseRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public StripeController(
            StripeCheckoutService stripeCheckoutService,
            CartItemRepository cartItemRepository,
            AppUserRepository appUserRepository,
            PurchaseRepository purchaseRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository) {

        this.stripeCheckoutService = stripeCheckoutService;
        this.cartItemRepository = cartItemRepository;
        this.appUserRepository = appUserRepository;
        this.purchaseRepository = purchaseRepository;
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
                        new RuntimeException("Logged-in user not found"));

        List<CartItem> cartItems = cartItemRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        for (CartItem item : cartItems) {
            if (item.getQuantity() <= 0 ||
                    item.getQuantity() > item.getProduct().getStock()) {
                return "redirect:/cart?payment=stock_error";
            }
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
            Authentication authentication,
            @RequestParam("session_id") String sessionId)
            throws StripeException {

        AppUser user = appUserRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("Logged-in user not found"));

// Retrieve the actual session from Stripe.
        Session session =
                stripeCheckoutService.verifyCheckoutSession(sessionId);

// Confirm that Stripe has received the payment.
        if (!"paid".equals(session.getPaymentStatus())) {
            return "redirect:/cart?payment=not_paid";
        }

// Confirm that the session belongs to this customer.
        if (session.getClientReferenceId() == null ||
                !user.getId().toString().equals(session.getClientReferenceId())) {
            return "redirect:/cart?payment=invalid_session";
        }

// Do not process the same Stripe session twice.
        if (orderRepository.existsByStripeSessionId(sessionId)) {
            return "redirect:/orders";
        }

        List<CartItem> cartItems = cartItemRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

// Validate stock.
        for (CartItem item : cartItems) {
            if (item.getQuantity() <= 0 ||
                    item.getQuantity() > item.getProduct().getStock()) {
                return "redirect:/cart?payment=stock_error";
            }
        }

// Calculate the expected order total.
        double total = 0;

        for (CartItem item : cartItems) {
            total += item.getProduct().getPrice() * item.getQuantity();
        }

        long expectedAmount = Math.round(total * 100);

// Compare the expected total with the amount Stripe received.
        if (session.getAmountTotal() == null ||
                session.getAmountTotal().longValue() != expectedAmount) {
            return "redirect:/cart?payment=amount_mismatch";
        }

// Create the order.
        Order order = new Order();
        order.setCustomer(user);
        order.setTotalAmount(total);
        order.setPaymentMethod("STRIPE");
        order.setPaymentStatus("PAID");
        order.setStripeSessionId(sessionId);
        order.setOrderStatus("PLACED");
        order.setPhoneNumber(user.getPhoneNumber());
        order.setAddressLine(user.getAddressLine());
        order.setCity(user.getCity());
        order.setState(user.getState());
        order.setPostalCode(user.getPostalCode());
        order.setOrderDate(LocalDateTime.now());

        orderRepository.save(order);

// Save order items and update inventory.
        for (CartItem item : cartItems) {
            Product product = item.getProduct();

            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(item.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItemRepository.save(orderItem);

            Purchase purchase = new Purchase();
            purchase.setCustomer(user);
            purchase.setProduct(product);
            purchase.setQuantity(item.getQuantity());
            purchase.setTotalPrice(
                    product.getPrice() * item.getQuantity());
            purchase.setPurchaseDate(LocalDateTime.now());
            purchaseRepository.save(purchase);

            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }

        cartItemRepository.deleteAll(cartItems);

        return "redirect:/cod-success";


    }

}