package ru.mephi.hotel.controller;

import ru.mephi.hotel.dto.HotelDto;
import ru.mephi.hotel.service.HotelService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    private final HotelService hotelService;

    public HotelController(HotelService hotelService) {
        this.hotelService = hotelService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public HotelDto create(@Valid @RequestBody HotelDto dto) {
        return hotelService.create(dto);
    }

    @GetMapping
    public List<HotelDto> list() {
        return hotelService.list();
    }
}
