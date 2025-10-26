package ru.mephi.booking.service;

import ru.mephi.booking.domain.RequestLog;
import ru.mephi.booking.repo.RequestLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class IdempotencyService {

    private final RequestLogRepository requestLogRepository;

    public IdempotencyService(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    public boolean exists(String requestId) {
        return requestLogRepository.findByRequestId(requestId).isPresent();
    }

    @Transactional
    public void remember(String requestId) {
        if (!exists(requestId)) {
            requestLogRepository.save(RequestLog.builder().requestId(requestId).createdAt(OffsetDateTime.now()).build());
        }
    }
}
