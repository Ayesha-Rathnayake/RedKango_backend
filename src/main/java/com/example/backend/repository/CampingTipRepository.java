package com.example.backend.repository;

import com.example.backend.domain.CampingTip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CampingTipRepository extends JpaRepository<CampingTip, Long> {

    /** All published tips ordered newest first — used for public listing */
    List<CampingTip> findByPublishedTrueOrderByCreatedAtDesc();

    /** Single published tip — used for public detail view */
    Optional<CampingTip> findByIdAndPublishedTrue(Long id);

    Optional<CampingTip> findBySlugAndPublishedTrue(String slug);

    /** Admin: all tips including unpublished */
    List<CampingTip> findAllByOrderByCreatedAtDesc();

    Optional<CampingTip> findBySlug(String slug);
}