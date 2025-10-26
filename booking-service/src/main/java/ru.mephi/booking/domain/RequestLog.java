package ru.mephi.booking.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "request_log", indexes = @Index(name = "idx_request_id", columnList = "requestId", unique = true))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RequestLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String requestId;
    private OffsetDateTime createdAt;
}
