package ru.mephi.hotel;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HotelAdminSecurityTest {

    @Autowired
    MockMvc mvc;

    @Test
    void createHotel_anon_401or403() throws Exception {
        mvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"address\":\"Y\"}"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void createHotel_user_403() throws Exception {
        mvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\",\"address\":\"Y\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void createHotel_admin_ok() throws Exception {
        mvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"DemoHotel\",\"address\":\"Addr\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }
}
