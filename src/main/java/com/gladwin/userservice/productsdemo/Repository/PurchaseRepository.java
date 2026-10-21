package com.gladwin.userservice.productsdemo.Repository;

import com.gladwin.userservice.productsdemo.Model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRepository extends JpaRepository<Purchase, Integer> {

    List<Purchase> findAllByOrderByPurchaseDateDesc();
    boolean existsByProductId(int productId);
}