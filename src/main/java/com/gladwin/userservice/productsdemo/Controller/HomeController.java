package com.gladwin.userservice.productsdemo.Controller;

import com.gladwin.userservice.productsdemo.Model.Category;
import com.gladwin.userservice.productsdemo.Model.Product;
import com.gladwin.userservice.productsdemo.Repository.CategoryRepository;
import com.gladwin.userservice.productsdemo.Repository.ProductRepository;
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
    public String home(Model model) {

        List<Product> products = productRepository.findAll();

        List<Category> categories = categoryRepository.findAll();

        List<Category> mainCategories = categories.stream()
                .filter(category -> category.getParent() == null)
                .collect(Collectors.toList());

        model.addAttribute("allProductsList", products);
        model.addAttribute("mainCategories", mainCategories);

        return "home";
    }
}