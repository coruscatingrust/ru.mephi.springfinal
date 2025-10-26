package ru.mephi.hotel.controller;

import ru.mephi.hotel.dto.ConfirmAvailabilityRequest;
import ru.mephi.hotel.dto.RoomDto;
import ru.mephi.hotel.service.HotelService;
import ru.mephi.hotel.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final HotelService hotelService;
    private final RoomService roomService;

    public RoomController(HotelService hotelService, RoomService roomService) {
        this.hotelService = hotelService;
        this.roomService = roomService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public RoomDto add(@RequestParam Long hotelId,
                       @RequestParam String number,
                       @RequestParam(defaultValue = "true") boolean available) {
        return roomService.addRoom(hotelService.getOrThrow(hotelId), number, available);
    }

    @GetMapping
    public List<RoomDto> list(@RequestParam LocalDate start, @RequestParam LocalDate end) {
        return roomService.listAvailable(start, end);
    }

    @GetMapping("/recommend")
    public List<RoomDto> recommend(@RequestParam LocalDate start, @RequestParam LocalDate end) {
        return roomService.recommend(start, end);
    }

    // INTERNAL (called by booking service)
    @PostMapping("/{id}/confirm-availability")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public void confirm(@PathVariable Long id, @Valid @RequestBody ConfirmAvailabilityRequest req) {
        roomService.confirmAvailability(id, req);
    }

    // INTERNAL compensation
    @PostMapping("/{id}/release")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public void release(@PathVariable Long id, @RequestParam String bookingId) {
        roomService.release(id, bookingId);
    }
}
