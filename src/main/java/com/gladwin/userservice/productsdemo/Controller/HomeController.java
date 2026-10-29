package com.gladwin.userservice.productsdemo.Controller;

import com.gladwin.userservice.productsdemo.Model.Category;
import com.gladwin.userservice.productsdemo.Model.Product;
import com.gladwin.userservice.productsdemo.Repository.CategoryRepository;
import com.gladwin.userservice.productsdemo.Repository.ProductRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public HomeController(
            ProductRepository productRepository,
            CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping("/")
    public String home(Model model, Authentication authentication) {

        // Fetch dynamic high-sales products first
        List<Product> products = productRepository.findTopSellingProducts();

        // Fallback to all products if sales data is empty (e.g., new store setup)
        if (products == null || products.isEmpty()) {
            products = productRepository.findAll();
        }

        List<Category> categories = categoryRepository.findAll();

        List<Category> mainCategories = categories.stream()
                .filter(category -> category.getParent() == null)
                .collect(Collectors.toList());

        boolean isLoggedIn = (authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal()));

        model.addAttribute("allProductsList", products);
        model.addAttribute("mainCategories", mainCategories);
        model.addAttribute("isLoggedIn", isLoggedIn);

        return "home";
    }
}