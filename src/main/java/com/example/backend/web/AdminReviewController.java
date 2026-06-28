package com.example.backend.web;

import com.example.backend.domain.Review;
import com.example.backend.dto.AdminReviewResponse;
import com.example.backend.dto.UpdateReviewReplyRequest;
import com.example.backend.repository.ReviewRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final ReviewRepository reviewRepository;

    public AdminReviewController(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @GetMapping
    public List<AdminReviewResponse> getAllReviews() {
        return reviewRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @PutMapping("/{id}/reply")
    public AdminReviewResponse saveReply(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewReplyRequest request
    ) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        review.setReply(request.getReply() == null ? "" : request.getReply().trim());

        Review saved = reviewRepository.saveAndFlush(review);
        return toAdminResponse(saved);
    }

    @DeleteMapping("/{id}/reply")
    public AdminReviewResponse deleteReply(@PathVariable Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        review.setReply("");
        Review saved = reviewRepository.saveAndFlush(review);

        return toAdminResponse(saved);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> deleteReview(@PathVariable Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        reviewRepository.delete(review);

        return Map.of("message", "Review deleted successfully");
    }

    private AdminReviewResponse toAdminResponse(Review review) {
        String targetType = review.getTargetType().name().toLowerCase();

        String serviceLabel = "service".equals(targetType)
                ? review.getService()
                : review.getProductName();

        return new AdminReviewResponse(
                review.getId(),
                review.getName(),
                serviceLabel,
                targetType,
                review.getProductName(),
                review.getRating(),
                review.getReview(),
                review.getReply(),
                review.getCreatedAt()
        );
    }
}