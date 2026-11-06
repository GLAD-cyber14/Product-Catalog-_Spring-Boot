package com.gladwin.userservice.productsdemo.Controller;

import com.gladwin.userservice.productsdemo.Model.AppUser;
import com.gladwin.userservice.productsdemo.Repository.AppUserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.gladwin.userservice.productsdemo.service.CloudinaryService;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.IOException;
import java.util.List;
import java.util.Locale;


@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;
    private final Cloudinary cloudinary; // Injected for deleting old assets


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public AdminUserController(AppUserRepository appUserRepository,
                               PasswordEncoder passwordEncoder,
                               CloudinaryService cloudinaryService,
                               Cloudinary cloudinary) {

        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.cloudinaryService = cloudinaryService;
        this.cloudinary = cloudinary;
    }


    // =========================================================
    // HELPER: SANITIZE FOLDER NAMES TO PREVENT DUPLICATES
    // =========================================================
    private String sanitizeFolderName(String name) {
        if (name == null || name.trim().isEmpty()) return "general";
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    }


    // =========================================================
    // HELPER: DELETE OLD CLOUDINARY IMAGE
    // =========================================================
    private void deleteOldCloudinaryImage(String imageUrl) {
        if (imageUrl != null && imageUrl.contains("cloudinary.com")) {
            try {
                String pathPart = imageUrl.substring(imageUrl.indexOf("/upload/") + 8);
                if (pathPart.matches("^v\\d+/.*")) {
                    pathPart = pathPart.replaceFirst("^v\\d+/", "");
                }
                int lastDot = pathPart.lastIndexOf('.');
                if (lastDot != -1) {
                    pathPart = pathPart.substring(0, lastDot);
                }
                cloudinary.uploader().destroy(pathPart, ObjectUtils.emptyMap());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    // =========================================================
    // VIEW USERS
    // =========================================================

    @GetMapping
    public String viewUsersAdmins(Model model) {
        List<AppUser> allusers =
                appUserRepository.findAll();
        model.addAttribute("users", allusers);
        return "admin-users";
    }



    // =========================================================
    // SHOW CREATE USER PAGE
    // =========================================================

    @GetMapping("/create")
    public String showCreateUserPage() {

        return "admin-create-user";
    }


    // =========================================================
    // CREATE USER
    // =========================================================

    @PostMapping("/create")
    public String createUser(

            @RequestParam("firstName")
            String firstName,

            @RequestParam("lastName")
            String lastName,

            @RequestParam("username")
            String username,

            @RequestParam("email")
            String email,

            @RequestParam("password")
            String password,

            @RequestParam("role")
            String role,

            @RequestParam(value = "profileImage",
                    required = false)
            MultipartFile profileImage,
            RedirectAttributes redirectAttributes

    ) throws IOException {
        // Check if email already exists
        if (appUserRepository.findByEmail(email).isPresent()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "⚠️ Duplicate Email: The email '" + email + "' is already registered."
            );
            return "redirect:/admin/users/create";
        }


        AppUser user = new AppUser();

        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);

        // Encrypt password
        user.setPassword(
                passwordEncoder.encode(password)
        );

        // USER or ADMIN
        user.setRole(role);


        // =====================================================
        // PROFILE IMAGE
        // =====================================================

        if (profileImage != null && !profileImage.isEmpty()) {

            String imageUrl = cloudinaryService.uploadImage(
                    profileImage,
                    sanitizeFolderName("profiles"),
                    sanitizeFolderName("users")
            );

            user.setProfileImage(imageUrl);
        }


        appUserRepository.save(user);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "User created successfully."
        );

        return "redirect:/admin/users";
    }


    // =========================================================
    // SHOW EDIT USER PAGE
    // =========================================================

    @GetMapping("/edit/{id}")
    public String showEditUserPage(

            @PathVariable Long id,
            Model model,
            RedirectAttributes redirectAttributes

    ) {

        AppUser user =
                appUserRepository.findById(id).orElse(null);


        if (user == null) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "User not found."
            );

            return "redirect:/admin/users";
        }


        model.addAttribute("user", user);

        return "admin-edit-user";
    }


    // =========================================================
    // UPDATE USER
    // =========================================================

    @PostMapping("/edit/{id}")
    public String updateUser(

            @PathVariable Long id,

            @RequestParam("firstName")
            String firstName,

            @RequestParam("lastName")
            String lastName,
            @RequestParam("username")
            String username,

            @RequestParam("email")
            String email,

            @RequestParam("role")
            String role,

            @RequestParam(value = "password",
                    required = false)
            String password,

            @RequestParam(value = "profileImage",
                    required = false)
            MultipartFile profileImage,

            RedirectAttributes redirectAttributes

    ) throws IOException {


        AppUser user =
                appUserRepository.findById(id).orElse(null);


        if (user == null) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "User not found."
            );

            return "redirect:/admin/users";
        }


        // =====================================================
        // UPDATE BASIC INFORMATION
        // =====================================================
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setEmail(email);

        // Prevent an existing ADMIN from being changed to USER
        if ("ADMIN".equalsIgnoreCase(user.getRole())
                && "USER".equalsIgnoreCase(role)) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "An ADMIN account cannot be changed to USER."
            );

            return "redirect:/admin/users";
        }

        // Only the username "admin" is protected from role changes
        if (!"admin".equalsIgnoreCase(user.getUsername())) {
            user.setRole(role);
        }

        // =====================================================
        // UPDATE PASSWORD ONLY IF ENTERED
        // =====================================================

        if (password != null &&
                !password.trim().isEmpty()) {

            user.setPassword(
                    passwordEncoder.encode(password)
            );
        }


        // =====================================================
        // UPDATE PROFILE IMAGE
        // =====================================================

        if (profileImage != null && !profileImage.isEmpty()) {

            // Delete old profile image from Cloudinary first
            if (user.getProfileImage() != null) {
                deleteOldCloudinaryImage(user.getProfileImage());
            }

            String imageUrl = cloudinaryService.uploadImage(
                    profileImage,
                    sanitizeFolderName("profiles"),
                    sanitizeFolderName("users")
            );

            user.setProfileImage(imageUrl);
        }


        appUserRepository.save(user);


        redirectAttributes.addFlashAttribute(
                "successMessage",
                "User updated successfully."
        );

        return "redirect:/admin/users";
    }


    // =========================================================
    // DELETE USER
    // =========================================================

    @GetMapping("/delete/{id}")
    public String deleteUser(

            @PathVariable Long id,
            RedirectAttributes redirectAttributes

    ) {

        AppUser user =
                appUserRepository.findById(id).orElse(null);


        if (user == null) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "User not found."
            );

            return "redirect:/admin/users";
        }


        // Delete database record
        appUserRepository.deleteById(id);


        redirectAttributes.addFlashAttribute(
                "successMessage",
                "User deleted successfully."
        );


        return "redirect:/admin/users";
    }

}