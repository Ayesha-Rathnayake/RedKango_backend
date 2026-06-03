package com.example.backend.service;

import com.example.backend.config.AppProperties;
import com.example.backend.config.PayHereProperties;
import com.example.backend.domain.CustomerOrder;
import com.example.backend.domain.OrderStatus;
import com.example.backend.domain.PaymentStatus;
import com.example.backend.domain.RentalBooking;
import com.example.backend.domain.RentalBookingStatus;
import com.example.backend.domain.RentalPaymentStatus;
import com.example.backend.dto.PaymentDto;
import com.example.backend.repository.CustomerOrderRepository;
import com.example.backend.repository.RentalBookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final CustomerOrderRepository orderRepository;
    private final RentalBookingRepository rentalBookingRepository;
    private final PayHereProperties payHereProperties;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public PaymentDto.PayHereInitResponse createPayHerePayment(Long orderId) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Cancelled orders cannot be paid");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Order is already paid");
        }

        String amount = formatAmount(order.getTotalAmount());
        String currency = payHereProperties.getCurrency();
        String orderNumber = order.getOrderNumber();

        String hash = generateCheckoutHash(
                payHereProperties.getMerchantId(),
                orderNumber,
                amount,
                currency,
                payHereProperties.getMerchantSecret()
        );

        PaymentDto.PayHereInitResponse response = new PaymentDto.PayHereInitResponse();

        response.setCheckoutUrl(payHereProperties.getCheckoutUrl());
        response.setMerchantId(payHereProperties.getMerchantId());
        response.setReturnUrl(appProperties.getFrontendBaseUrl() + "/payment/success?orderId=" + order.getId());
        response.setCancelUrl(appProperties.getFrontendBaseUrl() + "/payment/cancel?orderId=" + order.getId());
        response.setNotifyUrl(appProperties.getBackendBaseUrl() + "/api/payments/payhere/notify");

        response.setOrderId(orderNumber);
        response.setItems("RedKango Order " + orderNumber);
        response.setCurrency(currency);
        response.setAmount(amount);
        response.setHash(hash);

        response.setFirstName(order.getUser().getFirstName());
        response.setLastName(order.getUser().getLastName());
        response.setEmail(order.getUser().getEmail());
        response.setPhone(order.getDeliveryPhone());
        response.setAddress(buildAddress(order));
        response.setCity(order.getDeliveryCity());
        response.setCountry("Sri Lanka");

        return response;
    }

    @Transactional(readOnly = true)
    public PaymentDto.PayHereInitResponse createRentalPayHerePayment(Long bookingId) {
        RentalBooking booking = rentalBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Rental booking not found"));

        if (booking.getBookingStatus() == RentalBookingStatus.CANCELLED) {
            throw new RuntimeException("Cancelled rental booking cannot be paid");
        }

        if (booking.getPaymentStatus() == RentalPaymentStatus.ADVANCE_PAID) {
            throw new RuntimeException("Rental advance payment is already completed");
        }

        BigDecimal payNowAmount = booking.getAdvanceAmount().add(booking.getDeliveryCharge());

        String amount = formatAmount(payNowAmount);
        String currency = payHereProperties.getCurrency();
        String bookingNumber = booking.getBookingNumber();

        String hash = generateCheckoutHash(
                payHereProperties.getMerchantId(),
                bookingNumber,
                amount,
                currency,
                payHereProperties.getMerchantSecret()
        );

        PaymentDto.PayHereInitResponse response = new PaymentDto.PayHereInitResponse();

        response.setCheckoutUrl(payHereProperties.getCheckoutUrl());
        response.setMerchantId(payHereProperties.getMerchantId());

        response.setReturnUrl(
                appProperties.getFrontendBaseUrl()
                        + "/payment/success?type=rental&bookingId="
                        + booking.getId()
                        + "&booking_number="
                        + booking.getBookingNumber()
        );

        response.setCancelUrl(
                appProperties.getFrontendBaseUrl()
                        + "/payment/cancel?type=rental&bookingId="
                        + booking.getId()
        );

        response.setNotifyUrl(appProperties.getBackendBaseUrl() + "/api/payments/payhere/notify");

        response.setOrderId(bookingNumber);
        response.setItems("RedKango Rental Booking " + bookingNumber);
        response.setCurrency(currency);
        response.setAmount(amount);
        response.setHash(hash);

        response.setFirstName(booking.getUser().getFirstName());
        response.setLastName(booking.getUser().getLastName());
        response.setEmail(booking.getUser().getEmail());
        response.setPhone(booking.getDeliveryPhone());
        response.setAddress(buildRentalAddress(booking));
        response.setCity(booking.getDeliveryCity());
        response.setCountry("Sri Lanka");

        return response;
    }

    @Transactional
    public void handlePayHereNotify(PaymentDto.PayHereNotifyRequest request) {
        boolean valid = verifyNotificationSignature(request);

        if (!valid) {
            throw new RuntimeException("Invalid PayHere notification signature");
        }

        String paymentOrderId = request.getOrder_id();

        if (paymentOrderId != null && paymentOrderId.startsWith("RKG-RNT-")) {
            handleRentalPaymentNotify(request);
            return;
        }

        handleShopOrderPaymentNotify(request);
    }

    private void handleShopOrderPaymentNotify(PaymentDto.PayHereNotifyRequest request) {
        CustomerOrder order = orderRepository.findByOrderNumber(request.getOrder_id())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if ("2".equals(request.getStatus_code())) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setOrderStatus(OrderStatus.PROCESSING);
            order.setPaidAt(Instant.now());
            order.setPaymentGateway("PAYHERE");
            order.setPaymentMethod(request.getMethod());
        } else if ("-2".equals(request.getStatus_code())) {
            order.setPaymentStatus(PaymentStatus.FAILED);
        } else if ("0".equals(request.getStatus_code())) {
            order.setPaymentStatus(PaymentStatus.PENDING);
        }

        orderRepository.save(order);
    }

    private void handleRentalPaymentNotify(PaymentDto.PayHereNotifyRequest request) {
        RentalBooking booking = rentalBookingRepository.findByBookingNumber(request.getOrder_id())
                .orElseThrow(() -> new RuntimeException("Rental booking not found"));

        if ("2".equals(request.getStatus_code())) {
            booking.setPaymentStatus(RentalPaymentStatus.ADVANCE_PAID);
            booking.setBookingStatus(RentalBookingStatus.CONFIRMED);
            booking.setAdvancePaidAt(Instant.now());
        } else if ("-2".equals(request.getStatus_code())) {
            booking.setPaymentStatus(RentalPaymentStatus.FAILED);
        } else if ("0".equals(request.getStatus_code())) {
            booking.setPaymentStatus(RentalPaymentStatus.PENDING);
        }

        rentalBookingRepository.save(booking);
    }

    private boolean verifyNotificationSignature(PaymentDto.PayHereNotifyRequest request) {
        String localMd5sig = generateNotificationHash(
                request.getMerchant_id(),
                request.getOrder_id(),
                request.getPayhere_amount(),
                request.getPayhere_currency(),
                request.getStatus_code(),
                payHereProperties.getMerchantSecret()
        );

        return localMd5sig.equalsIgnoreCase(request.getMd5sig());
    }

    private String generateCheckoutHash(
            String merchantId,
            String orderId,
            String amount,
            String currency,
            String merchantSecret
    ) {
        String hashedSecret = md5(merchantSecret).toUpperCase(Locale.ROOT);

        return md5(
                merchantId +
                        orderId +
                        amount +
                        currency +
                        hashedSecret
        ).toUpperCase(Locale.ROOT);
    }

    private String generateNotificationHash(
            String merchantId,
            String orderId,
            String amount,
            String currency,
            String statusCode,
            String merchantSecret
    ) {
        String hashedSecret = md5(merchantSecret).toUpperCase(Locale.ROOT);

        return md5(
                merchantId +
                        orderId +
                        amount +
                        currency +
                        statusCode +
                        hashedSecret
        ).toUpperCase(Locale.ROOT);
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());

            StringBuilder sb = new StringBuilder();

            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available");
        }
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String buildAddress(CustomerOrder order) {
        String line1 = order.getDeliveryAddressLine1() == null ? "" : order.getDeliveryAddressLine1();
        String line2 = order.getDeliveryAddressLine2() == null ? "" : order.getDeliveryAddressLine2();

        return (line1 + " " + line2).trim();
    }

    private String buildRentalAddress(RentalBooking booking) {
        String line1 = booking.getDeliveryAddressLine1() == null ? "" : booking.getDeliveryAddressLine1();
        String line2 = booking.getDeliveryAddressLine2() == null ? "" : booking.getDeliveryAddressLine2();

        return (line1 + " " + line2).trim();
    }
}