package hr.tvz.artdrop.artdropapp;

import tools.jackson.databind.ObjectMapper;
import hr.tvz.artdrop.artdropapp.dto.ArtworkCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkImageCommand;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
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

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class ArtworkControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ArtworkJpaRepository artworkRepository;
    @Autowired private UserJpaRepository userRepository;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private Long existingArtworkId;
    private String existingArtworkTitle;

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        Artwork seed = artworkRepository.findAll().stream().findFirst().orElseThrow();
        existingArtworkId = seed.getId();
        existingArtworkTitle = seed.getTitle();
    }

    @Test
    void listArtworksAnonymouslyReturnsOk() throws Exception {
        mockMvc.perform(get("/api/artworks").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listArtworksFilteredByMedium() throws Exception {
        String medium = artworkRepository.findById(existingArtworkId).orElseThrow().getMedium();
        mockMvc.perform(get("/api/artworks").param("medium", medium))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getMediumsReturnsList() throws Exception {
        mockMvc.perform(get("/api/artworks/mediums"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getArtworkByIdExistingReturnsOk() throws Exception {
        mockMvc.perform(get("/api/artworks/id/" + existingArtworkId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingArtworkId));
    }

    @Test
    void getArtworkByIdMissingReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/artworks/id/9999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getArtworkByTitleExistingReturnsOk() throws Exception {
        mockMvc.perform(get("/api/artworks/title/" + existingArtworkTitle))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(existingArtworkTitle));
    }

    @Test
    void getArtworkByTitleMissingReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/artworks/title/__definitely_missing_title__"))
                .andExpect(status().isNotFound());
    }

    @Test
    void searchArtworksReturnsOk() throws Exception {
        mockMvc.perform(get("/api/artworks/search").param("q", existingArtworkTitle.substring(0, 1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createArtworkUnauthenticatedReturnsUnauthorized() throws Exception {
        ArtworkCommand cmd = newValidCommand("Untitled draft", "PAINTING");
        mockMvc.perform(post("/api/artworks")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void createArtworkValidReturnsCreated() throws Exception {
        ArtworkCommand cmd = newValidCommand("Lab9 Test Artwork " + System.nanoTime(), "PAINTING");
        mockMvc.perform(post("/api/artworks")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(cmd.title()));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void createArtworkInvalidPayloadReturnsBadRequest() throws Exception {
        // Missing required title and images
        String body = "{\"medium\":\"PAINTING\"}";
        mockMvc.perform(post("/api/artworks")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void likeArtworkReturnsCreatedThenNoContent() throws Exception {
        mockMvc.perform(post("/api/artworks/" + existingArtworkId + "/likes"))
                .andExpect(status().is2xxSuccessful());
        mockMvc.perform(post("/api/artworks/" + existingArtworkId + "/likes"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void likeMissingArtworkReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/artworks/9999999/likes"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void unlikeArtworkReturnsNoContent() throws Exception {
        mockMvc.perform(post("/api/artworks/" + existingArtworkId + "/likes"));
        mockMvc.perform(delete("/api/artworks/" + existingArtworkId + "/likes"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void updateArtworkOwnNonPriceFieldsReturnsOk() throws Exception {
        Long userId = userRepository.findByUsername("user").orElseThrow().getId();
        Artwork own = artworkRepository.findAll().stream()
                .filter(a -> a.getAuthor() != null && userId.equals(a.getAuthor().getId()))
                .findFirst().orElseThrow();
        String body = "{\"description\":\"Updated by lab9 test\"}";
        mockMvc.perform(patch("/api/artworks/" + own.getId())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void updateMissingArtworkReturnsNotFound() throws Exception {
        mockMvc.perform(patch("/api/artworks/9999999")
                        .contentType("application/json")
                        .content("{\"description\":\"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getEligibleChallengesAuthenticatedReturnsOk() throws Exception {
        mockMvc.perform(get("/api/artworks/" + existingArtworkId + "/eligible-challenges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private ArtworkCommand newValidCommand(String title, String medium) {
        return new ArtworkCommand(
                title,
                medium,
                "Test description",
                List.of(new ArtworkImageCommand("test/public-id", 0, true, null)),
                null, null, null, null, null, null,
                null, null, null, null, null
        );
    }
}
