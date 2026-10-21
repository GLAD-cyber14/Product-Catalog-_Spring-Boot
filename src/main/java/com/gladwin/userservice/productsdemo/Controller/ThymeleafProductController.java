package com.gladwin.userservice.productsdemo.Controller;
import com.gladwin.userservice.productsdemo.service.ProductService;
import org.springframework.ui.Model;
import com.gladwin.userservice.productsdemo.Model.Category;
import com.gladwin.userservice.productsdemo.Model.Product;
import com.gladwin.userservice.productsdemo.Repository.CategoryRepository;
import com.gladwin.userservice.productsdemo.Repository.ProductRepository;
import com.gladwin.userservice.productsdemo.service.CloudinaryService;
import com.gladwin.userservice.productsdemo.Repository.PurchaseRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/products")
public class ThymeleafProductController {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    private PurchaseRepository purchaseRepository;


    @Value("${app.upload.dir}")
    private String uploadDir;


    // ============================================================
    // SHOW PRODUCTS
    // ============================================================

    @GetMapping
    public String viewDashboard(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(defaultValue = "id") String sortField,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String search,
            Model model) {

        // --------------------------------------------------------
        // 1. GET ALL CATEGORIES
        // --------------------------------------------------------

        List<Category> categories = categoryRepository.findAll();


        // --------------------------------------------------------
        // 2. GET MAIN CATEGORIES
        // Main category = category without a parent
        // --------------------------------------------------------

        List<Category> mainCategories = categories.stream()
                .filter(category -> category.getParent() == null)
                .collect(Collectors.toList());


        Category selectedCategory = null;
        Category selectedMainCategory = null;


        // --------------------------------------------------------
        // 3. FIND SELECTED CATEGORY
        // --------------------------------------------------------

        if (categoryId != null) {

            selectedCategory = categories.stream()
                    .filter(category ->
                            category.getCategoryid() == categoryId)
                    .findFirst()
                    .orElse(null);


            if (selectedCategory != null) {

                // If selected category is a SUBCATEGORY
                if (selectedCategory.getParent() != null) {

                    selectedMainCategory =
                            selectedCategory.getParent();

                }

                // If selected category is a MAIN CATEGORY
                else {

                    selectedMainCategory =
                            selectedCategory;
                }
            }
        }


        // --------------------------------------------------------
        // 4. GET ALL PRODUCTS FROM DATABASE
        // --------------------------------------------------------

        List<Product> allProducts =
                productService.getAllProducts();

        // --------------------------------------------------------
        // 5. START WITH ALL PRODUCTS
        //
        // IMPORTANT:
        // When /products is opened without categoryId,
        // ALL PRODUCTS are shown.
        // --------------------------------------------------------

        List<Product> filteredProducts = allProducts;


        // --------------------------------------------------------
        // 6. FILTER PRODUCTS BY CATEGORY AND/OR SEARCH
        // --------------------------------------------------------

        if (selectedCategory != null) {


            // ----------------------------------------------------
            // SUBCATEGORY SELECTED
            // ----------------------------------------------------

            if (selectedCategory.getParent() != null) {

                int selectedId =
                        selectedCategory.getCategoryid();

                filteredProducts = allProducts.stream()
                        .filter(product ->
                                product.getCategoryid() == selectedId)
                        .collect(Collectors.toList());
            }


            // ----------------------------------------------------
            // MAIN CATEGORY SELECTED
            // ----------------------------------------------------

            else {

                int mainId =
                        selectedCategory.getCategoryid();


                // Find children of the main category
                List<Integer> childIds = categories.stream()
                        .filter(category ->
                                category.getParent() != null
                                        &&
                                        category.getParent()
                                                .getCategoryid() == mainId)
                        .map(Category::getCategoryid)
                        .collect(Collectors.toList());


                // Include:
                // 1. Products assigned directly to main category
                // 2. Products assigned to its subcategories

                filteredProducts = allProducts.stream()
                        .filter(product ->
                                product.getCategoryid() == mainId
                                        ||
                                        childIds.contains(
                                                product.getCategoryid()))
                        .collect(Collectors.toList());
            }
        }


        // --------------------------------------------------------
        // SEARCH
        // A search such as "laptop" should show the laptop section
        // and products belonging to that category, even when the
        // individual product name does not contain the word laptop.
        // --------------------------------------------------------

        if (search != null && !search.trim().isEmpty()) {

            String searchText = search.trim().toLowerCase(Locale.ROOT);

            List<Category> matchingCategories = categories.stream()
                    .filter(category ->
                            category.getCategoryname() != null
                                    && category.getCategoryname()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(searchText))
                    .collect(Collectors.toList());

            // If the search matches a category, category search takes
            // priority over product-name search.
            // For a MAIN category, include products assigned directly
            // to that category and to all of its immediate subcategories.
            List<Integer> matchingCategoryIds = matchingCategories.stream()
                    .flatMap(category -> {
                        if (category.getParent() == null) {
                            int mainId = category.getCategoryid();

                            return categories.stream()
                                    .filter(child ->
                                            child.getCategoryid() == mainId
                                                    || (child.getParent() != null
                                                    && child.getParent()
                                                    .getCategoryid() == mainId))
                                    .map(Category::getCategoryid);
                        }

                        return java.util.stream.Stream.of(
                                category.getCategoryid());
                    })
                    .distinct()
                    .collect(Collectors.toList());

            if (!matchingCategoryIds.isEmpty()) {

                filteredProducts = filteredProducts.stream()
                        .filter(product ->
                                matchingCategoryIds.contains(
                                        product.getCategoryid()))
                        .collect(Collectors.toList());

            } else {

                // No category matched, so search by product name.
                // Partial and case-insensitive searches are supported.
                filteredProducts = filteredProducts.stream()
                        .filter(product ->
                                product.getName() != null
                                        && product.getName()
                                        .toLowerCase(Locale.ROOT)
                                        .contains(searchText))
                        .collect(Collectors.toList());
            }

            // When the search itself selected a category, show that
            // category's navigation section on the page.
            if (categoryId == null && !matchingCategories.isEmpty()) {

                Category matchedCategory = matchingCategories.get(0);
                selectedCategory = matchedCategory;

                selectedMainCategory =
                        matchedCategory.getParent() != null
                                ? matchedCategory.getParent()
                                : matchedCategory;
            }
        }

        // --------------------------------------------------------
        // 7. SORT PRODUCTS
        // --------------------------------------------------------

        if ("name".equalsIgnoreCase(sortField)) {

            if ("asc".equalsIgnoreCase(sortDir)) {

                filteredProducts.sort(
                        (a, b) ->
                                a.getName()
                                        .compareToIgnoreCase(
                                                b.getName())
                );

            } else {

                filteredProducts.sort(
                        (a, b) ->
                                b.getName()
                                        .compareToIgnoreCase(
                                                a.getName())
                );
            }

        } else {

            if ("asc".equalsIgnoreCase(sortDir)) {

                filteredProducts.sort(
                        (a, b) ->
                                Integer.compare(
                                        a.getId(),
                                        b.getId())
                );

            } else {

                filteredProducts.sort(
                        (a, b) ->
                                Integer.compare(
                                        b.getId(),
                                        a.getId())
                );
            }
        }


        // --------------------------------------------------------
        // 8. SEPARATE OUT-OF-STOCK PRODUCTS
        // --------------------------------------------------------

        List<Product> outOfStockProducts =
                filteredProducts.stream()
                        .filter(product ->
                                product.getStock() <= 0)
                        .collect(Collectors.toList());


        // --------------------------------------------------------
        // 9. SEPARATE AVAILABLE PRODUCTS
        // --------------------------------------------------------

        List<Product> availableProducts =
                filteredProducts.stream()
                        .filter(product ->
                                product.getStock() > 0)
                        .collect(Collectors.toList());


        // --------------------------------------------------------
        // 10. PAGINATION
        // Pagination applies only to available products.
        // Out-of-stock products remain visible separately.
        // --------------------------------------------------------

        int totalProducts =
                availableProducts.size();

        int totalPages =
                (int) Math.ceil(
                        (double) totalProducts / size);


        if (totalPages == 0) {
            totalPages = 1;
        }


        // Prevent invalid page numbers

        if (page < 0) {
            page = 0;
        }

        if (page >= totalPages) {
            page = totalPages - 1;
        }


        int start =
                page * size;

        int end =
                Math.min(
                        start + size,
                        totalProducts);


        List<Product> paginatedProducts;


        if (start < totalProducts) {

            paginatedProducts =
                    availableProducts.subList(
                            start,
                            end);

        } else {

            paginatedProducts =
                    List.of();
        }


        // --------------------------------------------------------
        // 11. SEND DATA TO THYMELEAF
        // --------------------------------------------------------

        model.addAttribute(
                "categories",
                categories);

        model.addAttribute(
                "mainCategories",
                mainCategories);


        model.addAttribute(
                "selectedCategoryId",
                categoryId);

        model.addAttribute(
                "selectedCategory",
                selectedCategory);

        model.addAttribute(
                "selectedMainCategory",
                selectedMainCategory);

        model.addAttribute(
                "search",
                search);


        // ALL FILTERED PRODUCTS
        model.addAttribute(
                "allProductsList",
                filteredProducts);


        // AVAILABLE PRODUCTS FOR CURRENT PAGE
        model.addAttribute(
                "availableProducts",
                paginatedProducts);


        // OUT-OF-STOCK PRODUCTS
        model.addAttribute(
                "outOfStockProducts",
                outOfStockProducts);


        // Pagination

        model.addAttribute(
                "currentPage",
                page);

        model.addAttribute(
                "totalPages",
                totalPages);

        model.addAttribute(
                "totalItems",
                totalProducts);


        // Sorting

        model.addAttribute(
                "sortField",
                sortField);

        model.addAttribute(
                "sortDir",
                sortDir);


        model.addAttribute(
                "reverseSortDir",
                sortDir.equalsIgnoreCase("asc")
                        ? "desc"
                        : "asc");


        // --------------------------------------------------------
        // 12. PRODUCT FORM
        // --------------------------------------------------------

        if (!model.containsAttribute("productForm")) {

            model.addAttribute(
                    "productForm",
                    new Product());
        }


        // --------------------------------------------------------
        // 13. RETURN INDEX.HTML
        // --------------------------------------------------------

        return "index";
    }


