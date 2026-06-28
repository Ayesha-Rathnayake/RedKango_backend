package com.example.backend.repository;

import com.example.backend.domain.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {

    List<AdminNotification> findTop50ByOrderByCreatedAtDesc();

    long countByReadFalse();
}
