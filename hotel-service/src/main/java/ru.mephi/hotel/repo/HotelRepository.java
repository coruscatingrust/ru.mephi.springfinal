package ru.mephi.hotel.repo;

import ru.mephi.hotel.domain.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
}
