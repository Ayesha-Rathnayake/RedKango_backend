package com.example.backend.dto;

import java.time.Instant;

public class ProductResponse {

    private Long id;
    private String productId;
    private String productName;
    private String description;
    private Integer totalUnits;
    private Integer availableUnits;
    private Double price;
    private String type;
    private String imageUrl;
    private String stockStatus;
    private Double stockPercentage;
    private Instant createdAt;
    private Instant updatedAt;

    public ProductResponse(
            Long id,
            String productId,
            String productName,
            String description,
            Integer totalUnits,
            Integer availableUnits,
            Double price,
            String type,
            String imageUrl,
            String stockStatus,
            Double stockPercentage,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.description = description;
        this.totalUnits = totalUnits;
        this.availableUnits = availableUnits;
        this.price = price;
        this.type = type;
        this.imageUrl = imageUrl;
        this.stockStatus = stockStatus;
        this.stockPercentage = stockPercentage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getDescription() { return description; }
    public Integer getTotalUnits() { return totalUnits; }
    public Integer getAvailableUnits() { return availableUnits; }
    public Double getPrice() { return price; }
    public String getType() { return type; }
    public String getImageUrl() { return imageUrl; }
    public String getStockStatus() { return stockStatus; }
    public Double getStockPercentage() { return stockPercentage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}