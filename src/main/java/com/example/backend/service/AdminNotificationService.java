package com.example.backend.service;

import com.example.backend.domain.AdminNotification;
import com.example.backend.repository.AdminNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private final AdminNotificationRepository notificationRepository;

    // Thread-safe list of active SSE emitters (one per admin browser tab)
    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * Called by the frontend to subscribe to the SSE stream.
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        return emitter;
    }

    /**
     * Creates a notification, persists it, and pushes it to all connected admins.
     */
    @Transactional
    public void push(AdminNotification.NotificationType type, String message) {
        AdminNotification notification = new AdminNotification();
        notification.setType(type);
        notification.setMessage(message);
        notification.setRead(false);

        AdminNotification saved = notificationRepository.save(notification);

        // Push to all connected SSE clients
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(toDto(saved)));
            } catch (IOException e) {
                emitters.remove(emitter);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getRecent() {
        return notificationRepository.findTop50ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return notificationRepository.countByReadFalse();
    }

    @Transactional
    public void markAsRead(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllAsRead() {
        notificationRepository.findTop50ByOrderByCreatedAtDesc()
                .stream()
                .filter(n -> !n.isRead())
                .forEach(n -> {
                    n.setRead(true);
                    notificationRepository.save(n);
                });
    }

    @Transactional
    public void deleteById(Long id) {
        notificationRepository.deleteById(id);
    }

    private NotificationDto toDto(AdminNotification n) {
        return new NotificationDto(
                n.getId(),
                n.getType().name(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt().toEpochMilli()
        );
    }

    public record NotificationDto(
            Long id,
            String type,
            String message,
            boolean read,
            long createdAt
    ) {}
}
