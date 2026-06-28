package com.example.backend.web;

import com.example.backend.service.AdminNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService notificationService;

    /**
     * SSE stream endpoint — admin subscribes here on page load.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return notificationService.subscribe();
    }

    /**
     * Get recent notifications on initial load.
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminNotificationService.NotificationDto> getAll() {
        return notificationService.getRecent();
    }

    /**
     * Get unread count.
     */
    @GetMapping("/unread-count")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Long> getUnreadCount() {
        return Map.of("count", notificationService.getUnreadCount());
    }

    /**
     * Mark single notification as read.
     */
    @PatchMapping("/{id}/read")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Mark all as read.
     */
    @PatchMapping("/read-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok().build();
    }

    /**
     * Delete a notification.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        notificationService.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
