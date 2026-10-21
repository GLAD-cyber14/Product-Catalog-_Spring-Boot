package com.gladwin.userservice.productsdemo.Repository;

import com.gladwin.userservice.productsdemo.Model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // Find all main categories
    List<Category> findByParentIsNull();

    // Find all subcategories of a particular category
    List<Category> findByParent(Category parent);
}