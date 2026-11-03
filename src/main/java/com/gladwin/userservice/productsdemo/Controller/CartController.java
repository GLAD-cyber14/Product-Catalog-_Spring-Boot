package com.gladwin.userservice.productsdemo.Controller;

import com.gladwin.userservice.productsdemo.Model.AppUser;
import com.gladwin.userservice.productsdemo.Model.CartItem;
import com.gladwin.userservice.productsdemo.Model.Product;
import com.gladwin.userservice.productsdemo.Repository.CartItemRepository;
import com.gladwin.userservice.productsdemo.Repository.ProductRepository;
import com.gladwin.userservice.productsdemo.Repository.AppUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // ------------------------------------------------------------------
    // AJAX (fetch) endpoints used by the Home / Products pages.
    // They work by PRODUCT id and ALWAYS answer with the full cart
    // snapshot, so the browser can redraw straight from the server.
    //   { success, items: { "productId": quantity, ... }, totalItems }
    // ------------------------------------------------------------------

    // Current cart of the logged-in user (called when a page loads)
    @GetMapping("/state")
    @ResponseBody
    public ResponseEntity<?> cartState(Authentication authentication) {
        AppUser user = getLoggedInUser(authentication);
        return ResponseEntity.ok(cartSnapshot(user));
    }

    // Add one unit of a product (creates the row only if it doesn't exist yet)
    @PostMapping("/add/{productId}")
    @ResponseBody
    public ResponseEntity<?> addToCart(
            @PathVariable Long productId,
            Authentication authentication) {

        AppUser user = getLoggedInUser(authentication);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.getStock() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "outofstock"));
        }

        boolean limitReached = false;

        CartItem cartItem = cartItemRepository
                .findByUserAndProductId(user, productId)
                .orElse(null);

        if (cartItem != null) {
            // Row already exists: only the number changes
            if (cartItem.getQuantity() < product.getStock()) {
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                cartItemRepository.save(cartItem);
            } else {
                limitReached = true;
            }
        } else {
            try {
                cartItemRepository.save(new CartItem(user, product, 1));
            } catch (DataIntegrityViolationException e) {
                // Two requests inserted the same (user, product) at the same moment.
                // The unique constraint rejected the second one, so bump the existing row.
                cartItemRepository.findByUserAndProductId(user, productId)
                        .ifPresent(existing -> {
                            if (existing.getQuantity() < product.getStock()) {
                                existing.setQuantity(existing.getQuantity() + 1);
                                cartItemRepository.save(existing);
                            }
                        });
            }
        }

        Map<String, Object> snapshot = cartSnapshot(user);
        if (limitReached) {
            snapshot.put("limitReached", true);
        }
        return ResponseEntity.ok(snapshot);
    }

    // Remove one unit of a product (deletes the row when it reaches 0)
    @PostMapping("/decrease-product/{productId}")
    @ResponseBody
    public ResponseEntity<?> decreaseProduct(
            @PathVariable Long productId,
            Authentication authentication) {

        AppUser user = getLoggedInUser(authentication);

        cartItemRepository.findByUserAndProductId(user, productId).ifPresent(item -> {
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
                cartItemRepository.save(item);
            } else {
                cartItemRepository.delete(item);
            }
        });

        return ResponseEntity.ok(cartSnapshot(user));
    }

    // ------------------------------------------------------------------
    // Form endpoints used by the "My Cart" page.
    // They work by CART ITEM id and redirect back to /cart.
    // ------------------------------------------------------------------

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

    // The user's whole cart as { productId -> quantity } plus the unit total
    private Map<String, Object> cartSnapshot(AppUser user) {
        Map<String, Integer> items = new HashMap<>();
        int total = 0;

        for (CartItem ci : cartItemRepository.findByUser(user)) {
            items.merge(String.valueOf(ci.getProduct().getId()), ci.getQuantity(), Integer::sum);
            total += ci.getQuantity();
        }

        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("success", true);
        snapshot.put("items", items);
        snapshot.put("totalItems", total);
        return snapshot;
    }

    // Find the currently logged-in user
    private AppUser getLoggedInUser(Authentication authentication) {
        String username = authentication.getName();
        AppUser user = appUserRepository.findByUsername(username);

        if (user == null) {
            throw new RuntimeException("Logged-in user not found");
        }
        return user;
    }
}