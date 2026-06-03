package com.example.backend.repository;

import com.example.backend.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByProductId(String productId);

    boolean existsByProductId(String productId);

    List<Product> findAllByOrderByCreatedAtDesc();
}