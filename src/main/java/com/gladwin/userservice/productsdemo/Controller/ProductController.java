package com.gladwin.userservice.productsdemo.Controller;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.gladwin.userservice.productsdemo.Model.Product;
import com.gladwin.userservice.productsdemo.Repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.gladwin.userservice.productsdemo.Repository.CategoryRepository;

import java.util.Arrays;
import java.util.List;

@RestController//Tells Spring this handles REST HTTP requests
@CrossOrigin(origins="*")
@RequestMapping("/api/gladwin")//sets base url
public class ProductController {


    @Autowired//Automatically injects the database repository instance
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping//Maps HTTP GET requests to this method
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    @PostMapping//POST:http://localhost:8080/api/gladwin
public Product addProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }
        @PutMapping("/{id}") // PUT: http://localhost:8080/api/gladwin/1
        public Product updateProduct(@PathVariable int id, @RequestBody Product productDetails) {
            // 1. Find the existing product by ID, or throw an error if it doesn't exist
            Product existingProduct = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

            // 2. Map the updated values from the request body
            existingProduct.setName(productDetails.getName());
            existingProduct.setPrice(productDetails.getPrice());

            // 3. Save the modified product back to the MySQL database
            return productRepository.save(existingProduct);


    }

    @DeleteMapping("/{id}") // DELETE: http://localhost:8080/api/gladwin/1
    public String deleteProduct(@PathVariable int id) {
        // 1. Check if the product exists in MySQL before attempting deletion
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));

        // 2. Delete the record from the database
        productRepository.delete(existingProduct);

        // 3. Return a plain text confirmation message
        return "Product with ID " + id + " deleted successfully.";
    }

}
