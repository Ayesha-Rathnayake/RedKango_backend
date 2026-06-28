package com.example.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "rental_bookings")
public class RentalBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_number", nullable = false, unique = true)
    private String bookingNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RentalBookingItem> items = new ArrayList<>();

    @Column(name = "rental_start_date", nullable = false)
    private LocalDate rentalStartDate;

    @Column(name = "rental_end_date", nullable = false)
    private LocalDate rentalEndDate;

    @Column(name = "total_days", nullable = false)
    private Integer totalDays;

    @Column(name = "subtotal", nullable = false)
    private BigDecimal subtotal;

    @Column(name = "delivery_method", nullable = false)
    private String deliveryMethod = "DELIVERY"; // DELIVERY or PICKUP


    @Column(name = "delivery_charge", nullable = false)
    private BigDecimal deliveryCharge;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "advance_amount", nullable = false)
    private BigDecimal advanceAmount;

    @Column(name = "remaining_amount", nullable = false)
    private BigDecimal remainingAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false)
    private RentalBookingStatus bookingStatus = RentalBookingStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private RentalPaymentStatus paymentStatus = RentalPaymentStatus.PENDING;

    @Column(name = "delivery_full_name")
    private String deliveryFullName;

    @Column(name = "delivery_phone")
    private String deliveryPhone;

    @Column(name = "delivery_address_line1")
    private String deliveryAddressLine1;

    @Column(name = "delivery_address_line2")
    private String deliveryAddressLine2;

    @Column(name = "delivery_city")
    private String deliveryCity;

    @Column(name = "delivery_district")
    private String deliveryDistrict;

    @Column(name = "delivery_postal_code")
    private String deliveryPostalCode;

    @Column(name = "courier_name")
    private String courierName;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @Column(name = "customer_note", length = 1000)
    private String customerNote;

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "advance_paid_at")
    private Instant advancePaidAt;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}