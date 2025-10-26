package ru.mephi.booking.repo;

import ru.mephi.booking.domain.RequestLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {
    Optional<RequestLog> findByRequestId(String requestId);
}
