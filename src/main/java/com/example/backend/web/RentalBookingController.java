package com.example.backend.web;

import com.example.backend.dto.RentalBookingDto;
import com.example.backend.service.RentalBookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rental-bookings")
public class RentalBookingController {

    private final RentalBookingService rentalBookingService;

    @GetMapping("/availability")
    public ResponseEntity<List<RentalBookingDto.AvailabilityResponse>> checkAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return ResponseEntity.ok(
                rentalBookingService.getRentalProductAvailability(startDate, endDate)
        );
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RentalBookingDto.RentalBookingResponse> createBooking(
            @RequestBody RentalBookingDto.CreateRentalBookingRequest request
    ) {
        return ResponseEntity.ok(rentalBookingService.createBooking(request));
    }

    @PutMapping("/{bookingId}/advance-paid")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RentalBookingDto.RentalBookingResponse> markAdvancePaid(
            @PathVariable Long bookingId
    ) {
        return ResponseEntity.ok(rentalBookingService.markAdvancePaid(bookingId));
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RentalBookingDto.RentalBookingResponse>> getMyBookings() {
        return ResponseEntity.ok(rentalBookingService.getMyBookings());
    }

    @PutMapping("/{bookingId}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RentalBookingDto.RentalBookingResponse> cancelBooking(
            @PathVariable Long bookingId
    ) {
        return ResponseEntity.ok(rentalBookingService.cancelBooking(bookingId));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RentalBookingDto.RentalBookingResponse>> getAllBookingsForAdmin() {
        return ResponseEntity.ok(rentalBookingService.getAllBookingsForAdmin());
    }

    @PutMapping("/admin/{bookingId}/dispatch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RentalBookingDto.RentalBookingResponse> dispatchBooking(
            @PathVariable Long bookingId,
            @RequestBody RentalBookingDto.DispatchRentalRequest request
    ) {
        return ResponseEntity.ok(rentalBookingService.dispatchBooking(bookingId, request));
    }

    @PutMapping("/admin/{bookingId}/rented")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RentalBookingDto.RentalBookingResponse> markAsRented(
            @PathVariable Long bookingId
    ) {
        return ResponseEntity.ok(rentalBookingService.markAsRented(bookingId));
    }

    @PutMapping("/admin/{bookingId}/returned")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RentalBookingDto.RentalBookingResponse> markAsReturned(
            @PathVariable Long bookingId
    ) {
        return ResponseEntity.ok(rentalBookingService.markAsReturned(bookingId));
    }

    @PutMapping("/admin/{bookingId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RentalBookingDto.RentalBookingResponse> completeBooking(
            @PathVariable Long bookingId
    ) {
        return ResponseEntity.ok(rentalBookingService.completeBooking(bookingId));
    }
}