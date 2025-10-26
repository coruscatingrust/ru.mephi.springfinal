package ru.mephi.hotel.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(indexes = {
        @Index(name = "idx_room_lock_room", columnList = "room_id"),
        @Index(name = "idx_room_lock_booking", columnList = "booking_id", unique = true)
})
public class RoomLock {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    private LocalDate startDate;
    private LocalDate endDate;

    /** bookingId for correlation across services. */
    @Column(name = "booking_id")
    private String bookingId;

    /** requestId for idempotency of confirm call. */
    @Column(name = "request_id", unique = true, nullable = false)
    private String requestId;
}
