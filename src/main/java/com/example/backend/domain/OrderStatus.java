package com.example.backend.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    PROCESSING,
    DISPATCHED,
    DELIVERED,
    CANCELLED
}