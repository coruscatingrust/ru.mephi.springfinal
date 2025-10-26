package ru.mephi.hotel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ru.mephi.hotel.repo.HotelRepository;
import ru.mephi.hotel.repo.RoomRepository;
import ru.mephi.hotel.domain.Hotel;
import ru.mephi.hotel.domain.Room;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class RoomFlowTest {

    @Autowired MockMvc mvc;
    @Autowired HotelRepository hotels;
    @Autowired RoomRepository rooms;

    Long hotelId;
    Long roomId;

    @BeforeEach
    void seed() {
        rooms.deleteAll();
        hotels.deleteAll();
        Hotel h = hotels.save(Hotel.builder().name("H").address("A").build());
        hotelId = h.getId();
        Room r1 = rooms.save(Room.builder().hotel(h).number("101").available(true).timesBooked(0).build());
        rooms.save(Room.builder().hotel(h).number("102").available(true).timesBooked(5).build());
        roomId = r1.getId();
    }

    @Test
    void recommend_public_sortedByTimesBooked() throws Exception {
        mvc.perform(get("/api/rooms/recommend")
                        .param("start", "2025-11-01")
                        .param("end", "2025-11-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].number").value("101"));
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void confirm_then_idempotent_then_overlap409() throws Exception {
        // confirm ok
        mvc.perform(post("/api/rooms/{id}/confirm-availability", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2025-11-01\",\"endDate\":\"2025-11-05\",\"bookingId\":\"B1\",\"requestId\":\"R1\"}"))
                .andExpect(status().isOk());

        // idempotent ok
        mvc.perform(post("/api/rooms/{id}/confirm-availability", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2025-11-01\",\"endDate\":\"2025-11-05\",\"bookingId\":\"B1\",\"requestId\":\"R1\"}"))
                .andExpect(status().isOk());

        // overlap -> 409
        mvc.perform(post("/api/rooms/{id}/confirm-availability", roomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2025-11-02\",\"endDate\":\"2025-11-06\",\"bookingId\":\"B2\",\"requestId\":\"R2\"}"))
                .andExpect(status().isConflict());

        // release ok
        mvc.perform(post("/api/rooms/{id}/release", roomId)
                        .param("bookingId", "B1"))
                .andExpect(status().isOk());
    }
}
