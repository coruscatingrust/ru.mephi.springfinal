package ru.mephi.hotel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import ru.mephi.hotel.domain.Hotel;
import ru.mephi.hotel.domain.Room;
import ru.mephi.hotel.domain.RoomLock;
import ru.mephi.hotel.repo.HotelRepository;
import ru.mephi.hotel.repo.RoomLockRepository;
import ru.mephi.hotel.repo.RoomRepository;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RoomLockRepositoryTest {

    @Autowired HotelRepository hotels;
    @Autowired RoomRepository rooms;
    @Autowired RoomLockRepository locks;

    @Test
    void overlaps_detected() {
        Hotel h = hotels.save(Hotel.builder().name("H").address("A").build());
        Room r = rooms.save(Room.builder().hotel(h).number("101").available(true).timesBooked(0).build());

        RoomLock l = RoomLock.builder()
                .room(r)
                .startDate(LocalDate.of(2025, 11, 1))
                .endDate(LocalDate.of(2025, 11, 5))
                .bookingId("B1")
                .requestId("R1")
                .build();
        locks.save(l);

        List<RoomLock> overl = locks.findOverlaps(r,
                LocalDate.of(2025,11,2), LocalDate.of(2025,11,6));
        assertThat(overl).hasSize(1);
    }
}
