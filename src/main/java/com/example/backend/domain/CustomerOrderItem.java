package com.example.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "customer_order_items")
public class CustomerOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private CustomerOrder order;

    @Column(name = "product_db_id", nullable = false)
    private Long productDbId;

    @Column(name = "product_id")
    private String productId;

    @Column(name = "product_name")
    private String productName;

    @Column(name = "image_url", length = 5000)
    private String imageUrl;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;
}