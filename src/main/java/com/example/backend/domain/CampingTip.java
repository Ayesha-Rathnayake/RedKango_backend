package com.example.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter @Setter
@Entity
@Table(name = "camping_tips")
public class CampingTip {

    public enum MediaType { IMAGE, YOUTUBE, VIDEO }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(unique = true, nullable = false)
    private String slug;

    /** Short summary shown on the listing page */
    @Column(nullable = false, length = 500)
    private String summary;

    /** Full article content — stored as HTML or plain text */
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private String author;

    /**
     * IMAGE  → imageUrl holds the image path/URL
     * YOUTUBE → mediaUrl holds the YouTube watch URL
     * VIDEO   → mediaUrl holds the hosted video file URL
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MediaType mediaType = MediaType.IMAGE;

    private String imageUrl;   // used when mediaType = IMAGE
    private String mediaUrl;   // used when mediaType = YOUTUBE or VIDEO

    private String readTime;   // e.g. "5 min read"

    @Column(nullable = false)
    private boolean published = true;  // admin can hide articles

    @Column(updatable = false)
    private Instant createdAt;
    private Instant updatedAt;

    @PrePersist  protected void onCreate() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate   protected void onUpdate() { updatedAt = Instant.now(); }
}