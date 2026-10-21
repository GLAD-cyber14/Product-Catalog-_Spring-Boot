package com.gladwin.userservice.productsdemo.Controller;

import com.gladwin.userservice.productsdemo.Model.AppUser;
import com.gladwin.userservice.productsdemo.Model.CartItem;
import com.gladwin.userservice.productsdemo.Model.Product;
import com.gladwin.userservice.productsdemo.Repository.CartItemRepository;
import com.gladwin.userservice.productsdemo.Repository.ProductRepository;
import com.gladwin.userservice.productsdemo.Repository.AppUserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final AppUserRepository appUserRepository;

    public CartController(
            CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            AppUserRepository appUserRepository) {

        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.appUserRepository = appUserRepository;
    }

    // Show the logged-in user's cart
    @GetMapping
    public String viewCart(Authentication authentication, Model model) {

        AppUser user = getLoggedInUser(authentication);

        List<CartItem> cartItems = cartItemRepository.findByUser(user);

        double cartTotal = cartItems.stream()
                .mapToDouble(item ->
                        item.getProduct().getPrice() * item.getQuantity())
                .sum();

        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartTotal", cartTotal);

        return "cart";
    }

    // Add a product to the cart
    @PostMapping("/add/{productId}")
    public String addToCart(
            @PathVariable int productId,
            Authentication authentication) {

        AppUser user = getLoggedInUser(authentication);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Don't allow adding an out-of-stock product
        if (product.getStock() <= 0) {
            return "redirect:/products?error=outofstock";
        }

        // Check if the product is already in the cart
        CartItem cartItem = cartItemRepository
                .findByUserAndProductId(user, productId)
                .orElse(null);

        if (cartItem != null) {

            // Don't allow quantity to exceed available stock
            if (cartItem.getQuantity() < product.getStock()) {
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                cartItemRepository.save(cartItem);
            }

        } else {

            CartItem newCartItem =
                    new CartItem(user, product, 1);

            cartItemRepository.save(newCartItem);
        }

        return "redirect:/products";
    }

    // Increase quantity
    @PostMapping("/increase/{id}")
    public String increaseQuantity(
            @PathVariable int id,
            Authentication authentication) {

        AppUser user = getLoggedInUser(authentication);

        CartItem cartItem = cartItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        // Security check: cart item must belong to logged-in user
        if (cartItem.getUser().getId() != user.getId()) {
            return "redirect:/cart";
        }

        Product product = cartItem.getProduct();

        if (cartItem.getQuantity() < product.getStock()) {
            cartItem.setQuantity(cartItem.getQuantity() + 1);
            cartItemRepository.save(cartItem);
        }

        return "redirect:/cart";
    }

    // Decrease quantity
    @PostMapping("/decrease/{id}")
    public String decreaseQuantity(
            @PathVariable int id,
            Authentication authentication) {

        AppUser user = getLoggedInUser(authentication);

        CartItem cartItem = cartItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (cartItem.getUser().getId() != user.getId()) {
            return "redirect:/cart";
        }

        if (cartItem.getQuantity() > 1) {

            cartItem.setQuantity(cartItem.getQuantity() - 1);
            cartItemRepository.save(cartItem);

        } else {

            cartItemRepository.delete(cartItem);
        }

        return "redirect:/cart";
    }

    // Remove product completely from cart
    @PostMapping("/remove/{id}")
    public String removeFromCart(
            @PathVariable int id,
            Authentication authentication) {

        AppUser user = getLoggedInUser(authentication);

        CartItem cartItem = cartItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cart item not found"));

        if (cartItem.getUser().getId() != user.getId()) {
            return "redirect:/cart";
        }

        cartItemRepository.delete(cartItem);

        return "redirect:/cart";
    }

    // Find the currently logged-in user
    private AppUser getLoggedInUser(Authentication authentication) {

        String username = authentication.getName();

        return appUserRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException("Logged-in user not found"));
    }
}