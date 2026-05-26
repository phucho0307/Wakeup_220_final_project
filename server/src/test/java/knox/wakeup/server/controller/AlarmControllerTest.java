package knox.wakeup.server.controller;

import knox.wakeup.server.service.AlarmService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AlarmControllerTest {

    @Autowired MockMvc mvc;
    @Autowired AlarmService service;

    @Test
    void unknownUserIsIdle() throws Exception {
        mvc.perform(get("/alarm/status").param("user", "ghost"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("idle"));
    }

    @Test
    void setRequiresPositiveSeconds() throws Exception {
        mvc.perform(post("/alarm/set").param("user", "phuc").param("seconds", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
        mvc.perform(post("/alarm/set").param("user", "phuc").param("seconds", "-3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void setThenStatusReturnsArmed() throws Exception {
        mvc.perform(post("/alarm/set").param("user", "alice").param("seconds", "60"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value("armed"));
        mvc.perform(get("/alarm/status").param("user", "alice"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("armed"));
    }

    @Test
    void cancelRequiresArmed() throws Exception {
        mvc.perform(post("/alarm/cancel").param("user", "nobody"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void solveOnIdleIsBadRequest() throws Exception {
        mvc.perform(post("/alarm/solve").param("user", "bob")
                        .contentType("application/json").content("{\"answer\": 1}"))
                .andExpect(status().isBadRequest());
    }
}
