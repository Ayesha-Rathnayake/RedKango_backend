package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public class RentalBookingDto {

    @Getter
    @Setter
    public static class CreateRentalBookingRequest {
        private LocalDate rentalStartDate;
        private LocalDate rentalEndDate;
        private List<RentalCartItemRequest> items;
        private DeliveryAddressRequest deliveryAddress;
        private String customerNote;
    }

    @Getter
    @Setter
    public static class RentalCartItemRequest {
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
    public static class RentalBookingItemResponse {
        private Long productDbId;
        private String productId;
        private String productName;
        private String imageUrl;
        private BigDecimal dailyRate;
        private Integer quantity;
        private BigDecimal lineTotal;
    }

    @Getter
    @Setter
    public static class RentalBookingResponse {
        private Long bookingId;
        private String bookingNumber;
        private String customerName;
        private String customerEmail;

        private LocalDate rentalStartDate;
        private LocalDate rentalEndDate;
        private Integer totalDays;

        private List<RentalBookingItemResponse> items;

        private BigDecimal subtotal;
        private BigDecimal deliveryCharge;
        private BigDecimal totalAmount;
        private BigDecimal advanceAmount;
        private BigDecimal remainingAmount;

        private String bookingStatus;
        private String paymentStatus;

        private String deliveryFullName;
        private String deliveryPhone;
        private String deliveryAddressLine1;
        private String deliveryAddressLine2;
        private String deliveryCity;
        private String deliveryDistrict;
        private String deliveryPostalCode;

        private String courierName;
        private String trackingNumber;

        private Instant createdAt;
        private Instant advancePaidAt;
        private Instant dispatchedAt;
        private Instant returnedAt;
        private Instant completedAt;
    }

    @Getter
    @Setter
    public static class AvailabilityResponse {
        private Long productId;
        private String productName;
        private Integer totalUnits;
        private Integer bookedUnits;
        private Integer availableUnits;
        private Boolean available;
    }

    @Getter
    @Setter
    public static class DispatchRentalRequest {
        private String courierName;
        private String trackingNumber;
    }
}