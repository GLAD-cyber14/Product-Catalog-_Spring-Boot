package com.gladwin.userservice.productsdemo.Controller;
import com.gladwin.userservice.productsdemo.service.ProductService;
import org.springframework.ui.Model;
import com.gladwin.userservice.productsdemo.Model.Category;
import com.gladwin.userservice.productsdemo.Model.Product;
import com.gladwin.userservice.productsdemo.Repository.CategoryRepository;
import com.gladwin.userservice.productsdemo.Repository.ProductRepository;
import com.gladwin.userservice.productsdemo.service.CloudinaryService;
import com.gladwin.userservice.productsdemo.Repository.PurchaseRepository;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
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
    private Cloudinary cloudinary; // Injected for deleting old assets

    @Autowired
    private PurchaseRepository purchaseRepository;


    @Value("${app.upload.dir}")
    private String uploadDir;


    // ============================================================
    // HELPER: SANITIZE FOLDER NAMES TO PREVENT DUPLICATES
    // ============================================================
    private String sanitizeFolderName(String name) {
        if (name == null || name.trim().isEmpty()) return "general";
        return name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
    }


    // ============================================================
    // HELPER: DELETE OLD CLOUDINARY IMAGE
    // ============================================================
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
        // --------------------------------------------------------

        List<Product> filteredProducts = allProducts;


        // --------------------------------------------------------
        // 6. FILTER PRODUCTS BY CATEGORY AND/OR SEARCH
        // --------------------------------------------------------

        if (selectedCategory != null) {

            if (selectedCategory.getParent() != null) {

                int selectedId =
                        selectedCategory.getCategoryid();

                filteredProducts = allProducts.stream()
                        .filter(product ->
                                product.getCategoryid() == selectedId)
                        .collect(Collectors.toList());
            }

            else {

                int mainId =
                        selectedCategory.getCategoryid();

                List<Integer> childIds = categories.stream()
                        .filter(category ->
                                category.getParent() != null
                                        &&
                                        category.getParent()
                                                .getCategoryid() == mainId)
                        .map(Category::getCategoryid)
                        .collect(Collectors.toList());

                filteredProducts = allProducts.stream()
                        .filter(product ->
                                product.getCategoryid() == mainId
                                        ||
                                        childIds.contains(
                                                product.getCategoryid()))
                        .collect(Collectors.toList());
            }
        }

        if (search != null && !search.trim().isEmpty()) {

            String searchText = search.trim().toLowerCase(Locale.ROOT);

            List<Category> matchingCategories = categories.stream()
                    .filter(category ->
                            category.getCategoryname() != null
                                    && category.getCategoryname()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(searchText))
                    .collect(Collectors.toList());

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

                filteredProducts = filteredProducts.stream()
                        .filter(product ->
                                product.getName() != null
                                        && product.getName()
                                        .toLowerCase(Locale.ROOT)
                                        .contains(searchText))
                        .collect(Collectors.toList());
            }

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
                                Long.compare(
                                        a.getId(),
                                        b.getId())
                );

            } else {

                filteredProducts.sort(
                        (a, b) ->
                                Long.compare(
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
        // --------------------------------------------------------

        int totalProducts =
                availableProducts.size();

        int totalPages =
                (int) Math.ceil(
                        (double) totalProducts / size);


        if (totalPages == 0) {
            totalPages = 1;
        }

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

        model.addAttribute("categories", categories);
        model.addAttribute("mainCategories", mainCategories);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedCategory", selectedCategory);
        model.addAttribute("selectedMainCategory", selectedMainCategory);
        model.addAttribute("search", search);
        model.addAttribute("allProductsList", filteredProducts);
        model.addAttribute("availableProducts", paginatedProducts);
        model.addAttribute("outOfStockProducts", outOfStockProducts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalProducts);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", sortDir.equalsIgnoreCase("asc") ? "desc" : "asc");

        if (!model.containsAttribute("productForm")) {
            model.addAttribute("productForm", new Product());
        }

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

        boolean isUpdate = product.getId() == null || product.getId() == 0;

        try {

            // =====================================================
            // EDITING EXISTING PRODUCT
            // =====================================================
            if (!isUpdate) {

                Product existingProduct = productRepository.findById(product.getId())
                        .orElseThrow(() ->
                                new RuntimeException("Product not found"));

                // If admin selected a NEW image
                if (imageFile != null && !imageFile.isEmpty()) {

                    // 1. Delete old image from Cloudinary first
                    if (existingProduct.getImage() != null) {
                        deleteOldCloudinaryImage(existingProduct.getImage());
                    }

                    // 2. Find and sanitize category
                    Category selectedCategory = categoryRepository
                            .findById(product.getCategoryid())
                            .orElseThrow(() -> new RuntimeException("Category not found"));

                    String mainCategory;
                    String subCategory;

                    if (selectedCategory.getParent() != null) {
                        mainCategory = sanitizeFolderName(selectedCategory.getParent().getCategoryname());
                        subCategory = sanitizeFolderName(selectedCategory.getCategoryname());
                    } else {
                        mainCategory = sanitizeFolderName(selectedCategory.getCategoryname());
                        subCategory = "general";
                    }

                    // 3. Upload new image to Cloudinary
                    String imageUrl = cloudinaryService.uploadImage(
                            imageFile,
                            mainCategory,
                            subCategory
                    );

                    product.setImage(imageUrl);

                } else {
                    // No new image selected → keep existing image
                    product.setImage(existingProduct.getImage());
                }

            }

            // =====================================================
            // ADDING NEW PRODUCT
            // =====================================================
            else {
                if (imageFile != null && !imageFile.isEmpty()) {

                    Category selectedCategory = categoryRepository
                            .findById(product.getCategoryid())
                            .orElseThrow(() -> new RuntimeException("Category not found"));

                    String mainCategory;
                    String subCategory;

                    if (selectedCategory.getParent() != null) {
                        mainCategory = sanitizeFolderName(selectedCategory.getParent().getCategoryname());
                        subCategory = sanitizeFolderName(selectedCategory.getCategoryname());
                    } else {
                        mainCategory = sanitizeFolderName(selectedCategory.getCategoryname());
                        subCategory = "general";
                    }

                    String imageUrl = cloudinaryService.uploadImage(
                            imageFile,
                            mainCategory,
                            subCategory
                    );

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
            @PathVariable("id") Long id,
            Model model) {

        Product targetProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        model.addAttribute("productForm", targetProduct);

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
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        boolean hasPurchases = purchaseRepository.existsByProductId(id);

        if (hasPurchases) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This product cannot be deleted because it has purchase history."
            );

            return "redirect:/products";
        }

        productService.deleteProduct(id);

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "Product deleted successfully."
        );

        return "redirect:/products";
    }
}