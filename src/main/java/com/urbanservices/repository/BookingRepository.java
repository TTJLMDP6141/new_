package com.urbanservices.repository;

import com.urbanservices.model.Booking;
import com.urbanservices.model.BookingStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);
    List<Booking> findByProviderProfile_IdOrderByBookingDateAscBookingTimeAsc(Long providerId);
    List<Booking> findTop8ByOrderByCreatedAtDesc();
    Optional<Booking> findByIdAndCustomer_Id(Long id, Long customerId);
    Optional<Booking> findByIdAndProviderProfile_Id(Long id, Long providerId);
    long countByStatus(BookingStatus status);
    boolean existsByService_Id(Long serviceId);
}
