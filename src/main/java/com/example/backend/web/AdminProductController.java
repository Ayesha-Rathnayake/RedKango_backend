package com.example.backend.web;

import com.example.backend.domain.Product;
import com.example.backend.domain.ProductType;
import com.example.backend.dto.ProductRequest;
import com.example.backend.dto.ProductResponse;
import com.example.backend.repository.ProductRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductController {

    private final ProductRepository productRepository;

    @Value("${file.upload-dir:uploads/products}")
    private String uploadDir;

    public AdminProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public List<ProductResponse> getProducts() {
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

    @PostMapping(
            value = "/upload-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
        }

        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        }

        try {
            Path uploadPath = Paths.get(System.getProperty("user.dir"))
                    .resolve(uploadDir)
                    .normalize();

            Files.createDirectories(uploadPath);

            String originalName = file.getOriginalFilename();
            String extension = ".jpg";

            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }

            String fileName = UUID.randomUUID() + extension;
            Path targetPath = uploadPath.resolve(fileName).normalize();

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return Map.of(
                    "imageUrl",
                    "http://localhost:8080/uploads/products/" + fileName
            );

        } catch (IOException e) {
            e.printStackTrace();

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to upload image: " + e.getMessage()
            );
        }
    }

    @PostMapping
    public ProductResponse createProduct(@Valid @RequestBody ProductRequest request) {
        if (productRepository.existsByProductId(request.getProductId().trim())) {
            throw new IllegalArgumentException("Product ID already exists");
        }

        Product product = new Product();
        applyRequest(product, request);

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @PutMapping("/{id}")
    public ProductResponse updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        Product existingProduct = productRepository.findByProductId(request.getProductId().trim())
                .orElse(null);

        if (existingProduct != null && !existingProduct.getId().equals(id)) {
            throw new IllegalArgumentException("Product ID already exists");
        }

        applyRequest(product, request);

        Product saved = productRepository.save(product);
        return toResponse(saved);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteProduct(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        productRepository.delete(product);

        return Map.of("message", "Product deleted successfully");
    }

    private void applyRequest(Product product, ProductRequest request) {
        if (request.getAvailableUnits() > request.getTotalUnits()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Available units cannot be greater than total units"
            );
        }

        product.setProductId(request.getProductId().trim());
        product.setProductName(request.getProductName().trim());
        product.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        product.setTotalUnits(request.getTotalUnits());
        product.setAvailableUnits(request.getAvailableUnits());
        product.setPrice(request.getPrice());
        product.setType(ProductType.valueOf(request.getType().trim().toUpperCase()));
        product.setImageUrl(request.getImageUrl() == null ? "" : request.getImageUrl());
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
//STOCK PERCENTAGE
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