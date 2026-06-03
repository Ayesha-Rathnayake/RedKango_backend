package com.example.backend.web;

import com.example.backend.domain.TermsCondition;
import com.example.backend.repository.TermsConditionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
@CrossOrigin
public class TermsController {

    private final TermsConditionRepository termsRepository;

    @GetMapping("/active")
    public TermsCondition getActiveTerms() {

        return termsRepository.findByActiveTrue()
                .orElseThrow(() ->
                        new RuntimeException("No active terms found"));
    }
}