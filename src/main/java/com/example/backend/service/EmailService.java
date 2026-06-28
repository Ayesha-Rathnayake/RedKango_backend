package com.example.backend.service;

import com.example.backend.config.AppProperties;
import com.example.backend.domain.CustomerOrder;
import com.example.backend.domain.CustomerOrderItem;
import com.example.backend.domain.RentalBooking;
import com.example.backend.domain.RentalBookingItem;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mail;
    private final AppProperties props;

    private static final NumberFormat LKR = NumberFormat.getNumberInstance(Locale.US);
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ── AUTH EMAILS ──────────────────────────────────────────────────────────

    @Async
    public void sendVerificationEmail(String to, String token) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject("Verify your email");
            msg.setText("Click the link below to verify your account:\n\n"
                    + buildVerifyLink(token));
            mail.send(msg);
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
                        "\n\nThis link expires in " + props.getReset().getTtlMinutes() + " minutes." +
                        "\n\nIf you did not request a password reset, please ignore this email."
        );
        mail.send(msg);
    }

    // ── ORDER INVOICE EMAIL ──────────────────────────────────────────────────

    @Async
    public void sendOrderInvoiceEmail(String to, CustomerOrder order) {
        try {
            String customerName = order.getUser().getFirstName()
                    + " " + order.getUser().getLastName();
            String subject = "Payment Confirmed – Order " + order.getOrderNumber()
                    + " | RedKango";
            sendHtml(to, subject, buildOrderInvoiceHtml(customerName, order));
        } catch (Exception e) {
            System.out.println("ORDER INVOICE EMAIL FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── RENTAL INVOICE EMAIL ─────────────────────────────────────────────────

    @Async
    public void sendRentalInvoiceEmail(String to, RentalBooking booking) {
        try {
            String customerName = booking.getUser().getFirstName()
                    + " " + booking.getUser().getLastName();
            String subject = "Booking Confirmed – " + booking.getBookingNumber()
                    + " | RedKango";
            sendHtml(to, subject, buildRentalInvoiceHtml(customerName, booking));
        } catch (Exception e) {
            System.out.println("RENTAL INVOICE EMAIL FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── DISPATCH EMAILS ──────────────────────────────────────────────────────

    @Async
    public void sendDispatchEmail(String to, String customerName,
                                  String orderNumber, String courierName,
                                  String trackingNumber) {
        try {
            sendHtml(to,
                    "Your Order Has Been Shipped – " + orderNumber + " | RedKango",
                    buildDispatchHtml(customerName, orderNumber, courierName,
                            trackingNumber, false));
        } catch (Exception e) {
            System.out.println("DISPATCH EMAIL FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void sendRentalDispatchEmail(String to, String customerName,
                                        String bookingNumber, String courierName,
                                        String trackingNumber) {
        try {
            sendHtml(to,
                    "Your Rental Equipment Has Been Dispatched – "
                            + bookingNumber + " | RedKango",
                    buildDispatchHtml(customerName, bookingNumber, courierName,
                            trackingNumber, true));
        } catch (Exception e) {
            System.out.println("RENTAL DISPATCH EMAIL FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── LEGACY — keeps existing call in RentalBookingService compiling ───────

    @Async
    public void sendRentalBookingConfirmedEmail(
            String to, String customerName, String bookingNumber,
            String startDate, String endDate,
            String advanceAmount, String remainingAmount) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Rental Booking Confirmed – " + bookingNumber + " | RedKango");
        msg.setText(
                "Dear " + customerName + ",\n\n" +
                        "Your rental booking " + bookingNumber + " has been confirmed.\n\n" +
                        "Rental Start : " + startDate + "\n" +
                        "Rental End   : " + endDate + "\n" +
                        "Advance Paid : LKR " + advanceAmount + "\n" +
                        "Balance Due  : LKR " + remainingAmount + "\n\n" +
                        "Thank you for choosing RedKango.\n\nBest Regards,\nRedKango Team"
        );
        mail.send(msg);
    }

    @Async
    public void sendRentalCancelledEmail(String to, String customerName,
                                         String bookingNumber, boolean advancePaid) {
        try {
            String subject = "Booking Cancelled – " + bookingNumber + " | RedKango";
            StringBuilder sb = new StringBuilder();
            sb.append(htmlHeader());
            sb.append(hero("Booking Cancelled",
                    "Your rental booking has been cancelled."));
            sb.append("<div style='padding:32px 40px;'>");
            sb.append("<p style='font-size:15px;color:#374151;margin:0 0 24px;'>")
                    .append("Hi <strong>").append(esc(customerName)).append("</strong>,<br><br>")
                    .append("Your rental booking <strong>").append(esc(bookingNumber))
                    .append("</strong> has been cancelled.</p>");
            sb.append("<table style='width:100%;border-collapse:collapse;margin-bottom:24px;'>")
                    .append("<tr>")
                    .append(metaCell("Booking Number", bookingNumber))
                    .append(metaCell("Status", "Cancelled"))
                    .append("</tr></table>");
            if (advancePaid) {
                sb.append("<div style='background:#fef2f2;border:1px solid #fecaca;")
                        .append("border-radius:8px;padding:16px;margin-bottom:24px;")
                        .append("font-size:14px;color:#991b1b;'>")
                        .append("<strong>&#9888; Please Note:</strong> Your advance payment is ")
                        .append("non-refundable as per our cancellation policy.</div>");
            } else {
                sb.append("<div style='background:#f0fdf4;border:1px solid #bbf7d0;")
                        .append("border-radius:8px;padding:16px;margin-bottom:24px;")
                        .append("font-size:14px;color:#15803d;'>")
                        .append("<strong>&#10003; No charges apply</strong> — ")
                        .append("no payment was made for this booking.</div>");
            }
            sb.append("<p style='font-size:13px;color:#6b7280;margin:0;'>")
                    .append("If you have any questions, please contact our support team.")
                    .append("</p>");
            sb.append("</div>");
            sb.append(htmlFooter());
            sendHtml(to, subject, sb.toString());
        } catch (Exception e) {
            System.out.println("RENTAL CANCELLED EMAIL FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Async
    public void sendAccountSuspendedEmail(String to, String customerName,
                                          String reason, String notes) {
        try {
            String subject = "Your RedKango Account Has Been Suspended";
            StringBuilder sb = new StringBuilder();
            sb.append(htmlHeader());
            sb.append(hero("Account Suspended",
                    "Your account access has been temporarily suspended."));
            sb.append("<div style='padding:32px 40px;'>");
            sb.append("<p style='font-size:15px;color:#374151;margin:0 0 24px;'>")
                    .append("Hi <strong>").append(esc(customerName)).append("</strong>,<br><br>")
                    .append("Your RedKango account has been temporarily suspended. ")
                    .append("You will not be able to log in until the suspension is lifted.</p>");
            sb.append("<div style='background:#fefce8;border:1px solid #fde047;border-radius:8px;")
                    .append("padding:16px;margin-bottom:24px;'>")
                    .append("<p style='font-size:13px;color:#713f12;margin:0 0 8px;'>")
                    .append("<strong>Reason:</strong> ").append(esc(reason)).append("</p>");
            if (notes != null && !notes.isBlank()) {
                sb.append("<p style='font-size:13px;color:#713f12;margin:0;'>")
                        .append("<strong>Additional Notes:</strong> ").append(esc(notes)).append("</p>");
            }
            sb.append("</div>");
            sb.append("<p style='font-size:13px;color:#6b7280;margin:0;'>")
                    .append("If you believe this is a mistake or wish to appeal, please contact our ")
                    .append("support team at <strong>support@redkango.com</strong>.</p>");
            sb.append("</div>");
            sb.append(htmlFooter());
            sendHtml(to, subject, sb.toString());
        } catch (Exception e) {
            System.out.println("ACCOUNT SUSPENDED EMAIL FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void sendAccountDeactivatedEmail(String to, String customerName,
                                            String reason, String notes) {
        try {
            String subject = "Your RedKango Account Has Been Deactivated";
            StringBuilder sb = new StringBuilder();
            sb.append(htmlHeader());
            sb.append(hero("Account Deactivated",
                    "Your account has been permanently deactivated."));
            sb.append("<div style='padding:32px 40px;'>");
            sb.append("<p style='font-size:15px;color:#374151;margin:0 0 24px;'>")
                    .append("Hi <strong>").append(esc(customerName)).append("</strong>,<br><br>")
                    .append("Your RedKango account has been permanently deactivated. ")
                    .append("You will no longer be able to log in or place orders.</p>");
            sb.append("<div style='background:#fef2f2;border:1px solid #fecaca;border-radius:8px;")
                    .append("padding:16px;margin-bottom:24px;'>")
                    .append("<p style='font-size:13px;color:#991b1b;margin:0 0 8px;'>")
                    .append("<strong>Reason:</strong> ").append(esc(reason)).append("</p>");
            if (notes != null && !notes.isBlank()) {
                sb.append("<p style='font-size:13px;color:#991b1b;margin:0;'>")
                        .append("<strong>Additional Notes:</strong> ").append(esc(notes)).append("</p>");
            }
            sb.append("</div>");
            sb.append("<p style='font-size:13px;color:#6b7280;margin:0;'>")
                    .append("Your existing orders and bookings are preserved for record-keeping. ")
                    .append("If you believe this is an error, contact <strong>support@redkango.com</strong>.</p>");
            sb.append("</div>");
            sb.append(htmlFooter());
            sendHtml(to, subject, sb.toString());
        } catch (Exception e) {
            System.out.println("ACCOUNT DEACTIVATED EMAIL FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Async
    public void sendRentalReturnedEmail(String to, String customerName,
                                        String bookingNumber, String paymentStatus,
                                        String remainingAmount) {
        try {
            String subject = "Gear Returned – " + bookingNumber + " | RedKango";
            String html = buildRentalReturnedHtml(customerName, bookingNumber,
                    paymentStatus, remainingAmount);
            sendHtml(to, subject, html);
        } catch (Exception e) {
            System.out.println("RENTAL RETURNED EMAIL FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Async
    public void sendRentalPickupConfirmedEmail(String to, String customerName,
                                               String bookingNumber,
                                               String startDate, String endDate) {
        try {
            String subject = "Gear Collected – " + bookingNumber + " | RedKango";
            StringBuilder sb = new StringBuilder();
            sb.append(htmlHeader());
            sb.append(hero("Gear Collected – Enjoy Your Adventure!",
                    "Your rental gear has been collected. Have a great trip!"));
            sb.append("<div style='padding:32px 40px;'>");
            sb.append("<p style='font-size:15px;color:#374151;margin:0 0 24px;'>")
                    .append("Hi <strong>").append(esc(customerName)).append("</strong>,<br><br>")
                    .append("Your rental gear for booking <strong>").append(esc(bookingNumber))
                    .append("</strong> has been collected. Your rental period is now active!</p>");
            sb.append("<table style='width:100%;border-collapse:collapse;margin-bottom:24px;'>")
                    .append("<tr>")
                    .append(metaCell("Booking Number", bookingNumber))
                    .append(metaCell("Status", "&#10003; Gear Collected"))
                    .append("</tr><tr>")
                    .append(metaCell("Rental Start", startDate))
                    .append(metaCell("Rental End", endDate))
                    .append("</tr></table>");
            sb.append("<p style='font-size:13px;color:#6b7280;margin:0;'>")
                    .append("Please return the gear by the rental end date. ")
                    .append("If you need to extend your rental, contact us as soon as possible.")
                    .append("</p>");
            sb.append("</div>");
            sb.append(htmlFooter());
            sendHtml(to, subject, sb.toString());
        } catch (Exception e) {
            System.out.println("PICKUP CONFIRMED EMAIL FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }


    @Async
    public void sendRentalCompletedEmail(String to, String customerName,
                                         String bookingNumber) {
        try {
            String subject = "Booking Completed – " + bookingNumber + " | RedKango";
            String html = buildRentalCompletedHtml(customerName, bookingNumber);
            sendHtml(to, subject, html);
        } catch (Exception e) {
            System.out.println("RENTAL COMPLETED EMAIL FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }


    // ── HTML BUILDERS ────────────────────────────────────────────────────────

    private String buildOrderInvoiceHtml(String customerName, CustomerOrder order) {
        String paidAt = order.getPaidAt() != null
                ? DATE_FMT.format(order.getPaidAt().atZone(ZoneId.systemDefault()))
                : "—";

        StringBuilder sb = new StringBuilder();
        sb.append(htmlHeader());
        sb.append(hero("Payment Confirmed",
                "Your order has been paid and is now being processed."));
        sb.append("<div style='padding:32px 40px;'>");

        sb.append("<p style='font-size:15px;color:#374151;margin:0 0 24px;'>")
                .append("Hi <strong>").append(esc(customerName)).append("</strong>,<br><br>")
                .append("Thank you for your purchase! We have received your payment and your ")
                .append("order is now being prepared. Below is your invoice.</p>");

        sb.append("<table style='width:100%;border-collapse:collapse;margin-bottom:24px;'>")
                .append("<tr>")
                .append(metaCell("Order Number", order.getOrderNumber()))
                .append(metaCell("Payment Date", paidAt))
                .append("</tr><tr>")
                .append(metaCell("Status", "&#10003; Paid"))
                .append(metaCell("Payment Method", "Card / PayHere"))
                .append("</tr></table>");

        sb.append(sectionTitle("Items Ordered"));
        sb.append("<table style='width:100%;border-collapse:collapse;margin-bottom:24px;'>")
                .append("<thead><tr style='background:#f3f4f6;'>")
                .append(th("Product")).append(th("Qty"))
                .append(th("Unit Price")).append(th("Total"))
                .append("</tr></thead><tbody>");

        for (CustomerOrderItem item : order.getItems()) {
            sb.append("<tr style='border-bottom:1px solid #e5e7eb;'>")
                    .append(td(esc(item.getProductName())))
                    .append(tdCenter(String.valueOf(item.getQuantity())))
                    .append(tdRight("LKR " + LKR.format(item.getUnitPrice())))
                    .append(tdRight("LKR " + LKR.format(item.getLineTotal())))
                    .append("</tr>");
        }
        sb.append("</tbody></table>");

        sb.append(totalsTable(List.of(
                new String[]{"Subtotal", "LKR " + LKR.format(order.getSubtotal())},
                new String[]{"Delivery Charge",
                        "LKR " + LKR.format(order.getDeliveryCharge())}
        ), "Total Paid", "LKR " + LKR.format(order.getTotalAmount())));

        sb.append(sectionTitle("Delivery Address"));
        sb.append(addressBlock(
                order.getDeliveryFullName(), order.getDeliveryPhone(),
                order.getDeliveryAddressLine1(), order.getDeliveryAddressLine2(),
                order.getDeliveryCity(), order.getDeliveryDistrict(),
                order.getDeliveryPostalCode()));

        sb.append("<p style='font-size:13px;color:#6b7280;margin:24px 0 0;'>")
                .append("We will notify you once your order is dispatched with a tracking number.")
                .append("</p>");


        sb.append("</div>");
        sb.append(htmlFooter());
        return sb.toString();
    }

    private String buildRentalInvoiceHtml(String customerName, RentalBooking booking) {
        String paidAt = booking.getAdvancePaidAt() != null
                ? DATE_FMT.format(
                booking.getAdvancePaidAt().atZone(ZoneId.systemDefault()))
                : "—";
        String startDate = DATE_FMT.format(booking.getRentalStartDate());
        String endDate   = DATE_FMT.format(booking.getRentalEndDate());
        boolean isPickup = "PICKUP".equals(booking.getDeliveryMethod());

        StringBuilder sb = new StringBuilder();
        sb.append(htmlHeader());
        sb.append(hero("Rental Booking Confirmed",
                "Your advance payment is received and your booking is confirmed."));
        sb.append("<div style='padding:32px 40px;'>");

        sb.append("<p style='font-size:15px;color:#374151;margin:0 0 24px;'>")

                .append("Hi <strong>").append(esc(customerName)).append("</strong>,<br><br>")
                .append("Your rental booking has been confirmed. ");

        if (isPickup) {
            sb.append("Please visit our store on or before your rental start date to collect your gear. ");
        } else {
            sb.append("Your gear will be dispatched before your rental start date. ");
        }

        sb.append("Below is your booking invoice.</p>");


        sb.append("<table style='width:100%;border-collapse:collapse;margin-bottom:24px;'>")
                .append("<tr>")
                .append(metaCell("Booking Number", booking.getBookingNumber()))
                .append(metaCell("Advance Paid On", paidAt))
                .append("</tr><tr>")
                .append(metaCell("Rental Start", startDate))
                .append(metaCell("Rental End", endDate))
                .append("</tr><tr>")
                .append(metaCell("Total Days", booking.getTotalDays() + " day(s)"))
                .append(metaCell("Status", "&#10003; Advance Paid"))
                .append("</tr></table>");

        sb.append(sectionTitle("Rental Items"));
        sb.append("<table style='width:100%;border-collapse:collapse;margin-bottom:24px;'>")
                .append("<thead><tr style='background:#f3f4f6;'>")
                .append(th("Product")).append(th("Qty")).append(th("Daily Rate"))
                .append(th("Days")).append(th("Line Total"))
                .append("</tr></thead><tbody>");

        for (RentalBookingItem item : booking.getItems()) {
            sb.append("<tr style='border-bottom:1px solid #e5e7eb;'>")
                    .append(td(esc(item.getProductName())))
                    .append(tdCenter(String.valueOf(item.getQuantity())))
                    .append(tdRight("LKR " + LKR.format(item.getDailyRate())))
                    .append(tdCenter(String.valueOf(booking.getTotalDays())))
                    .append(tdRight("LKR " + LKR.format(item.getLineTotal())))
                    .append("</tr>");
        }
        sb.append("</tbody></table>");

        sb.append(totalsTable(List.of(
                new String[]{"Rental Cost", "LKR " + LKR.format(booking.getSubtotal())},
                new String[]{"Delivery Charge",
                        "LKR " + LKR.format(booking.getDeliveryCharge())},
                new String[]{"Total Amount",
                        "LKR " + LKR.format(booking.getTotalAmount())},
                new String[]{"Advance Paid (50%)",
                        "LKR " + LKR.format(booking.getAdvanceAmount())}
        ), "Remaining Balance Due", "LKR " + LKR.format(booking.getRemainingAmount())));
        String codNote = isPickup
                ? "due in cash when you collect your gear from our store (50% rental balance)"
                : "due in cash on delivery (50% rental balance + LKR 1,000 delivery charge)";

        sb.append("<div style='background:#fefce8;border:1px solid #fde047;" +
                        "border-radius:8px;padding:16px;margin-bottom:24px;" +
                        "font-size:14px;color:#713f12;'>")
                .append("<strong>&#9888; Cash Payment Due: LKR ")
                .append(LKR.format(booking.getRemainingAmount()))
                .append("</strong><br>")
                .append("<span style='font-size:13px;'>This amount is ")
                .append(codNote)
                .append(".</span></div>");


        if (isPickup) {
            sb.append(sectionTitle("Pickup Information"));
            sb.append("<div style='background:#f0fdf4;border:1px solid #bbf7d0;" +
                            "border-radius:8px;padding:16px;margin-bottom:24px;" +
                            "font-size:13px;color:#374151;line-height:1.8;'>")
                    .append("<strong style='color:#15803d;'>RedKango Store</strong><br>")
                    .append("Wewathanna, Bandarawela<br>")
                    .append("Sri Lanka<br><br>")
                    .append("&#128222; <strong>+94 76 537 8422</strong><br>")
                    .append("<strong>Please bring this booking confirmation and a valid ID when collecting your gear.</strong>")
                    .append("</div>");
            sb.append("<p style='font-size:13px;color:#6b7280;margin:0 0 24px;'>")
                    .append("Contact us if you need to reschedule your pickup.")
                    .append("</p>");
        } else {
            sb.append(sectionTitle("Delivery Address"));
            sb.append(addressBlock(
                    booking.getDeliveryFullName(), booking.getDeliveryPhone(),
                    booking.getDeliveryAddressLine1(), booking.getDeliveryAddressLine2(),
                    booking.getDeliveryCity(), booking.getDeliveryDistrict(),
                    booking.getDeliveryPostalCode()));
            sb.append("<p style='font-size:13px;color:#6b7280;margin:24px 0 0;'>")
                    .append("You will receive a separate email with courier and tracking details.")
                    .append("</p>");
        }


        sb.append("</div>");
        sb.append(htmlFooter());
        return sb.toString();
    }

    private String buildRentalReturnedHtml(String customerName, String bookingNumber,
                                           String paymentStatus, String remainingAmount) {
        StringBuilder sb = new StringBuilder();
        sb.append(htmlHeader());
        sb.append(hero("Gear Received – Thank You!",
                "Your rental equipment has been successfully returned."));
        sb.append("<div style='padding:32px 40px;'>");

        sb.append("<p style='font-size:15px;color:#374151;margin:0 0 24px;'>")
                .append("Hi <strong>").append(esc(customerName)).append("</strong>,<br><br>")
                .append("We have received your rental gear for booking <strong>")
                .append(esc(bookingNumber)).append("</strong>. Thank you for returning it on time!</p>");

        sb.append("<table style='width:100%;border-collapse:collapse;margin-bottom:24px;'>")
                .append("<tr>")
                .append(metaCell("Booking Number", bookingNumber))
                .append(metaCell("Return Status", "&#10003; Gear Received"))
                .append("</tr></table>");

        boolean balanceDue = "ADVANCE_PAID".equals(paymentStatus);

        if (balanceDue) {
            sb.append("<div style='background:#fefce8;border:1px solid #fde047;" +
                            "border-radius:8px;padding:16px;margin-bottom:24px;" +
                            "font-size:14px;color:#713f12;'>")
                    .append("<strong>&#9888; Outstanding Balance: LKR ").append(esc(remainingAmount))
                    .append("</strong><br>")
                    .append("<span style='font-size:13px;'>Your remaining balance is due. ")
                    .append("Please settle this with our team.</span>")
                    .append("</div>");
        } else {
            sb.append("<div style='background:#f0fdf4;border:1px solid #bbf7d0;" +
                            "border-radius:8px;padding:16px;margin-bottom:24px;" +
                            "font-size:14px;color:#15803d;'>")
                    .append("<strong>&#10003; All payments settled.</strong> Your booking is now complete.")
                    .append("</div>");
        }

        sb.append("<p style='font-size:13px;color:#6b7280;margin:0;'>")
                .append("We hope you had a great outdoor experience with RedKango gear. ")
                .append("We look forward to serving you again!</p>");

        sb.append("</div>");
        sb.append(htmlFooter());
        return sb.toString();
    }

    private String buildRentalCompletedHtml(String customerName, String bookingNumber) {
        StringBuilder sb = new StringBuilder();
        sb.append(htmlHeader());
        sb.append(hero("Booking Completed",
                "Your rental booking has been fully completed."));
        sb.append("<div style='padding:32px 40px;'>");

        sb.append("<p style='font-size:15px;color:#374151;margin:0 0 24px;'>")
                .append("Hi <strong>").append(esc(customerName)).append("</strong>,<br><br>")
                .append("Your rental booking <strong>").append(esc(bookingNumber))
                .append("</strong> has been fully completed and all payments are settled. ")
                .append("Thank you for choosing RedKango!</p>");

        sb.append("<table style='width:100%;border-collapse:collapse;margin-bottom:24px;'>")
                .append("<tr>")
                .append(metaCell("Booking Number", bookingNumber))
                .append(metaCell("Status", "&#10003; Completed"))
                .append("</tr></table>");

        sb.append("<div style='background:#f0fdf4;border:1px solid #bbf7d0;" +
                        "border-radius:8px;padding:16px;margin-bottom:24px;" +
                        "font-size:14px;color:#15803d;'>")
                .append("<strong>All payments are settled and your booking is closed.</strong>")
                .append("</div>");

        sb.append("<p style='font-size:13px;color:#6b7280;margin:0;'>")
                .append("We hope you enjoyed your outdoor adventure. ")
                .append("Please consider leaving a review — it helps us improve our service!")
                .append("</p>");

        sb.append("</div>");
        sb.append(htmlFooter());
        return sb.toString();
    }


    private String buildDispatchHtml(String customerName, String refNumber,
                                     String courierName, String trackingNumber,
                                     boolean isRental) {
        String title = isRental
                ? "Your Rental Equipment Is On The Way"
                : "Your Order Has Been Shipped";
        String subtitle = isRental
                ? "Your camping gear has been dispatched and is heading your way."
                : "Your order has been packed and handed to the courier.";
        String label = isRental ? "Booking Number" : "Order Number";

        StringBuilder sb = new StringBuilder();
        sb.append(htmlHeader());
        sb.append(hero(title, subtitle));
        sb.append("<div style='padding:32px 40px;'>")
                .append("<p style='font-size:15px;color:#374151;margin:0 0 24px;'>")
                .append("Hi <strong>").append(esc(customerName)).append("</strong>,<br><br>")
                .append(esc(subtitle)).append("</p>");

        sb.append("<table style='width:100%;border-collapse:collapse;margin-bottom:24px;'>")
                .append("<tr>")
                .append(metaCell(label, refNumber))
                .append(metaCell("Courier Service", courierName))
                .append("</tr><tr>")
                .append(metaCell("Tracking Number", trackingNumber))
                .append(metaCell("", ""))
                .append("</tr></table>");

        sb.append("<p style='font-size:13px;color:#6b7280;margin:24px 0 0;'>")
                .append("Use the tracking number above to track your ")
                .append(isRental ? "delivery" : "package")
                .append(" with the courier service.</p>");

        sb.append("</div>");
        sb.append(htmlFooter());
        return sb.toString();
    }

    // ── HTML COMPONENTS ──────────────────────────────────────────────────────

    private String htmlHeader() {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
                "</head><body style='margin:0;padding:0;background:#f3f4f6;" +
                "font-family:Arial,Helvetica,sans-serif;'>" +
                "<table width='100%' cellpadding='0' cellspacing='0'>" +
                "<tr><td align='center' style='padding:32px 16px;'>" +
                "<table width='600' cellpadding='0' cellspacing='0' " +
                "style='background:#ffffff;border-radius:12px;overflow:hidden;" +
                "box-shadow:0 2px 8px rgba(0,0,0,0.08);'>";
    }

    private String hero(String heading, String subtext) {
        return "<tr><td style='background:#15803d;padding:36px 40px;text-align:center;'>" +
                "<h1 style='margin:0 0 8px;font-size:22px;color:#ffffff;font-weight:700;'>"
                + esc(heading) + "</h1>" +
                "<p style='margin:0;font-size:14px;color:#bbf7d0;'>"
                + esc(subtext) + "</p></td></tr><tr><td>";
    }

    private String sectionTitle(String title) {
        return "<h3 style='font-size:14px;font-weight:700;color:#15803d;" +
                "text-transform:uppercase;letter-spacing:0.05em;" +
                "border-bottom:2px solid #dcfce7;padding-bottom:8px;" +
                "margin:28px 0 16px;'>" + esc(title) + "</h3>";
    }

    private String metaCell(String label, String value) {
        return "<td style='padding:8px 12px 8px 0;vertical-align:top;width:50%;'>" +
                "<div style='font-size:11px;color:#9ca3af;text-transform:uppercase;" +
                "letter-spacing:0.05em;margin-bottom:2px;'>" + esc(label) + "</div>" +
                "<div style='font-size:14px;color:#111827;font-weight:600;'>"
                + (value) + "</div></td>";
    }

    private String th(String text) {
        return "<th style='padding:10px 12px;text-align:left;font-size:12px;" +
                "color:#6b7280;font-weight:600;border-bottom:1px solid #e5e7eb;'>"
                + esc(text) + "</th>";
    }

    private String td(String text) {
        return "<td style='padding:10px 12px;font-size:13px;color:#374151;'>"
                + esc(text) + "</td>";
    }

    private String tdCenter(String text) {
        return "<td style='padding:10px 12px;font-size:13px;color:#374151;" +
                "text-align:center;'>" + esc(text) + "</td>";
    }

    private String tdRight(String text) {
        return "<td style='padding:10px 12px;font-size:13px;color:#374151;" +
                "text-align:right;'>" + esc(text) + "</td>";
    }

    private String totalsTable(List<String[]> rows, String finalLabel, String finalValue) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table style='width:100%;border-collapse:collapse;margin-bottom:24px;'>");
        for (String[] row : rows) {
            sb.append("<tr>")
                    .append("<td style='padding:6px 0;font-size:13px;color:#6b7280;'>")
                    .append(esc(row[0])).append("</td>")
                    .append("<td style='padding:6px 0;font-size:13px;color:#374151;text-align:right;'>")
                    .append(esc(row[1])).append("</td></tr>");
        }
        sb.append("<tr style='border-top:2px solid #15803d;'>")
                .append("<td style='padding:12px 0 6px;font-size:15px;font-weight:700;color:#111827;'>")
                .append(esc(finalLabel)).append("</td>")
                .append("<td style='padding:12px 0 6px;font-size:15px;font-weight:700;" +
                        "color:#15803d;text-align:right;'>")
                .append(esc(finalValue)).append("</td></tr></table>");
        return sb.toString();
    }

    private String addressBlock(String name, String phone,
                                String line1, String line2,
                                String city, String district, String postalCode) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div style='background:#f9fafb;border:1px solid #e5e7eb;" +
                "border-radius:8px;padding:16px;margin-bottom:24px;" +
                "font-size:13px;color:#374151;line-height:1.7;'>");
        if (name    != null && !name.isBlank())    sb.append("<strong>").append(esc(name)).append("</strong><br>");
        if (phone   != null && !phone.isBlank())   sb.append(esc(phone)).append("<br>");
        if (line1   != null && !line1.isBlank())   sb.append(esc(line1)).append("<br>");
        if (line2   != null && !line2.isBlank())   sb.append(esc(line2)).append("<br>");

        StringBuilder cityLine = new StringBuilder();
        if (city       != null && !city.isBlank())       cityLine.append(city);
        if (district   != null && !district.isBlank())   { if (!cityLine.isEmpty()) cityLine.append(", "); cityLine.append(district); }
        if (postalCode != null && !postalCode.isBlank()) { if (!cityLine.isEmpty()) cityLine.append(" ");  cityLine.append(postalCode); }
        if (!cityLine.isEmpty()) sb.append(esc(cityLine.toString())).append("<br>");

        sb.append("Sri Lanka</div>");
        return sb.toString();
    }

    private String htmlFooter() {
        return "</td></tr>" +
                "<tr><td style='background:#f9fafb;padding:24px 40px;text-align:center;" +
                "border-top:1px solid #e5e7eb;'>" +
                "<p style='margin:0 0 4px;font-size:13px;font-weight:700;color:#15803d;'>" +
                "RedKango</p>" +
                "<p style='margin:0;font-size:12px;color:#9ca3af;'>" +
                "Camping &amp; Outdoor Equipment &nbsp;|&nbsp; Sri Lanka</p>" +
                "</td></tr></table></td></tr></table></body></html>";
    }

    // ── UTILITIES ────────────────────────────────────────────────────────────

    private void sendHtml(String to, String subject, String html)
            throws MessagingException {
        MimeMessage message = mail.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mail.send(message);
        System.out.println("HTML EMAIL SENT TO: " + to);
    }

private String esc(String s) {
    if (s == null) return "";
    String result = s;
    result = result.replace("&", "&amp;");
    result = result.replace("<", "<");
    result = result.replace(">", "&gt;");
    result = result.replace("\"", "&#34;");
    return result;
}



    private String buildVerifyLink(String token) {
        return props.getBackendBaseUrl() + "/api/auth/verify?token=" + token;
    }

    private String buildResetLink(String token) {
        if (props.getEmailLinks().isFrontendEnabled()) {
            return props.getFrontendBaseUrl() + "/reset-password?token=" + token;
        } else {
            return props.getBackendBaseUrl() + "/api/auth/reset-password?token=" + token;
        }
    }
    }