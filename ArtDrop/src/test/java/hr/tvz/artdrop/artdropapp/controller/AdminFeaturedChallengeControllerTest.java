package hr.tvz.artdrop.artdropapp.controller;

import tools.jackson.databind.ObjectMapper;
import hr.tvz.artdrop.artdropapp.dto.ScheduleReplacementRequest;
import hr.tvz.artdrop.artdropapp.dto.SetFeaturedCurrentRequest;
import hr.tvz.artdrop.artdropapp.model.Challenge;
import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import hr.tvz.artdrop.artdropapp.model.FeaturedTriggerType;
import hr.tvz.artdrop.artdropapp.repository.ChallengeJpaRepository;
import hr.tvz.artdrop.artdropapp.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AdminFeaturedChallengeControllerTest extends AbstractPostgresIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper mapper;
    @Autowired private ChallengeJpaRepository challengeRepo;

    private MockMvc mvc;

    @BeforeEach
    void setup() {
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void putCurrent_setsTheFeatured() throws Exception {
        Challenge c = save("Test");
        var body = mapper.writeValueAsString(new SetFeaturedCurrentRequest(c.getId()));

        mvc.perform(put("/api/admin/featured-challenge/current")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current.id").value(c.getId()));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void schedule_atTime_succeeds() throws Exception {
        Challenge a = save("A");
        Challenge b = save("B");
        mvc.perform(put("/api/admin/featured-challenge/current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new SetFeaturedCurrentRequest(a.getId()))))
                .andExpect(status().isOk());

        var req = new ScheduleReplacementRequest(b.getId(), FeaturedTriggerType.AT_TIME,
                LocalDateTime.now().plusHours(1));
        mvc.perform(put("/api/admin/featured-challenge/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.next.id").value(b.getId()))
                .andExpect(jsonPath("$.triggerType").value("AT_TIME"));
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void schedule_atTimeWithoutTriggerAt_returns400() throws Exception {
        Challenge a = save("A");
        Challenge b = save("B");
        mvc.perform(put("/api/admin/featured-challenge/current")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new SetFeaturedCurrentRequest(a.getId()))));

        var req = new ScheduleReplacementRequest(b.getId(), FeaturedTriggerType.AT_TIME, null);
        mvc.perform(put("/api/admin/featured-challenge/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void get_unauthenticated_returns401or403() throws Exception {
        mvc.perform(get("/api/admin/featured-challenge"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status != 401 && status != 403) {
                        throw new AssertionError("Expected 401 or 403, got " + status);
                    }
                });
    }

    private Challenge save(String title) {
        Challenge c = new Challenge();
        c.setTitle(title);
        c.setStatus(ChallengeStatus.ACTIVE);
        c.setStartsAt(LocalDateTime.now().minusDays(1));
        c.setEndsAt(LocalDateTime.now().plusDays(7));
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return challengeRepo.save(c);
    }
}
