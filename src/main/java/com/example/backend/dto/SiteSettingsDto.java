package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SiteSettingsDto {
    private String businessName;
    private String phone;
    private String email;
    private String address;
    private String whatsappNumber;
    private String facebookUrl;
    private String instagramUrl;
    private String youtubeUrl;
    private String tagline;
}
