package com.example.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "site_settings")
public class SiteSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Contact Info
    @Column(name = "business_name")
    private String businessName = "Red Kango";

    @Column(name = "phone")
    private String phone = "+94 76 537 8422";

    @Column(name = "email")
    private String email = "redkango@gmail.com";

    @Column(name = "address")
    private String address = "Bandarawela, Sri Lanka";


    // Social Links
    @Column(name = "whatsapp_number")
    private String whatsappNumber = "94765378422";

    @Column(name = "facebook_url", length = 500)
    private String facebookUrl = "https://www.facebook.com/share/1D5NRgZtyT/";

    @Column(name = "instagram_url", length = 500)
    private String instagramUrl = "https://instagram.com/";

    @Column(name = "youtube_url", length = 500)
    private String youtubeUrl = "https://www.youtube.com/@NutNKnot";

    // About text shown in footer
    @Column(name = "tagline", length = 500)
    private String tagline = "Your trusted partner for camping adventures. Quality tents, equipment, and unforgettable experiences.";

    @Column(name = "updated_at")
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
