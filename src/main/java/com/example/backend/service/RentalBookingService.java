package com.example.backend.service;

import com.example.backend.domain.*;
import com.example.backend.dto.RentalBookingDto;
import com.example.backend.repository.ProductRepository;
import com.example.backend.repository.RentalBookingRepository;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RentalBookingService {

    private final RentalBookingRepository rentalBookingRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final BigDecimal DELIVERY_CHARGE = BigDecimal.valueOf(1000);

    private static final Set<RentalBookingStatus> ACTIVE_BOOKING_STATUSES = Set.of(
            RentalBookingStatus.PENDING_PAYMENT,
            RentalBookingStatus.CONFIRMED,
            RentalBookingStatus.READY_FOR_DISPATCH,
            RentalBookingStatus.DISPATCHED,
            RentalBookingStatus.RENTED
    );

    @Transactional
    public RentalBookingDto.RentalBookingResponse createBooking(
            RentalBookingDto.CreateRentalBookingRequest request
    ) {
        User user = getCurrentUser();

        validateRequest(request);

        int totalDays = calculateDays(
                request.getRentalStartDate(),
                request.getRentalEndDate()
        );

        RentalBooking booking = new RentalBooking();
        booking.setUser(user);
        booking.setBookingNumber(generateBookingNumber());
        booking.setRentalStartDate(request.getRentalStartDate());
        booking.setRentalEndDate(request.getRentalEndDate());
        booking.setTotalDays(totalDays);
        booking.setCustomerNote(request.getCustomerNote());

        var address = request.getDeliveryAddress();
        booking.setDeliveryFullName(address.getFullName());
        booking.setDeliveryPhone(address.getPhone());
        booking.setDeliveryAddressLine1(address.getAddressLine1());
        booking.setDeliveryAddressLine2(address.getAddressLine2());
        booking.setDeliveryCity(address.getCity());
        booking.setDeliveryDistrict(address.getDistrict());
        booking.setDeliveryPostalCode(address.getPostalCode());

        BigDecimal subtotal = BigDecimal.ZERO;

        for (RentalBookingDto.RentalCartItemRequest cartItem : request.getItems()) {
            Product product = productRepository.findById(cartItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            if (product.getType() != ProductType.RENTAL) {
                throw new RuntimeException(product.getProductName() + " is not available for rental");
            }

            if (cartItem.getQuantity() == null || cartItem.getQuantity() <= 0) {
                throw new RuntimeException("Invalid quantity");
            }

            int availableUnits = getAvailableUnitsForProduct(
                    product,
                    request.getRentalStartDate(),
                    request.getRentalEndDate()
            );

            if (cartItem.getQuantity() > availableUnits) {
                throw new RuntimeException(
                        product.getProductName() +
                                " has only " +
                                availableUnits +
                                " units available for selected dates"
                );
            }

            BigDecimal dailyRate = BigDecimal.valueOf(product.getPrice());
            BigDecimal lineTotal = dailyRate
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                    .multiply(BigDecimal.valueOf(totalDays));

            RentalBookingItem item = new RentalBookingItem();
            item.setBooking(booking);
            item.setProductDbId(product.getId());
            item.setProductId(product.getProductId());
            item.setProductName(product.getProductName());
            item.setImageUrl(product.getImageUrl());
            item.setDailyRate(dailyRate);
            item.setQuantity(cartItem.getQuantity());
            item.setLineTotal(lineTotal);

            booking.getItems().add(item);
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal totalAmount = subtotal.add(DELIVERY_CHARGE);
        BigDecimal advanceAmount = totalAmount
                .multiply(BigDecimal.valueOf(0.5))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainingAmount = totalAmount.subtract(advanceAmount);

        booking.setSubtotal(subtotal);
        booking.setDeliveryCharge(DELIVERY_CHARGE);
        booking.setTotalAmount(totalAmount);
        booking.setAdvanceAmount(advanceAmount);
        booking.setRemainingAmount(remainingAmount);
        booking.setBookingStatus(RentalBookingStatus.PENDING_PAYMENT);
        booking.setPaymentStatus(RentalPaymentStatus.PENDING);

        RentalBooking saved = rentalBookingRepository.save(booking);

        return toResponse(saved);
    }

    @Transactional
    public RentalBookingDto.RentalBookingResponse markAdvancePaid(Long bookingId) {
        RentalBooking booking = rentalBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Rental booking not found"));

        if (booking.getBookingStatus() == RentalBookingStatus.CANCELLED) {
            throw new RuntimeException("Cancelled booking cannot be paid");
        }

        booking.setPaymentStatus(RentalPaymentStatus.ADVANCE_PAID);
        booking.setBookingStatus(RentalBookingStatus.CONFIRMED);
        booking.setAdvancePaidAt(Instant.now());

        RentalBooking saved = rentalBookingRepository.save(booking);

        User user = saved.getUser();
        emailService.sendRentalBookingConfirmedEmail(
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                saved.getBookingNumber(),
                saved.getRentalStartDate().toString(),
                saved.getRentalEndDate().toString(),
                saved.getAdvanceAmount().toPlainString(),
                saved.getRemainingAmount().toPlainString()
        );

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<RentalBookingDto.RentalBookingResponse> getMyBookings() {
        User user = getCurrentUser();

        return rentalBookingRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RentalBookingDto.RentalBookingResponse> getAllBookingsForAdmin() {
        return rentalBookingRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RentalBookingDto.AvailabilityResponse> getRentalProductAvailability(
            LocalDate startDate,
            LocalDate endDate
    ) {
        validateDates(startDate, endDate);

        return productRepository.findAll()
                .stream()
                .filter(product -> product.getType() == ProductType.RENTAL)
                .map(product -> {
                    int bookedUnits = getBookedUnits(
                            product.getId(),
                            startDate,
                            endDate
                    );

                    int availableUnits = Math.max(
                            product.getTotalUnits() - bookedUnits,
                            0
                    );

                    RentalBookingDto.AvailabilityResponse response =
                            new RentalBookingDto.AvailabilityResponse();

                    response.setProductId(product.getId());
                    response.setProductName(product.getProductName());
                    response.setTotalUnits(product.getTotalUnits());
                    response.setBookedUnits(bookedUnits);
                    response.setAvailableUnits(availableUnits);
                    response.setAvailable(availableUnits > 0);

                    return response;
                })
                .toList();
    }

    @Transactional
    public RentalBookingDto.RentalBookingResponse dispatchBooking(
            Long bookingId,
            RentalBookingDto.DispatchRentalRequest request
    ) {
        RentalBooking booking = rentalBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Rental booking not found"));

        if (booking.getBookingStatus() != RentalBookingStatus.CONFIRMED &&
                booking.getBookingStatus() != RentalBookingStatus.READY_FOR_DISPATCH) {
            throw new RuntimeException("Only confirmed bookings can be dispatched");
        }

        if (booking.getPaymentStatus() != RentalPaymentStatus.ADVANCE_PAID) {
            throw new RuntimeException("Booking cannot be dispatched before advance payment");
        }

        if (request.getCourierName() == null || request.getCourierName().isBlank()) {
            throw new RuntimeException("Courier service is required");
        }

        if (request.getTrackingNumber() == null || request.getTrackingNumber().isBlank()) {
            throw new RuntimeException("Tracking number is required");
        }

        booking.setCourierName(request.getCourierName().trim());
        booking.setTrackingNumber(request.getTrackingNumber().trim());
        booking.setBookingStatus(RentalBookingStatus.DISPATCHED);
        booking.setDispatchedAt(Instant.now());

        RentalBooking saved = rentalBookingRepository.save(booking);

        User user = saved.getUser();

        emailService.sendRentalDispatchEmail(
                user.getEmail(),
                user.getFirstName() + " " + user.getLastName(),
                saved.getBookingNumber(),
                saved.getCourierName(),
                saved.getTrackingNumber()
        );

        return toResponse(saved);
    }

    @Transactional
    public RentalBookingDto.RentalBookingResponse markAsRented(Long bookingId) {
        RentalBooking booking = rentalBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Rental booking not found"));

        if (booking.getBookingStatus() != RentalBookingStatus.DISPATCHED) {
            throw new RuntimeException("Only dispatched bookings can be marked as rented");
        }

        booking.setBookingStatus(RentalBookingStatus.RENTED);

        return toResponse(rentalBookingRepository.save(booking));
    }

    @Transactional
    public RentalBookingDto.RentalBookingResponse markAsReturned(Long bookingId) {
        RentalBooking booking = rentalBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Rental booking not found"));

        if (booking.getBookingStatus() != RentalBookingStatus.RENTED) {
            throw new RuntimeException("Only rented bookings can be marked as returned");
        }

        booking.setBookingStatus(RentalBookingStatus.RETURNED);
        booking.setReturnedAt(Instant.now());

        return toResponse(rentalBookingRepository.save(booking));
    }

    @Transactional
    public RentalBookingDto.RentalBookingResponse completeBooking(Long bookingId) {
        RentalBooking booking = rentalBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Rental booking not found"));

        if (booking.getBookingStatus() != RentalBookingStatus.RETURNED) {
            throw new RuntimeException("Only returned bookings can be completed");
        }

        booking.setBookingStatus(RentalBookingStatus.COMPLETED);
        booking.setCompletedAt(Instant.now());

        return toResponse(rentalBookingRepository.save(booking));
    }

    @Transactional
    public RentalBookingDto.RentalBookingResponse cancelBooking(Long bookingId) {
        User user = getCurrentUser();

        RentalBooking booking = rentalBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Rental booking not found"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("You are not allowed to cancel this booking");
        }

        if (booking.getBookingStatus() == RentalBookingStatus.DISPATCHED ||
                booking.getBookingStatus() == RentalBookingStatus.RENTED ||
                booking.getBookingStatus() == RentalBookingStatus.RETURNED ||
                booking.getBookingStatus() == RentalBookingStatus.COMPLETED) {
            throw new RuntimeException("This booking cannot be cancelled at this stage");
        }

        booking.setBookingStatus(RentalBookingStatus.CANCELLED);
        booking.setPaymentStatus(RentalPaymentStatus.CANCELLED);

        return toResponse(rentalBookingRepository.save(booking));
    }

    private void validateRequest(RentalBookingDto.CreateRentalBookingRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Rental cart is empty");
        }

        if (request.getDeliveryAddress() == null) {
            throw new RuntimeException("Delivery address is required");
        }

        validateDates(request.getRentalStartDate(), request.getRentalEndDate());
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new RuntimeException("Rental start and end dates are required");
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new RuntimeException("Rental start date cannot be in the past");
        }

        if (endDate.isBefore(startDate)) {
            throw new RuntimeException("Rental end date cannot be before start date");
        }
    }

    private int calculateDays(LocalDate startDate, LocalDate endDate) {
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    private int getAvailableUnitsForProduct(
            Product product,
            LocalDate startDate,
            LocalDate endDate
    ) {
        int bookedUnits = getBookedUnits(product.getId(), startDate, endDate);
        return Math.max(product.getTotalUnits() - bookedUnits, 0);
    }

    private int getBookedUnits(
            Long productId,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Integer bookedUnits =
                rentalBookingRepository.countBookedQuantityForDateRange(
                        productId,
                        startDate,
                        endDate,
                        ACTIVE_BOOKING_STATUSES
                );

        return bookedUnits == null ? 0 : bookedUnits;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Logged user not found"));
    }

    private String generateBookingNumber() {
        String date = DateTimeFormatter.ofPattern("yyyyMMdd")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now());

        long count = rentalBookingRepository.count() + 1;

        return "RKG-RNT-" + date + "-" + String.format("%04d", count);
    }

    private RentalBookingDto.RentalBookingResponse toResponse(RentalBooking booking) {
        RentalBookingDto.RentalBookingResponse response =
                new RentalBookingDto.RentalBookingResponse();

        response.setBookingId(booking.getId());
        response.setBookingNumber(booking.getBookingNumber());
        response.setCustomerName(
                booking.getUser().getFirstName() + " " + booking.getUser().getLastName()
        );
        response.setCustomerEmail(booking.getUser().getEmail());

        response.setRentalStartDate(booking.getRentalStartDate());
        response.setRentalEndDate(booking.getRentalEndDate());
        response.setTotalDays(booking.getTotalDays());

        response.setSubtotal(booking.getSubtotal());
        response.setDeliveryCharge(booking.getDeliveryCharge());
        response.setTotalAmount(booking.getTotalAmount());
        response.setAdvanceAmount(booking.getAdvanceAmount());
        response.setRemainingAmount(booking.getRemainingAmount());

        response.setBookingStatus(booking.getBookingStatus().name());
        response.setPaymentStatus(booking.getPaymentStatus().name());

        response.setDeliveryFullName(booking.getDeliveryFullName());
        response.setDeliveryPhone(booking.getDeliveryPhone());
        response.setDeliveryAddressLine1(booking.getDeliveryAddressLine1());
        response.setDeliveryAddressLine2(booking.getDeliveryAddressLine2());
        response.setDeliveryCity(booking.getDeliveryCity());
        response.setDeliveryDistrict(booking.getDeliveryDistrict());
        response.setDeliveryPostalCode(booking.getDeliveryPostalCode());

        response.setCourierName(booking.getCourierName());
        response.setTrackingNumber(booking.getTrackingNumber());

        response.setCreatedAt(booking.getCreatedAt());
        response.setAdvancePaidAt(booking.getAdvancePaidAt());
        response.setDispatchedAt(booking.getDispatchedAt());
        response.setReturnedAt(booking.getReturnedAt());
        response.setCompletedAt(booking.getCompletedAt());

        response.setItems(
                booking.getItems()
                        .stream()
                        .map(item -> {
                            RentalBookingDto.RentalBookingItemResponse itemResponse =
                                    new RentalBookingDto.RentalBookingItemResponse();

                            itemResponse.setProductDbId(item.getProductDbId());
                            itemResponse.setProductId(item.getProductId());
                            itemResponse.setProductName(item.getProductName());
                            itemResponse.setImageUrl(item.getImageUrl());
                            itemResponse.setDailyRate(item.getDailyRate());
                            itemResponse.setQuantity(item.getQuantity());
                            itemResponse.setLineTotal(item.getLineTotal());

                            return itemResponse;
                        })
                        .toList()
        );

        return response;
    }
}