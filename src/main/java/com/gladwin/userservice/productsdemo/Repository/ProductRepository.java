package com.gladwin.userservice.productsdemo.Repository;

import com.gladwin.userservice.productsdemo.Model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository // Interfaces with the database
public interface ProductRepository extends JpaRepository<Product,Integer> {
}
