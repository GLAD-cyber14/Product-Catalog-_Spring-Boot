package com.gladwin.userservice.productsdemo.Controller;

import com.gladwin.userservice.productsdemo.Model.Category;
import com.gladwin.userservice.productsdemo.Repository.CategoryRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/category")
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }


    // =====================================================
    // VIEW ALL CATEGORIES
    // =====================================================

    @GetMapping
    public String showCategoryPage(Model model) {

        model.addAttribute(
                "categories",
                categoryRepository.findAll()
        );

        // Main categories for dropdown
        model.addAttribute(
                "mainCategories",
                categoryRepository.findByParentIsNull()
        );

        return "category";
    }


    // =====================================================
    // SHOW ADD CATEGORY FORM
    // =====================================================

    @GetMapping("/create")
    public String showCreateCategoryPage(Model model) {

        model.addAttribute("category", new Category());

        // Main categories available as parents
        model.addAttribute(
                "mainCategories",
                categoryRepository.findByParentIsNull()
        );

        return "category-form";
    }


    // =====================================================
    // SAVE CATEGORY
    // =====================================================

    @PostMapping("/save")
    public String saveCategory(
            @ModelAttribute Category category) {

        categoryRepository.save(category);

        return "redirect:/admin/category";
    }


    // =====================================================
    // SHOW EDIT CATEGORY FORM
    // =====================================================

    @GetMapping("/edit/{id}")
    public String showEditCategoryPage(
            @PathVariable("id") int id,
            Model model) {

        Category category =
                categoryRepository.findById(id)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Category not found"
                                )
                        );

        model.addAttribute("category", category);

        // Main categories available as parents
        model.addAttribute(
                "mainCategories",
                categoryRepository.findByParentIsNull()
        );

        return "category-form";
    }


    // =====================================================
    // DELETE CATEGORY
    // =====================================================

    @GetMapping("/delete/{id}")
    public String deleteCategory(
            @PathVariable("id") int id) {

        categoryRepository.deleteById(id);

        return "redirect:/admin/category";
    }
}