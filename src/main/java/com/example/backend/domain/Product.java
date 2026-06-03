package com.example.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "products", uniqueConstraints = @UniqueConstraint(columnNames = "product_id"))
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private String productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(length = 1000)
    private String description;

    @Column(name = "total_units", nullable = false)
    private Integer totalUnits;

    @Column(name = "available_units", nullable = false)
    private Integer availableUnits;

    @Column(nullable = false)
    private Double price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductType type;

    @Column(name = "image_url", length = 5000)
    private String imageUrl;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}