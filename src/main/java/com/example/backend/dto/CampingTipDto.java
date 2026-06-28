package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CampingTipDto {

    @NotBlank(message = "Title is required")
    public String title;

    @NotBlank(message = "Summary is required")
    @Size(max = 500, message = "Summary cannot exceed 500 characters")
    public String summary;

    public String content;   // full article body (HTML allowed)

    @NotBlank(message = "Author is required")
    public String author;

    @NotNull(message = "Media type is required")
    public String mediaType; // "IMAGE" | "YOUTUBE" | "VIDEO"

    public String imageUrl;  // for IMAGE type
    public String mediaUrl;  // for YOUTUBE / VIDEO type

    public String readTime;

    public boolean published = true;
}