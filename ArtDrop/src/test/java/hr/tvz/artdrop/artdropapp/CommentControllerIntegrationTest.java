package hr.tvz.artdrop.artdropapp;

import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.CommentJpaRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class CommentControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ArtworkJpaRepository artworkRepository;
    @Autowired private CommentJpaRepository commentRepository;

    private MockMvc mockMvc;
    private Long artworkId;

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        artworkId = artworkRepository.findAll().stream().findFirst().orElseThrow().getId();
    }

    @Test
    void listCommentsAnonymouslyReturnsOk() throws Exception {
        mockMvc.perform(get("/api/artworks/" + artworkId + "/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void createCommentValidReturnsCreated() throws Exception {
        mockMvc.perform(post("/api/artworks/" + artworkId + "/comments")
                        .contentType("application/json")
                        .content("{\"text\":\"Lab9 test comment\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.text").value("Lab9 test comment"));
    }

    @Test
    void createCommentUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/artworks/" + artworkId + "/comments")
                        .contentType("application/json")
                        .content("{\"text\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void createCommentBlankTextReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/artworks/" + artworkId + "/comments")
                        .contentType("application/json")
                        .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void createCommentOnMissingArtworkReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/artworks/9999999/comments")
                        .contentType("application/json")
                        .content("{\"text\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void deleteOwnCommentReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/artworks/" + artworkId + "/comments")
                        .contentType("application/json")
                        .content("{\"text\":\"to delete\"}"))
                .andExpect(status().isCreated());
        long createdId = commentRepository.findAll().stream()
                .filter(c -> "to delete".equals(c.getText()))
                .findFirst().orElseThrow().getId();
        mockMvc.perform(delete("/api/comments/" + createdId))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void deleteMissingCommentReturnsNotFound() throws Exception {
        mockMvc.perform(delete("/api/comments/9999999"))
                .andExpect(status().isNotFound());
    }
}
