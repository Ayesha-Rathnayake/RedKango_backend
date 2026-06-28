package com.example.backend.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

public class PaymentDto {

    @Getter
    @Setter
    public static class PayHereInitRequest {
        private Long orderId;
    }

    @Getter
    @Setter
    public static class PayHereInitResponse {
        private String checkoutUrl;

        private String merchantId;
        private String returnUrl;
        private String cancelUrl;
        private String notifyUrl;

        private String orderId;
        private String items;
        private String currency;
        private String amount;
        private String hash;

        private String firstName;
        private String lastName;
        private String email;
        private String phone;
        private String address;
        private String city;
        private String country;
    }

    @Getter
    @Setter
    public static class PayHereNotifyRequest {
        private String merchant_id;
        private String order_id;
        private String payment_id;
        private String payhere_amount;
        private String payhere_currency;
        private String status_code;
        private String md5sig;
        private String method;
        private String status_message;
    }

    @Getter
    @Setter
    public static class PayHereRentalInitRequest {
        private Long bookingId;
    }
}