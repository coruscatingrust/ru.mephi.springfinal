package ru.mephi.hotel.service;

import ru.mephi.hotel.domain.Hotel;
import ru.mephi.hotel.dto.HotelDto;
import ru.mephi.hotel.exception.NotFoundException;
import ru.mephi.hotel.repo.HotelRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HotelService {
    private final HotelRepository hotelRepository;

    public HotelService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    public HotelDto create(HotelDto dto) {
        Hotel h = Hotel.builder().name(dto.name()).address(dto.address()).build();
        h = hotelRepository.save(h);
        return new HotelDto(h.getId(), h.getName(), h.getAddress());
    }

    public List<HotelDto> list() {
        return hotelRepository.findAll().stream()
                .map(h -> new HotelDto(h.getId(), h.getName(), h.getAddress()))
                .toList();
    }

    public Hotel getOrThrow(Long id) {
        return hotelRepository.findById(id).orElseThrow(() -> new NotFoundException("Hotel " + id + " not found"));
    }
}
