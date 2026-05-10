package hr.tvz.artdrop.artdropapp;

import hr.tvz.artdrop.artdropapp.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class UserControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private WebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void searchUsersReturnsArray() throws Exception {
        mockMvc.perform(get("/api/users/search").param("q", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getMeUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getMeAuthenticatedReturnsProfile() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void updateMeValidReturnsOk() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .contentType("application/json")
                        .content("{\"bio\":\"Updated bio for lab 9\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Updated bio for lab 9"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void updateMeShortDisplayNameReturnsBadRequest() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .contentType("application/json")
                        .content("{\"displayName\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getMyArtworksReturnsArray() throws Exception {
        mockMvc.perform(get("/api/users/me/artworks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getUserBySlugExistingReturnsOk() throws Exception {
        mockMvc.perform(get("/api/users/julian-vane"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("julian-vane"));
    }

    @Test
    void getUserByMissingSlugReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/users/__missing__"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getArtworksBySlugReturnsArray() throws Exception {
        mockMvc.perform(get("/api/users/julian-vane/artworks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void circleStatusReturnsOk() throws Exception {
        mockMvc.perform(get("/api/users/mateo/circle-status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inCircle").exists());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void joinAndLeaveCircleHappyPath() throws Exception {
        mockMvc.perform(post("/api/users/mateo/circle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inCircle").value(true));
        mockMvc.perform(delete("/api/users/mateo/circle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inCircle").value(false));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void joinSelfCircleReturnsBadRequest() throws Exception {
        // 'user' is the seed user; trying to follow themselves
        mockMvc.perform(post("/api/users/julian-vane/circle"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void joinMissingUserCircleReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/users/__missing__/circle"))
                .andExpect(status().isNotFound());
    }
}
