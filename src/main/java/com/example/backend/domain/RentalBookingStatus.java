package com.example.backend.domain;

public enum RentalBookingStatus {
    PENDING_PAYMENT,
    CONFIRMED,
    READY_FOR_DISPATCH,
    DISPATCHED,
    RENTED,
    RETURNED,
    COMPLETED,
    CANCELLED
}