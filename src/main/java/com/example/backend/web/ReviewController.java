package com.example.backend.web;

import com.example.backend.domain.AdminNotification;
import com.example.backend.domain.Review;
import com.example.backend.domain.ReviewTargetType;
import com.example.backend.domain.User;
import com.example.backend.dto.CreateReviewRequest;
import com.example.backend.dto.ReviewResponse;
import com.example.backend.repository.ReviewRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.AdminNotificationService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "http://localhost:4200")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    private final AdminNotificationService adminNotificationService;

    public ReviewController(
            ReviewRepository reviewRepository,
            UserRepository userRepository,
            AdminNotificationService adminNotificationService
    ) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.adminNotificationService = adminNotificationService;
    }


    @GetMapping
    public List<ReviewResponse> getPublicReviews() {
        return reviewRepository.findByApprovedTrueOrderByCreatedAtDesc()
                .stream()
                .map(review -> toResponse(review, false))
                .toList();
    }

    @PostMapping
    public ReviewResponse createReview(
            @Valid @RequestBody CreateReviewRequest request,
            Authentication authentication
    ) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Review review = new Review();

        review.setName(user.getFirstName() + " " + user.getLastName());
        review.setEmail(user.getEmail());

        review.setTargetType(ReviewTargetType.valueOf(request.getTargetType().toUpperCase()));
        review.setProductName(request.getProductName());
        review.setService(request.getService());
        review.setRating(request.getRating());
        review.setReview(request.getReview());
        review.setApproved(true);

        Review savedReview = reviewRepository.save(review);

        adminNotificationService.push(
                AdminNotification.NotificationType.NEW_REVIEW,
                "New Review: " + savedReview.getRating() + "/5 ★ | " + user.getFirstName() + " " + user.getLastName() + " | " + savedReview.getService()
        );

        return toResponse(savedReview, false);

    }

    private ReviewResponse toResponse(Review review, boolean includeEmail) {
        return new ReviewResponse(
                review.getId(),
                review.getName(),
                includeEmail ? review.getEmail() : null,
                review.getTargetType() != null ? review.getTargetType().name().toLowerCase() : null,
                review.getProductName(),
                review.getService(),
                review.getRating(),
                review.getReview(),
                review.getReply(),
                review.getCreatedAt()
        );
    }
}