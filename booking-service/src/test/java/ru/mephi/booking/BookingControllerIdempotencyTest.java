package ru.mephi.booking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ru.mephi.booking.service.HotelClient;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BookingControllerIdempotencyTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    HotelClient hotelClient;

    @BeforeEach
    void setupHotelClient() {
        List<HotelClient.Room> rooms = List.of(new HotelClient.Room(1L, 1L, "101", true, 0));
        Mockito.when(hotelClient.recommend(
                Mockito.any(LocalDate.class), Mockito.any(LocalDate.class),
                Mockito.anyString(), Mockito.anyString()
        )).thenReturn(rooms);

        Mockito.doNothing().when(hotelClient).confirm(
                Mockito.anyLong(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString());
        Mockito.doNothing().when(hotelClient).release(
                Mockito.anyLong(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
    }

    @Test
    @WithMockUser(username = "alice", roles = {"USER"})
    void createBooking_idempotency_secondCall409() throws Exception {
        String start = "2025-11-01";
        String end   = "2025-11-05";

        // 1-й вызов — ОК (добавлен Authorization)
        mvc.perform(post("/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Test")
                        .header("X-Request-Id", "idem-1")
                        .content("{\"startDate\":\""+start+"\",\"endDate\":\""+end+"\",\"autoSelect\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingUid", not(isEmptyOrNullString())));

        // 2-й вызов с тем же X-Request-Id — ожидаем 409 (добавлен Authorization)
        mvc.perform(post("/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Test")
                        .header("X-Request-Id", "idem-1")
                        .content("{\"startDate\":\""+start+"\",\"endDate\":\""+end+"\",\"autoSelect\":true}"))
                .andExpect(status().isConflict());
    }

    @Test
    void createBooking_unauthorized_401() throws Exception {
        // специально без Authorization — ожидаем 401
        mvc.perform(post("/booking")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2025-11-01\",\"endDate\":\"2025-11-05\",\"autoSelect\":true}"))
                .andExpect(status().isUnauthorized());
    }
}
