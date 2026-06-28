package com.example.backend.web;

import com.example.backend.domain.Product;
import com.example.backend.dto.ProductResponse;
import com.example.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping
    public List<ProductResponse> getPublicProducts() {
        return productRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        return toResponse(product);
    }

    private ProductResponse toResponse(Product product) {
        double percentage = getStockPercentage(product);
        String stockStatus = getStockStatus(product);

        return new ProductResponse(
                product.getId(),
                product.getProductId(),
                product.getProductName(),
                product.getDescription(),
                product.getTotalUnits(),
                product.getAvailableUnits(),
                product.getPrice(),
                product.getType().name(),
                product.getImageUrl(),
                stockStatus,
                percentage,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private double getStockPercentage(Product product) {
        if (product.getTotalUnits() == null || product.getTotalUnits() == 0) {
            return 0;
        }

        return (product.getAvailableUnits() * 100.0) / product.getTotalUnits();
    }

    private String getStockStatus(Product product) {
        if (product.getAvailableUnits() == null || product.getAvailableUnits() == 0) {
            return "OUT_OF_STOCK";
        }

        double percentage = getStockPercentage(product);

        if (percentage <= 30) {
            return "LOW_STOCK";
        }

        return "IN_STOCK";
    }
}