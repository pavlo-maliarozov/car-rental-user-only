package com.example.rental;

import com.example.rental.dto.auth.LoginRequest;
import com.example.rental.dto.auth.SignupRequest;
import com.example.rental.dto.reservation.ReservationCreateRequest;
import com.example.rental.dto.reservation.ReservationUpdateRequest;
import com.example.rental.model.CarType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests aligned with user-only API:
 *  - POST /api/auth/signup
 *  - POST /api/auth/login
 *  - GET  /api/availability?carType=...&startAt=...&days=...
 *  - POST /api/reservations
 *  - PUT  /api/reservations/{id}
 *  - DELETE /api/reservations/{id}
 *  - GET  /api/reservations/my
 *
 * Notes:
 *  - Uses H2 with ddl-auto:create-drop in test profile.
 *  - Seeds capacity ONCE per class with @Sql BEFORE_TEST_CLASS, so availability > 0.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Sql(
        statements = {
                // Seed minimal capacities so users can book
                "INSERT INTO capacities (car_type, quantity) VALUES ('SEDAN', 1)",
                "INSERT INTO capacities (car_type, quantity) VALUES ('SUV', 1)",
                "INSERT INTO capacities (car_type, quantity) VALUES ('VAN', 1)"
        },
        executionPhase = ExecutionPhase.BEFORE_TEST_CLASS
)
public class IntegrationFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    private String tokenFrom(String json) throws Exception {
        return om.readTree(json).get("token").asText();
    }

    private Instant futureInstantHoursFromNow(int hours) {
        return Instant.now().plus(hours, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS);
    }

    @Test
    @Order(1)
    void availability_accepts_friendly_and_rejects_invalid_types() throws Exception {
        // Signup a user to obtain a token
        var signup = new SignupRequest("types@test.com", "pw");
        var token = tokenFrom(
                mvc.perform(post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(om.writeValueAsString(signup)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        Instant start = futureInstantHoursFromNow(1);

        // Friendly lower-case should work (server parses with CarType.from(...))
        mvc.perform(get("/api/availability")
                        .param("carType", "sedan")                 // lower-case
                        .param("startAt", start.toString())        // ISO-8601 (Instant#toString)
                        .param("days", "2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carType", is("sedan")))
                .andExpect(jsonPath("$.days", is(2)));

        // Invalid types should return 400 with a clear message
        mvc.perform(get("/api/availability")
                        .param("carType", "crossover")
                        .param("startAt", start.toString())
                        .param("days", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Unknown carType: crossover")));

        mvc.perform(get("/api/availability")
                        .param("carType", "motorbike")
                        .param("startAt", start.toString())
                        .param("days", "1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Unknown carType: motorbike")));
    }

    @Test
    @Order(2)
    void user_signup_login_and_create_conflict_flow() throws Exception {
        // user1 signup
        var u1 = new SignupRequest("u1@example.com", "pw");
        var tok1 = tokenFrom(
                mvc.perform(post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(om.writeValueAsString(u1)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        Instant start = futureInstantHoursFromNow(1);

        // Availability should be 1 (SEDAN capacity = 1 from @Sql seed)
        mvc.perform(get("/api/availability")
                        .param("carType", "sedan")
                        .param("startAt", start.toString())
                        .param("days", "2")
                        .header("Authorization", "Bearer " + tok1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available", is(1)));

        // user1 creates a reservation
        var r1 = new ReservationCreateRequest(CarType.SEDAN, start, 2);
        mvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(r1))
                        .header("Authorization", "Bearer " + tok1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")));

        // user2 signup and attempt same window -> expect 409
        var u2 = new SignupRequest("u2@example.com", "pw");
        var tok2 = tokenFrom(
                mvc.perform(post("/api/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(om.writeValueAsString(u2)))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        var r2 = new ReservationCreateRequest(CarType.SEDAN, start, 2);
        mvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(r2))
                        .header("Authorization", "Bearer " + tok2))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(3)
    void edit_then_cancel_flow() throws Exception {
        // login user1 (from previous test)
        var loginJson = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new LoginRequest("u1@example.com", "pw"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        var tok1 = tokenFrom(loginJson);

        // list my reservations
        var listJson = mvc.perform(get("/api/reservations/my")
                        .header("Authorization", "Bearer " + tok1))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long id = om.readTree(listJson).get(0).get("id").asLong();

        // edit to a new future date (3 days ahead)
        var newStart = futureInstantHoursFromNow(24 * 3);
        var upd = new ReservationUpdateRequest(CarType.SEDAN, newStart, 1);

        mvc.perform(put("/api/reservations/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(upd))
                        .header("Authorization", "Bearer " + tok1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days", is(1)));

        // cancel (allow any 2xx)
        mvc.perform(delete("/api/reservations/{id}", id)
                        .header("Authorization", "Bearer " + tok1))
                .andExpect(status().is2xxSuccessful());
    }
}
