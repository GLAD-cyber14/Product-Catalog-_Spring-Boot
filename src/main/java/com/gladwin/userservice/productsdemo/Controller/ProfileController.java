package com.gladwin.userservice.productsdemo.Controller;

import com.gladwin.userservice.productsdemo.Model.AppUser;
import com.gladwin.userservice.productsdemo.Repository.AppUserRepository;
import com.gladwin.userservice.productsdemo.service.CloudinaryService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final AppUserRepository appUserRepository;
    private final CloudinaryService cloudinaryService;

    public ProfileController(
            AppUserRepository appUserRepository,
            CloudinaryService cloudinaryService) {
        this.appUserRepository = appUserRepository;
        this.cloudinaryService = cloudinaryService;
    }

    // Display the logged-in user's profile
    @GetMapping
    public String showProfile(
            Authentication authentication,
            Model model) {

        AppUser user = appUserRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        model.addAttribute("user", user);

        return "profile";
    }

    // Update the logged-in user's profile
    @PostMapping("/update")
    public String updateProfile(
            Authentication authentication,

            @RequestParam("firstName")
            String firstName,

            @RequestParam("lastName")
            String lastName,

            @RequestParam(value = "profileImage",
                    required = false)
            MultipartFile profileImage,

            RedirectAttributes redirectAttributes)
            throws IOException {

        AppUser user = appUserRepository
                .findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        user.setFirstName(firstName);
        user.setLastName(lastName);

        // Upload a new profile picture if selected
        if (profileImage != null && !profileImage.isEmpty()) {

            if (profileImage.getContentType() == null
                    || !profileImage.getContentType()
                    .startsWith("image/")) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Please select a valid image.");

                return "redirect:/profile";
            }

            String imageUrl = cloudinaryService.uploadProfileImage(profileImage);

            user.setProfileImage(imageUrl);
        }

        appUserRepository.save(user);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Profile updated successfully!");

        return "redirect:/profile";
    }
}

