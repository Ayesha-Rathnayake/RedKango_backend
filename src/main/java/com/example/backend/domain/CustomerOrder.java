package com.example.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "customer_orders")
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomerOrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(name = "delivery_charge", nullable = false)
    private BigDecimal deliveryCharge;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus = OrderStatus.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_gateway")
    private String paymentGateway;

    @Column(name = "payhere_payment_id")
    private String payherePaymentId;

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

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "dispatched_at")
    private Instant dispatchedAt;

    @Column(name = "hidden_by_customer", nullable = false)
    private Boolean hiddenByCustomer = false;
}