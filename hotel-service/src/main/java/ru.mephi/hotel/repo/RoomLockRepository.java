package ru.mephi.hotel.repo;

import ru.mephi.hotel.domain.Room;
import ru.mephi.hotel.domain.RoomLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomLockRepository extends JpaRepository<RoomLock, Long> {

    @Query("select rl from RoomLock rl where rl.room = :room and not (rl.endDate <= :start or rl.startDate >= :end)")
    List<RoomLock> findOverlaps(Room room, LocalDate start, LocalDate end);

    Optional<RoomLock> findByBookingId(String bookingId);

    Optional<RoomLock> findByRequestId(String requestId);
}
