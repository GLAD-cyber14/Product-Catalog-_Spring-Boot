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

import java.io.IOException;
import java.util.List;


@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public AdminUserController(AppUserRepository appUserRepository,
                               PasswordEncoder passwordEncoder,
                               CloudinaryService cloudinaryService) {

        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.cloudinaryService = cloudinaryService;
    }


    // =========================================================
    // VIEW USERS
    // =========================================================

//    @GetMapping
//  public String viewUsers(Model model) {
//
//        List<AppUser> users =
//                appUserRepository.findByRole("USER");
//
//        model.addAttribute("users", users);
//
//        return "admin-users";
//    }

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
            MultipartFile profileImage

    ) throws IOException {


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

        // PROFILE IMAGE
        if (profileImage != null && !profileImage.isEmpty()) {

            String imageUrl = cloudinaryService.uploadImage(
                    profileImage,
                    "profiles",
                    "users"
            );

            user.setProfileImage(imageUrl);
        }


        appUserRepository.save(user);

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
        user.setRole(role);


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

            String imageUrl = cloudinaryService.uploadImage(
                    profileImage,
                    "profiles",
                    "users"
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