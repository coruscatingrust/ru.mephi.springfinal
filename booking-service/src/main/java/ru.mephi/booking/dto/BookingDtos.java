package ru.mephi.booking.dto;

import ru.mephi.booking.domain.BookingStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class BookingDtos {

    public record CreateBookingRequest(
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            Long roomId,
            Boolean autoSelect // if true, ignore roomId
    ) {}

    public record BookingResponse(
            Long id, String bookingUid, Long roomId, LocalDate startDate, LocalDate endDate, BookingStatus status
    ) {}
}
