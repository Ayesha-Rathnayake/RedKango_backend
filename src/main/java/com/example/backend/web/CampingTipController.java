package com.example.backend.web;

import com.example.backend.service.CampingTipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/camping-tips")
public class CampingTipController {

    private final CampingTipService service;

    public CampingTipController(CampingTipService service) {
        this.service = service;
    }

    // GET ALL PUBLISHED ARTICLES
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getList() {
        return ResponseEntity.ok(service.getPublishedList());
    }

    // GET ARTICLE BY ID
    @GetMapping("/id/{id}")
    public ResponseEntity<Map<String, Object>> getById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(service.getPublishedById(id));
    }

    // GET ARTICLE BY SLUG
    @GetMapping("/{slug}")
    public ResponseEntity<Map<String, Object>> getBySlug(
            @PathVariable String slug
    ) {
        return ResponseEntity.ok(service.getPublishedBySlug(slug));
    }
}