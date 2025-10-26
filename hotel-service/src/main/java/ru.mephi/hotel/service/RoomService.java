package ru.mephi.hotel.service;

import ru.mephi.hotel.domain.Hotel;
import ru.mephi.hotel.domain.Room;
import ru.mephi.hotel.domain.RoomLock;
import ru.mephi.hotel.dto.ConfirmAvailabilityRequest;
import ru.mephi.hotel.dto.RoomDto;
import ru.mephi.hotel.exception.ConflictException;
import ru.mephi.hotel.exception.NotFoundException;
import ru.mephi.hotel.repo.RoomLockRepository;
import ru.mephi.hotel.repo.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomLockRepository roomLockRepository;

    public RoomService(RoomRepository roomRepository, RoomLockRepository roomLockRepository) {
        this.roomRepository = roomRepository;
        this.roomLockRepository = roomLockRepository;
    }

    public RoomDto addRoom(Hotel hotel, String number, boolean available) {
        Room r = Room.builder().hotel(hotel).number(number).available(available).timesBooked(0).build();
        r = roomRepository.save(r);
        return toDto(r);
    }

    public List<RoomDto> listAvailable(LocalDate start, LocalDate end) {
        return roomRepository.findAllAvailable().stream()
                .filter(r -> roomLockRepository.findOverlaps(r, start, end).isEmpty())
                .map(this::toDto)
                .toList();
    }

    public List<RoomDto> recommend(LocalDate start, LocalDate end) {
        return listAvailable(start, end).stream()
                .sorted(Comparator.comparingLong(RoomDto::timesBooked).thenComparing(RoomDto::id))
                .toList();
    }

    @Transactional
    public void confirmAvailability(Long roomId, ConfirmAvailabilityRequest req) {
        if (roomLockRepository.findByRequestId(req.requestId()).isPresent()) return; // идемпотентность
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room " + roomId + " not found"));
        if (!room.isAvailable()) throw new ConflictException("Room is not operational");
        var overlaps = roomLockRepository.findOverlaps(room, req.startDate(), req.endDate());
        if (!overlaps.isEmpty()) throw new ConflictException("Room already locked for the period");
        RoomLock lock = RoomLock.builder()
                .room(room)
                .startDate(req.startDate())
                .endDate(req.endDate())
                .bookingId(req.bookingId())
                .requestId(req.requestId())
                .build();
        roomLockRepository.save(lock);
        room.setTimesBooked(room.getTimesBooked() + 1); // fairness metric
    }

    @Transactional
    public void release(Long roomId, String bookingId) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Room " + roomId + " not found"));
        roomLockRepository.findByBookingId(bookingId).ifPresent(roomLockRepository::delete);
    }

    private RoomDto toDto(Room r) {
        return new RoomDto(r.getId(), r.getHotel().getId(), r.getNumber(), r.isAvailable(), r.getTimesBooked());
    }
}
