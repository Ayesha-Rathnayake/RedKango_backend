package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderDto {

    @Getter
    @Setter
    public static class CreateOrderRequest {
        private List<CartItemRequest> items;
        private DeliveryAddressRequest deliveryAddress;
    }

    @Getter
    @Setter
    public static class CartItemRequest {
        private Long productId;
        private Integer quantity;
    }

    @Getter
    @Setter
    public static class DeliveryAddressRequest {
        private String fullName;
        private String phone;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String district;
        private String postalCode;
    }

    @Getter
    @Setter
    public static class CreateOrderResponse {
        private Long orderId;
        private String orderNumber;
        private BigDecimal totalAmount;
        private String orderStatus;
        private String paymentStatus;
    }

    @Getter
    @Setter
    public static class OrderItemResponse {
        private Long productDbId;
        private String productId;
        private String productName;
        private String imageUrl;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal lineTotal;
    }

    @Getter
    @Setter
    public static class OrderResponse {
        private Long orderId;
        private String orderNumber;
        private String customerName;
        private String customerEmail;

        private List<OrderItemResponse> items;

        private BigDecimal subtotal;
        private BigDecimal deliveryCharge;
        private BigDecimal totalAmount;

        private String orderStatus;
        private String paymentStatus;

        private String courierName;
        private String trackingNumber;

        private String deliveryFullName;
        private String deliveryPhone;
        private String deliveryAddressLine1;
        private String deliveryAddressLine2;
        private String deliveryCity;
        private String deliveryDistrict;
        private String deliveryPostalCode;

        private Instant createdAt;
        private Instant paidAt;
        private Instant dispatchedAt;
    }

    @Getter
    @Setter
    public static class DispatchRequest {
        private String courierName;
        private String trackingNumber;
    }
}