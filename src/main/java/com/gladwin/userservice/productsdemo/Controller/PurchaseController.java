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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@CrossOrigin(origins = "*")
@RequestMapping("/api/gladwin")
public class PurchaseController {

    @Autowired
    private PurchaseRepository purchaseRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @GetMapping("/cod-success")
    public String codSuccess() {
        return "cod-success";
    }

    @PostMapping("/cash-on-delivery")
    @Transactional
    public String cashOnDelivery(Authentication authentication) {

        String username = authentication.getName();

        AppUser user = appUserRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("Logged-in user not found"));

        List<CartItem> cartItems = cartItemRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        // Validate every item before changing stock or saving the order.
        for (CartItem item : cartItems) {
            Product product = item.getProduct();

            if (item.getQuantity() <= 0 ||
                    item.getQuantity() > product.getStock()) {
                return "redirect:/cart?error=stock";
            }
        }

        // Create one order for the complete checkout.
        Order order = new Order();
        order.setCustomer(user);

        double orderTotal = 0;

        for (CartItem item : cartItems) {
            orderTotal += item.getProduct().getPrice()
                    * item.getQuantity();
        }

        order.setTotalAmount(orderTotal);
        order.setPaymentMethod("COD");
        order.setPaymentStatus("PENDING");
        order.setOrderStatus("PLACED");
        order.setPhoneNumber(user.getPhoneNumber());
        order.setAddressLine(user.getAddressLine());
        order.setCity(user.getCity());
        order.setState(user.getState());
        order.setPostalCode(user.getPostalCode());
        order.setOrderDate(LocalDateTime.now());

        orderRepository.save(order);

        // Save every product as an order item and preserve Purchase records.
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

        return "redirect:/api/gladwin/cod-success";
    }
}