    // ============================================================
    // SAVE / UPDATE PRODUCT
    // ============================================================

    @PostMapping("/save")
    public String saveOrUpdateProduct(
            @ModelAttribute("productForm") Product product,
            @RequestParam("imageFile") MultipartFile imageFile,
            RedirectAttributes redirectAttributes) {

        boolean isUpdate = product.getId() != 0;

        try {

            // =====================================================
            // EDITING EXISTING PRODUCT
            // =====================================================
            if (isUpdate) {

                Product existingProduct = productRepository.findById(product.getId())
                        .orElseThrow(() ->
                                new RuntimeException("Product not found"));

                // If admin selected a NEW image
                // If admin selected a NEW image
                if (imageFile != null && !imageFile.isEmpty()) {

                    // Find the selected category
                    Category selectedCategory = categoryRepository
                            .findById(product.getCategoryid())
                            .orElseThrow(() -> new RuntimeException("Category not found"));

                    // Determine main category and subcategory
                    String mainCategory;
                    String subCategory;

                    if (selectedCategory.getParent() != null) {
                        // Selected category is a subcategory
                        mainCategory = selectedCategory.getParent().getCategoryname();
                        subCategory = selectedCategory.getCategoryname();
                    } else {
                        // Selected category itself is a main category
                        mainCategory = selectedCategory.getCategoryname();
                        subCategory = "General";
                    }

                    // Upload image to Cloudinary
                    String imageUrl = cloudinaryService.uploadImage(
                            imageFile,
                            mainCategory,
                            subCategory
                    );

                    // Store Cloudinary URL in database
                    product.setImage(imageUrl);

                } else {

                    // No new image selected → keep existing image
                    product.setImage(existingProduct.getImage());
                }

            }

            // =====================================================
            // ADDING NEW PRODUCT
            // =====================================================
            // ADDING NEW PRODUCT
            else {
                if (imageFile != null && !imageFile.isEmpty()) {

                    // Find the selected category
                    Category selectedCategory = categoryRepository
                            .findById(product.getCategoryid())
                            .orElseThrow(() -> new RuntimeException("Category not found"));

                    // Determine main category and subcategory
                    String mainCategory;
                    String subCategory;

                    if (selectedCategory.getParent() != null) {
                        // Selected category is a subcategory
                        mainCategory = selectedCategory.getParent().getCategoryname();
                        subCategory = selectedCategory.getCategoryname();
                    } else {
                        // Selected category itself is a main category
                        mainCategory = selectedCategory.getCategoryname();
                        subCategory = "General";
                    }

                    // Upload image to Cloudinary
                    String imageUrl = cloudinaryService.uploadImage(
                            imageFile,
                            mainCategory,
                            subCategory
                    );

                    // Store Cloudinary URL in database
                    product.setImage(imageUrl);
                }
            }

            // Save product
            productService.saveProduct(product);

        } catch (IOException e) {

            e.printStackTrace();

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Image upload failed!"
            );

            return "redirect:/products";
        }

        String message =
                isUpdate
                        ? "Product updated successfully!"
                        : "Product created successfully!";

        redirectAttributes.addFlashAttribute(
                "successMessage",
                message
        );

        return "redirect:/products";
    }

    // ============================================================
    // EDIT PRODUCT
    // ============================================================

    @GetMapping("/edit/{id}")
    public String initiateEditMode(
            @PathVariable("id") int id,
            Model model) {

        Product targetProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        model.addAttribute("productForm", targetProduct);

        // Open products page with that product's category
        return viewDashboard(
                0,
                8,
                "id",
                "asc",
                targetProduct.getCategoryid(),
                null,
                model);
    }


    // ============================================================
    // DELETE PRODUCT
    // ============================================================

    @GetMapping("/delete/{id}")
    public String removeProductRow(
            @PathVariable int id,
            RedirectAttributes redirectAttributes) {

        // Check whether this product has any purchase history
        boolean hasPurchases = purchaseRepository.existsByProductId(id);

        if (hasPurchases) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This product cannot be deleted because it has purchase history."
            );

            return "redirect:/products";
        }

        // Safe to delete if there are no purchases
        productService.deleteProduct(id);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Product deleted successfully."
        );

        return "redirect:/products";
    }
}