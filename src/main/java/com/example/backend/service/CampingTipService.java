package com.example.backend.service;

import com.example.backend.domain.CampingTip;
import com.example.backend.dto.CampingTipDto;
import com.example.backend.repository.CampingTipRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class CampingTipService {

    private final CampingTipRepository repo;

    public CampingTipService(CampingTipRepository repo) {
        this.repo = repo;
    }

    // ─── PUBLIC: listing (summary only — no full content) ────────────────────

    public List<Map<String, Object>> getPublishedList() {
        return repo.findByPublishedTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toSummaryDto)
                .toList();
    }

    // ─── PUBLIC: single article (full content) ────────────────────────────────

    public Map<String, Object> getPublishedById(Long id) {
        CampingTip tip = repo.findByIdAndPublishedTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));
        return toFullDto(tip);
    }

    public Map<String, Object> getPublishedBySlug(String slug) {

        CampingTip tip = repo.findBySlugAndPublishedTrue(slug)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));

        return toFullDto(tip);
    }

    public Map<String, Object> getBySlugForAdmin(String slug) {

        CampingTip tip = repo.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));

        return toFullDto(tip);
    }

    // ─── ADMIN: all (including unpublished) ──────────────────────────────────

    public List<Map<String, Object>> getAllForAdmin() {
        return repo.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toFullDto)
                .toList();
    }
// Admin:by id
    public Map<String, Object> getByIdForAdmin(Long id) {

        CampingTip tip = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));

        return toFullDto(tip);
    }

    // ─── ADMIN: create ────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> create(CampingTipDto dto) {
        CampingTip tip = new CampingTip();
        applyDto(tip, dto);
        return toFullDto(repo.saveAndFlush(tip));
    }

    // ─── ADMIN: update ────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> update(Long id, CampingTipDto dto) {
        CampingTip tip = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));
        applyDto(tip, dto);
        return toFullDto(repo.saveAndFlush(tip));
    }

    // ─── ADMIN: toggle published ──────────────────────────────────────────────

    @Transactional
    public Map<String, Object> togglePublished(Long id) {
        CampingTip tip = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Article not found"));
        tip.setPublished(!tip.isPublished());
        return toFullDto(repo.saveAndFlush(tip));
    }

    // ─── ADMIN: delete ────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("Article not found");
        }
        repo.deleteById(id);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private void applyDto(CampingTip tip, CampingTipDto dto) {

        tip.setTitle(dto.title);

        // Generate unique slug
        String baseSlug = dto.title
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        String slug = baseSlug;
        int counter = 1;

        while (true) {

            var existing = repo.findBySlug(slug);

            if (existing.isEmpty()) {
                break;
            }

            // Allow same article during update
            if (tip.getId() != null &&
                    existing.get().getId().equals(tip.getId())) {
                break;
            }

            slug = baseSlug + "-" + counter++;
        }

        tip.setSlug(slug);

        tip.setSummary(dto.summary);

        tip.setContent(dto.content);

        tip.setAuthor(dto.author);

        // Safe enum conversion
        try {
            tip.setMediaType(
                    CampingTip.MediaType.valueOf(dto.mediaType.toUpperCase())
            );
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid media type");
        }

        tip.setImageUrl(dto.imageUrl);

        tip.setMediaUrl(dto.mediaUrl);

        tip.setReadTime(dto.readTime);

        tip.setPublished(dto.published);
    }

    /** Summary DTO — no full content, used in listing page */
    private Map<String, Object> toSummaryDto(CampingTip t) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id",        t.getId());
        dto.put("title",     t.getTitle());
        dto.put("slug", t.getSlug());
        dto.put("summary",   t.getSummary());
        dto.put("author",    t.getAuthor());
        dto.put("mediaType", t.getMediaType().name());
        dto.put("imageUrl",  t.getImageUrl());
        dto.put("mediaUrl",  t.getMediaUrl());
        dto.put("readTime",  t.getReadTime());
        dto.put("createdAt", t.getCreatedAt());
        return dto;
    }

    /** Full DTO — includes content, used in detail page */
    private Map<String, Object> toFullDto(CampingTip t) {
        Map<String, Object> dto = toSummaryDto(t);
        dto.put("content",   t.getContent());
        dto.put("published", t.isPublished());
        dto.put("updatedAt", t.getUpdatedAt());
        return dto;
    }
}