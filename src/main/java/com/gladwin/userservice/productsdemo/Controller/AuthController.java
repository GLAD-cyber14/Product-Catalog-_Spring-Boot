package com.gladwin.userservice.productsdemo.Controller;

import com.gladwin.userservice.productsdemo.Model.AppUser;
import com.gladwin.userservice.productsdemo.Repository.AppUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register";
    }

    @GetMapping("/login")
    public String showLoginPage() {
        return "login";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute AppUser appUser, Model model) {

        // Check whether the username already exists
        if (appUserRepository.findByUsername(appUser.getUsername()) != null) {
            model.addAttribute("error",
                    "Username already exists. Please choose another username.");
            return "register";
        }

        // Check whether the email is provided
        if (appUser.getEmail() == null || appUser.getEmail().trim().isEmpty()) {
            model.addAttribute("error", "Please enter your email address.");
            return "register";
        }

        // Normalize the email address
        appUser.setEmail(appUser.getEmail().trim().toLowerCase());

        // Check whether the email is already registered (red message on the form)
        if (appUserRepository.existsByEmailIgnoreCase(appUser.getEmail())) {
            model.addAttribute("error",
                    "Duplicate email address. This email is already registered.");
            return "register";
        }

        // Encrypt the password
        appUser.setPassword(passwordEncoder.encode(appUser.getPassword()));

        // Assign the normal user role
        appUser.setRole("USER");

        // Save the user. If two sign-ups with the same email/username arrive at the
        // same instant, the database unique key still rejects the second one: show
        // the red message instead of the 500 error page.
        try {
            appUserRepository.save(appUser);
        } catch (DataIntegrityViolationException e) {
            model.addAttribute("error",
                    "Duplicate email address or username. Please use different details.");
            return "register";
        }

        // ?success makes login.html show the green "Registration successful" box
        return "redirect:/login?success";
    }
}