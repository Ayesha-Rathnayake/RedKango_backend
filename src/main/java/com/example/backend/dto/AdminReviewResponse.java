package com.example.backend.dto;

import java.time.Instant;

public class AdminReviewResponse {
    private Long id;
    private String name;
    private String service;
    private String targetType;
    private String productName;
    private int rating;
    private String comment;
    private String reply;
    private Instant date;

    public AdminReviewResponse(
            Long id,
            String name,
            String service,
            String targetType,
            String productName,
            int rating,
            String comment,
            String reply,
            Instant date
    ) {
        this.id = id;
        this.name = name;
        this.service = service;
        this.targetType = targetType;
        this.productName = productName;
        this.rating = rating;
        this.comment = comment;
        this.reply = reply;
        this.date = date;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getService() { return service; }
    public String getTargetType() { return targetType; }
    public String getProductName() { return productName; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public String getReply() { return reply; }
    public Instant getDate() { return date; }
}