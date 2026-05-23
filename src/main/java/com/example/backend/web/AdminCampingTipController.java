package com.example.backend.web;

import com.example.backend.dto.CampingTipDto;
import com.example.backend.service.CampingTipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/camping-tips")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminCampingTipController {

    private final CampingTipService campingTipService;

    // GET ALL TIPS (ADMIN)
    @GetMapping
    public List<Map<String, Object>> getAllTips() {
        return campingTipService.getAllForAdmin();
    }

    // GET SINGLE TIP (ADMIN)
    @GetMapping("/{id}")
    public Map<String, Object> getTipById(@PathVariable Long id) {
        return campingTipService.getByIdForAdmin(id);
    }

    // CREATE TIP
    @PostMapping
    public Map<String, Object> create(
            @Valid @RequestBody CampingTipDto dto
    ) {
        return campingTipService.create(dto);
    }

    // UPDATE TIP
    @PutMapping("/{id}")
    public Map<String, Object> update(
            @PathVariable Long id,
            @Valid @RequestBody CampingTipDto dto
    ) {
        return campingTipService.update(id, dto);
    }

    // DELETE TIP
    @DeleteMapping("/{id}")
    public void deleteTip(@PathVariable Long id) {
        campingTipService.delete(id);
    }

    // TOGGLE PUBLISH / UNPUBLISH
    @PatchMapping("/{id}/toggle-published")
    public Map<String, Object> togglePublished(
            @PathVariable Long id
    ) {
        return campingTipService.togglePublished(id);
    }
    @GetMapping("/slug/{slug}")
    public Map<String, Object> getBySlug(@PathVariable String slug) {
        return campingTipService.getBySlugForAdmin(slug);
    }
}