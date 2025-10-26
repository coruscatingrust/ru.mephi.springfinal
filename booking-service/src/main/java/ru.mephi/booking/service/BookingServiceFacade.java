package ru.mephi.booking.service;

import ru.mephi.booking.domain.Booking;
import ru.mephi.booking.domain.BookingStatus;
import ru.mephi.booking.dto.BookingDtos;
import ru.mephi.booking.exception.ConflictException;
import ru.mephi.booking.exception.NotFoundException;
import ru.mephi.booking.repo.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BookingServiceFacade {

    private static final Logger log = LoggerFactory.getLogger(BookingServiceFacade.class);

    private final BookingRepository bookingRepository;
    private final HotelClient hotelClient;
    private final IdempotencyService idempotency;

    public BookingServiceFacade(BookingRepository bookingRepository, HotelClient hotelClient, IdempotencyService idempotency) {
        this.bookingRepository = bookingRepository;
        this.hotelClient = hotelClient;
        this.idempotency = idempotency;
    }

    public List<BookingDtos.BookingResponse> listByUser(Long userId) {
        return bookingRepository.findByUserId(userId).stream().map(this::toDto).toList();
    }

    public BookingDtos.BookingResponse get(Long id, Long userId) {
        Booking b = bookingRepository.findById(id).orElseThrow(() -> new NotFoundException("Booking " + id + " not found"));
        if (!b.getUserId().equals(userId)) throw new NotFoundException("Booking " + id + " not found");
        return toDto(b);
    }

    @Transactional
    public BookingDtos.BookingResponse create(Long userId, BookingDtos.CreateBookingRequest req, String authHeader, String requestId) {
        if (req.startDate().isAfter(req.endDate()) || req.startDate().isBefore(LocalDate.now())) {
            throw new ConflictException("Invalid dates");
        }

        if (idempotency.exists(requestId)) {
            return bookingRepository.findByStatus(BookingStatus.PENDING).stream()
                    .filter(b -> b.getUserId().equals(userId))
                    .findFirst().map(this::toDto)
                    .orElseThrow(() -> new ConflictException("Duplicate request"));
        }
        idempotency.remember(requestId);

        Long roomId = req.roomId();
        if (Boolean.TRUE.equals(req.autoSelect())) {
            var rooms = hotelClient.recommend(req.startDate(), req.endDate(), authHeader, requestId);
            if (rooms == null || rooms.isEmpty()) throw new ConflictException("No rooms available");
            roomId = rooms.get(0).id();
        }
        if (roomId == null) throw new ConflictException("roomId required if autoSelect=false");

        String bookingUid = UUID.randomUUID().toString();
        Booking booking = Booking.builder()
                .userId(userId)
                .roomId(roomId)
                .startDate(req.startDate())
                .endDate(req.endDate())
                .status(BookingStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .bookingUid(bookingUid)
                .build();
        booking = bookingRepository.save(booking);

        try {
            hotelClient.confirm(roomId, req.startDate(), req.endDate(), bookingUid, requestId, authHeader);
            booking.setStatus(BookingStatus.CONFIRMED);
        } catch (Exception ex) {
            log.warn("Confirm failed: {}. Compensating...", ex.getMessage());
            booking.setStatus(BookingStatus.CANCELLED);
            try { hotelClient.release(roomId, bookingUid, authHeader, requestId); } catch (Exception ignored) {}
        }
        return toDto(booking);
    }

    @Transactional
    public void cancel(Long id, Long userId, String authHeader, String requestId) {
        Booking b = bookingRepository.findById(id).orElseThrow(() -> new NotFoundException("Booking " + id + " not found"));
        if (!b.getUserId().equals(userId)) throw new NotFoundException("Booking " + id + " not found");
        if (b.getStatus() == BookingStatus.CONFIRMED) {
            try { hotelClient.release(b.getRoomId(), b.getBookingUid(), authHeader, requestId); } catch (Exception ignored) {}
        }
        b.setStatus(BookingStatus.CANCELLED);
    }

    private BookingDtos.BookingResponse toDto(Booking b) {
        return new BookingDtos.BookingResponse(b.getId(), b.getBookingUid(), b.getRoomId(), b.getStartDate(), b.getEndDate(), b.getStatus());
    }
}
