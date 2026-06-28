package com.example.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "reviews")
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String email;

    @Enumerated(EnumType.STRING)
    private ReviewTargetType targetType;

    private String productName;

    private String service;

    private int rating;

    @Column(nullable = false, length = 2000)
    private String review;

    private boolean approved = true;

    @Column(updatable = false)
    private Instant createdAt;

    @Column(length = 2000)
    private String reply;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}