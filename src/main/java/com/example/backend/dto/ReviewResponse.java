package com.example.backend.dto;

import java.time.Instant;

public class ReviewResponse {
    private Long id;
    private String name;
    private String email;
    private String targetType;
    private String productName;
    private String service;
    private int rating;
    private String review;
    private String reply;
    private Instant date;

    public ReviewResponse(
            Long id,
            String name,
            String email,
            String targetType,
            String productName,
            String service,
            int rating,
            String review,
            String reply,
            Instant date
    ) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.targetType = targetType;
        this.productName = productName;
        this.service = service;
        this.rating = rating;
        this.review = review;
        this.reply = reply;
        this.date = date;
    }

    public Long getId() { return id; }

    public String getName() { return name; }

    public String getEmail() { return email; }

    public String getTargetType() { return targetType; }

    public String getProductName() { return productName; }

    public String getService() { return service; }

    public int getRating() { return rating; }

    public String getReview() { return review; }

    public String getReply() { return reply; }

    public Instant getDate() { return date; }
}