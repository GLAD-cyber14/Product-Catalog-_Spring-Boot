package com.gladwin.userservice.productsdemo.Model;

import jakarta.persistence.*;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String name;

    private double price;

    private String image;

    private int categoryid;

    private int stock;


    // ==========================================
    // DEFAULT CONSTRUCTOR
    // ==========================================

    public Product() {
    }


    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public Product(String name, double price, int categoryid, int stock) {
        this.name = name;
        this.price = price;
        this.categoryid = categoryid;
        this.stock = stock;
    }


    // ==========================================
    // ID
    // ==========================================

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }


    // ==========================================
    // NAME
    // ==========================================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    // ==========================================
    // PRICE
    // ==========================================

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }


    // ==========================================
    // IMAGE
    // ==========================================

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }


    // ==========================================
    // CATEGORY
    // ==========================================

    // Used by Thymeleaf form binding
    public int getCategoryid() {
        return categoryid;
    }

    public void setCategoryid(int categoryid) {
        this.categoryid = categoryid;
    }


    // Keep these for compatibility with your
    // existing REST ProductController
    public int getCategory() {
        return categoryid;
    }

    public void setCategory(int categoryid) {
        this.categoryid = categoryid;
    }


    // ==========================================
    // STOCK
    // ==========================================

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }
}