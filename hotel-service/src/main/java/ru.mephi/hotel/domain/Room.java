package ru.mephi.hotel.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    private String number;
    private boolean available = true;

    /**
     * How many times this room has been booked (for fairness).
     */
    @Column(name = "times_booked")
    private long timesBooked = 0;
}
