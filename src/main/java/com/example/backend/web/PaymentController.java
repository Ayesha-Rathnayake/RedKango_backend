package com.example.backend.web;

import com.example.backend.dto.PaymentDto;
import com.example.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/payhere/init")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentDto.PayHereInitResponse> initPayHerePayment(
            @RequestBody PaymentDto.PayHereInitRequest request
    ) {
        return ResponseEntity.ok(
                paymentService.createPayHerePayment(request.getOrderId())
        );
    }

    @PostMapping("/payhere/notify")
    public ResponseEntity<String> payHereNotify(
            @ModelAttribute PaymentDto.PayHereNotifyRequest request
    ) {
        paymentService.handlePayHereNotify(request);
        return ResponseEntity.ok("OK");
    }

    @PostMapping("/payhere/rental/init")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PaymentDto.PayHereInitResponse> initRentalPayHerePayment(
            @RequestBody PaymentDto.PayHereRentalInitRequest request
    ) {
        return ResponseEntity.ok(
                paymentService.createRentalPayHerePayment(request.getBookingId())
        );
    }
}