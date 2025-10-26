package ru.mephi.hotel.repo;

import ru.mephi.hotel.domain.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("select r from Room r where r.available = true")
    List<Room> findAllAvailable();
}
