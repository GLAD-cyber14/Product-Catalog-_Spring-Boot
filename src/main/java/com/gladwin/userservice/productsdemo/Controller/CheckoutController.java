package com.gladwin.userservice.productsdemo.Controller;

import com.gladwin.userservice.productsdemo.Model.AppUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.gladwin.userservice.productsdemo.Model.CartItem;
import com.gladwin.userservice.productsdemo.Repository.AppUserRepository;
import com.gladwin.userservice.productsdemo.Repository.CartItemRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final CartItemRepository cartItemRepository;
    private final AppUserRepository appUserRepository;

    public CheckoutController(
            CartItemRepository cartItemRepository,
            AppUserRepository appUserRepository) {

        this.cartItemRepository = cartItemRepository;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping
    public String showCheckout(
            Authentication authentication,
            Model model) {

        // Get the currently logged-in user
        AppUser user = getLoggedInUser(authentication);

        // Get this user's cart items
        List<CartItem> cartItems =
                cartItemRepository.findByUser(user);

        // If cart is empty, go back to cart page
        if (cartItems.isEmpty()) {
            return "redirect:/cart";
        }

        // Calculate total price
        double cartTotal = cartItems.stream()
                .mapToDouble(item ->
                        item.getProduct().getPrice()
                                * item.getQuantity())
                .sum();

        // Send data to checkout.html
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("cartTotal", cartTotal);
        model.addAttribute("user", user);

        return "checkout";
    }

    private AppUser getLoggedInUser(
            Authentication authentication) {

        String username = authentication.getName();

        return appUserRepository.findByUsername(username)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Logged-in user not found"));
    }
    @PostMapping("/save-address")
    public String saveAddress(
            Authentication authentication,
            @RequestParam String phoneNumber,
            @RequestParam String addressLine,
            @RequestParam String city,
            @RequestParam String state,
            @RequestParam String postalCode,
            RedirectAttributes redirectAttributes) {

        AppUser user = getLoggedInUser(authentication);

        user.setPhoneNumber(phoneNumber);
        user.setAddressLine(addressLine);
        user.setCity(city);
        user.setState(state);
        user.setPostalCode(postalCode);

        appUserRepository.save(user);

        redirectAttributes.addFlashAttribute(
                "addressSuccess", "Shipping address saved successfully!");

        return "redirect:/checkout";
    }
}