package com.example.backend.web;

import com.example.backend.domain.SiteSettings;
import com.example.backend.dto.SiteSettingsDto;
import com.example.backend.repository.SiteSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SiteSettingsController {

    private final SiteSettingsRepository repository;

    /**
     * Public endpoint — used by frontend footer.
     */
    @GetMapping("/api/site-settings")
    public ResponseEntity<SiteSettingsDto> getPublic() {
        return ResponseEntity.ok(toDto(getOrCreate()));
    }

    /**
     * Admin read.
     */
    @GetMapping("/api/admin/site-settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SiteSettingsDto> getAdmin() {
        return ResponseEntity.ok(toDto(getOrCreate()));
    }

    /**
     * Admin update.
     */
    @PutMapping("/api/admin/site-settings")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SiteSettingsDto> update(@RequestBody SiteSettingsDto dto) {
        SiteSettings settings = getOrCreate();

        if (dto.getBusinessName() != null) settings.setBusinessName(dto.getBusinessName());
        if (dto.getPhone() != null)         settings.setPhone(dto.getPhone());
        if (dto.getEmail() != null)         settings.setEmail(dto.getEmail());
        if (dto.getAddress() != null)       settings.setAddress(dto.getAddress());
        if (dto.getWhatsappNumber() != null) settings.setWhatsappNumber(dto.getWhatsappNumber());
        if (dto.getFacebookUrl() != null)   settings.setFacebookUrl(dto.getFacebookUrl());
        if (dto.getInstagramUrl() != null)  settings.setInstagramUrl(dto.getInstagramUrl());
        if (dto.getYoutubeUrl() != null)    settings.setYoutubeUrl(dto.getYoutubeUrl());
        if (dto.getTagline() != null)       settings.setTagline(dto.getTagline());

        return ResponseEntity.ok(toDto(repository.save(settings)));
    }

    /**
     * Gets existing settings or seeds defaults if none exist.
     */
    private SiteSettings getOrCreate() {
        List<SiteSettings> all = repository.findAll();
        if (all.isEmpty()) {
            return repository.save(new SiteSettings());
        }
        return all.get(0);
    }

    private SiteSettingsDto toDto(SiteSettings s) {
        SiteSettingsDto dto = new SiteSettingsDto();
        dto.setBusinessName(s.getBusinessName());
        dto.setPhone(s.getPhone());
        dto.setEmail(s.getEmail());
        dto.setAddress(s.getAddress());
        dto.setWhatsappNumber(s.getWhatsappNumber());
        dto.setFacebookUrl(s.getFacebookUrl());
        dto.setInstagramUrl(s.getInstagramUrl());
        dto.setYoutubeUrl(s.getYoutubeUrl());
        dto.setTagline(s.getTagline());
        return dto;
    }
}
