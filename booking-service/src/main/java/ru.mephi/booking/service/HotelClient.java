package ru.mephi.booking.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Component
public class HotelClient {

    private final WebClient webClient;

    public HotelClient(WebClient.Builder builder) {
        // Eureka/LoadBalancer will resolve http://hotel-service
        this.webClient = builder.baseUrl("http://hotel-service").build();
    }

    public record Room(Long id, Long hotelId, String number, boolean available, long timesBooked) {}

    public List<Room> recommend(LocalDate start, LocalDate end, String authToken, String requestId) {
        return webClient.get()
                .uri(uri -> uri.path("/api/rooms/recommend").queryParam("start", start).queryParam("end", end).build())
                .headers(h -> { if (authToken != null) h.set(HttpHeaders.AUTHORIZATION, authToken);
                                if (requestId != null) h.set("X-Request-Id", requestId); })
                .retrieve()
                .bodyToFlux(Room.class)
                .collectList()
                .retryWhen(Retry.backoff(2, Duration.ofMillis(200)))
                .block();
    }

    public void confirm(Long roomId, LocalDate start, LocalDate end, String bookingId, String requestId, String authToken) {
        webClient.post()
                .uri(uri -> uri.path("/api/rooms/{id}/confirm-availability").build(roomId))
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .header("X-Request-Id", requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "startDate", start.toString(),
                        "endDate", end.toString(),
                        "bookingId", bookingId,
                        "requestId", requestId
                ))
                .retrieve()
                .toBodilessEntity()
                .retryWhen(Retry.backoff(2, Duration.ofMillis(200)))
                .block();
    }

    public void release(Long roomId, String bookingId, String authToken, String requestId) {
        webClient.post()
                .uri(uri -> uri.path("/api/rooms/{id}/release").queryParam("bookingId", bookingId).build(roomId))
                .header(HttpHeaders.AUTHORIZATION, authToken)
                .header("X-Request-Id", requestId)
                .retrieve()
                .toBodilessEntity()
                .retryWhen(Retry.backoff(2, Duration.ofMillis(200)))
                .block();
    }
}
