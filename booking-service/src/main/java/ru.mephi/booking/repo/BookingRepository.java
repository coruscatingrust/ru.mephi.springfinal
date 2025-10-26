package ru.mephi.booking.repo;

import ru.mephi.booking.domain.Booking;
import ru.mephi.booking.domain.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUserId(Long userId);
    Optional<Booking> findByBookingUid(String uid);
    List<Booking> findByStatus(BookingStatus status);
}
