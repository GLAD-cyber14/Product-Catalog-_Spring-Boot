package com.gladwin.userservice.productsdemo.Model;

import jakarta.persistence.*;

@Entity
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int categoryid;

    private String categoryname;

    // Parent category
    // NULL means this is a MAIN category
    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Category parent;


    // Default constructor
    public Category() {
    }


    // Constructor
    public Category(String categoryname) {
        this.categoryname = categoryname;
    }


    // Getters and Setters

    public int getCategoryid() {
        return categoryid;
    }

    public void setCategoryid(int categoryid) {
        this.categoryid = categoryid;
    }


    public String getCategoryname() {
        return categoryname;
    }

    public void setCategoryname(String categoryname) {
        this.categoryname = categoryname;
    }


    public Category getParent() {
        return parent;
    }

    public void setParent(Category parent) {
        this.parent = parent;
    }
}