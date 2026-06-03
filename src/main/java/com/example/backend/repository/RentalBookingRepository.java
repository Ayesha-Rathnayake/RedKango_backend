package com.example.backend.repository;

import com.example.backend.domain.RentalBooking;
import com.example.backend.domain.RentalBookingStatus;
import com.example.backend.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RentalBookingRepository extends JpaRepository<RentalBooking, Long> {

    Optional<RentalBooking> findByBookingNumber(String bookingNumber);

    List<RentalBooking> findByUserOrderByCreatedAtDesc(User user);

    @Query("""
        SELECT COALESCE(SUM(i.quantity), 0)
        FROM RentalBooking b
        JOIN b.items i
        WHERE i.productDbId = :productId
        AND b.bookingStatus IN :statuses
        AND b.rentalStartDate <= :endDate
        AND b.rentalEndDate >= :startDate
    """)
    Integer countBookedQuantityForDateRange(
            Long productId,
            LocalDate startDate,
            LocalDate endDate,
            Collection<RentalBookingStatus> statuses
    );


}