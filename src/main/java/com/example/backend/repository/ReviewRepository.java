package com.example.backend.repository;

import com.example.backend.domain.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByApprovedTrueOrderByCreatedAtDesc();

    List<Review> findAllByOrderByCreatedAtDesc();
}