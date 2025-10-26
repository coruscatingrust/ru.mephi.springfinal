package ru.mephi.booking.controller;

import ru.mephi.booking.dto.BookingDtos;
import ru.mephi.booking.exception.UnauthorizedException;
import ru.mephi.booking.service.BookingServiceFacade;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class BookingController {

    private final BookingServiceFacade bookingService;

    public BookingController(BookingServiceFacade bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/booking")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public BookingDtos.BookingResponse create(@Valid @RequestBody BookingDtos.CreateBookingRequest req,
                                              Authentication auth,
                                              @RequestHeader(name = "Authorization") String authHeader,
                                              @RequestHeader(name = "X-Request-Id", required = false) String requestId) {
        if (auth == null) throw new UnauthorizedException("No auth");
        Long userId = Math.abs(auth.getName().hashCode()) * 1L; // demo-only
        if (requestId == null || requestId.isBlank()) requestId = java.util.UUID.randomUUID().toString();
        return bookingService.create(userId, req, authHeader, requestId);
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public List<BookingDtos.BookingResponse> list(Authentication auth) {
        Long userId = Math.abs(auth.getName().hashCode()) * 1L;
        return bookingService.listByUser(userId);
    }

    @GetMapping("/booking/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public BookingDtos.BookingResponse get(@PathVariable Long id, Authentication auth) {
        Long userId = Math.abs(auth.getName().hashCode()) * 1L;
        return bookingService.get(id, userId);
    }

    @DeleteMapping("/booking/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public void cancel(@PathVariable Long id,
                       Authentication auth,
                       @RequestHeader(name = "Authorization") String authHeader,
                       @RequestHeader(name = "X-Request-Id", required = false) String requestId) {
        Long userId = Math.abs(auth.getName().hashCode()) * 1L;
        if (requestId == null || requestId.isBlank()) requestId = java.util.UUID.randomUUID().toString();
        bookingService.cancel(id, userId, authHeader, requestId);
    }
}
