package com.example.backend.service;

import com.example.backend.config.AppProperties;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mail;
    private final AppProperties props;

    public EmailService(JavaMailSender mail, AppProperties props) {
        this.mail = mail;
        this.props = props;
    }

    @Async
    public void sendVerificationEmail(String to, String token) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject("Verify your email");

            msg.setText(
                    "Click the link below to verify your account:\n\n"
                            + buildVerifyLink(token)
            );

            mail.send(msg);

            System.out.println("EMAIL SENT SUCCESSFULLY TO: " + to);

        } catch (Exception e) {
            System.out.println("EMAIL SENDING FAILED");
            e.printStackTrace();
        }
    }

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        SimpleMailMessage msg = new SimpleMailMessage();

        msg.setTo(to);
        msg.setSubject("Reset your password");

        msg.setText(
                "Click the link below to reset your password:\n\n" +
                        buildResetLink(token) +
                        "\n\nThis link expires in " +
                        props.getReset().getTtlMinutes() +
                        " minutes." +
                        "\n\nIf you did not request a password reset, please ignore this email."
        );

        mail.send(msg);
    }

    @Async
    public void sendOrderConfirmationEmail(
            String to,
            Long orderId,
            String customerName,
            String invoiceDetails
    ) {
        SimpleMailMessage msg = new SimpleMailMessage();

        msg.setTo(to);
        msg.setSubject("RedKango Order Confirmation #" + orderId);

        msg.setText(
                "Dear " + customerName + ",\n\n" +
                        "Thank you for your purchase from RedKango.\n\n" +
                        "Order ID: #" + orderId + "\n\n" +
                        invoiceDetails +
                        "\n\nWe will process your order shortly." +
                        "\n\nRegards,\nRedKango Team"
        );

        mail.send(msg);
    }

    @Async
    public void sendDispatchEmail(
            String to,
            String customerName,
            String orderNumber,
            String courierName,
            String trackingNumber
    ) {
        SimpleMailMessage msg = new SimpleMailMessage();

        msg.setTo(to);
        msg.setSubject("Your RedKango Order Has Been Shipped");

        msg.setText(
                "RED KANGO\n" +
                        "----------------------------------------\n" +
                        "YOUR ORDER HAS BEEN SHIPPED\n" +
                        "----------------------------------------\n\n" +
                        "Hello " + customerName + ",\n\n" +
                        "Great news! Your order has been packed and shipped.\n\n" +
                        "Order Details\n" +
                        "----------------------------------------\n" +
                        "Order Number : " + orderNumber + "\n" +
                        "Courier      : " + courierName + "\n" +
                        "Tracking No. : " + trackingNumber + "\n\n" +
                        "You can use the tracking number above to track your package with the courier service.\n\n" +
                        "If you have any questions regarding your order, please contact our support team.\n\n" +
                        "Thank you for shopping with RedKango.\n\n" +
                        "Best Regards,\n" +
                        "RedKango Team\n" +
                        "Camping & Outdoor Equipment"
        );

        mail.send(msg);
    }

    @Async
    public void sendRentalBookingConfirmedEmail(
            String to,
            String customerName,
            String bookingNumber,
            String startDate,
            String endDate,
            String advanceAmount,
            String remainingAmount
    ) {
        SimpleMailMessage msg = new SimpleMailMessage();

        msg.setTo(to);
        msg.setSubject("RedKango Rental Booking Confirmed");

        msg.setText(
                "RED KANGO\n" +
                        "----------------------------------------\n" +
                        "RENTAL BOOKING CONFIRMED\n" +
                        "----------------------------------------\n\n" +
                        "Hello " + customerName + ",\n\n" +
                        "Your rental booking has been confirmed.\n\n" +
                        "Booking Details\n" +
                        "----------------------------------------\n" +
                        "Booking Number     : " + bookingNumber + "\n" +
                        "Rental Start Date  : " + startDate + "\n" +
                        "Rental End Date    : " + endDate + "\n" +
                        "Advance Paid       : LKR " + advanceAmount + "\n" +
                        "Remaining Balance  : LKR " + remainingAmount + "\n\n" +
                        "Please pay the remaining balance on pickup or delivery.\n\n" +
                        "Thank you for choosing RedKango.\n\n" +
                        "Best Regards,\n" +
                        "RedKango Team"
        );

        mail.send(msg);
    }

    @Async
    public void sendRentalDispatchEmail(
            String to,
            String customerName,
            String bookingNumber,
            String courierName,
            String trackingNumber
    ) {
        SimpleMailMessage msg = new SimpleMailMessage();

        msg.setTo(to);
        msg.setSubject("Your RedKango Rental Equipment Has Been Dispatched");

        msg.setText(
                "RED KANGO\n" +
                        "----------------------------------------\n" +
                        "RENTAL EQUIPMENT DISPATCHED\n" +
                        "----------------------------------------\n\n" +
                        "Hello " + customerName + ",\n\n" +
                        "Your rental equipment has been dispatched.\n\n" +
                        "Dispatch Details\n" +
                        "----------------------------------------\n" +
                        "Booking Number : " + bookingNumber + "\n" +
                        "Courier        : " + courierName + "\n" +
                        "Tracking No.   : " + trackingNumber + "\n\n" +
                        "You can use the tracking number above to track your delivery.\n\n" +
                        "Best Regards,\n" +
                        "RedKango Team"
        );

        mail.send(msg);
    }

    private String buildVerifyLink(String token) {
        return props.getBackendBaseUrl()
                + "/api/auth/verify?token="
                + token;
    }

    private String buildResetLink(String token) {
        if (props.getEmailLinks().isFrontendEnabled()) {
            return props.getFrontendBaseUrl() + "/reset-password?token=" + token;
        } else {
            return props.getBackendBaseUrl() + "/api/auth/reset-password?token=" + token;
        }
    }
}