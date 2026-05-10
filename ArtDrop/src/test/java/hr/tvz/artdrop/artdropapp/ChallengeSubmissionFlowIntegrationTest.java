package hr.tvz.artdrop.artdropapp;

import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.Challenge;
import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import hr.tvz.artdrop.artdropapp.model.ChallengeSubmission;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.ChallengeJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.ChallengeSubmissionJpaRepository;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class ChallengeSubmissionFlowIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ChallengeJpaRepository challengeRepository;
    @Autowired private ChallengeSubmissionJpaRepository submissionRepository;
    @Autowired private ArtworkJpaRepository artworkRepository;
    @Autowired private UserJpaRepository userRepository;

    private MockMvc mockMvc;
    private Long userArtworkId;
    private Long otherUserArtworkId;

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        submissionRepository.deleteAll();
        challengeRepository.deleteAll();

        User user = userRepository.findByUsername("user").orElseThrow();
        userArtworkId = artworkRepository.findAll().stream()
                .filter(a -> a.getAuthor() != null && user.getId().equals(a.getAuthor().getId()))
                .findFirst().orElseThrow().getId();
        otherUserArtworkId = artworkRepository.findAll().stream()
                .filter(a -> a.getAuthor() != null && !user.getId().equals(a.getAuthor().getId()))
                .findFirst().orElseThrow().getId();
    }

    private Challenge persistChallenge(ChallengeStatus status, LocalDateTime startsAt) {
        Challenge c = new Challenge();
        c.setTitle("Test Challenge");
        c.setStatus(status);
        c.setStartsAt(startsAt);
        c.setEndsAt(LocalDateTime.now().plusDays(7));
        c.setCreatedAt(LocalDateTime.now());
        c.setUpdatedAt(LocalDateTime.now());
        return challengeRepository.save(c);
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void submitOwnArtworkToActiveChallenge_returnsCreated() throws Exception {
        Challenge c = persistChallenge(ChallengeStatus.ACTIVE, LocalDateTime.now().minusDays(30));
        mockMvc.perform(post("/api/challenges/" + c.getId() + "/submissions")
                        .contentType("application/json")
                        .content("{\"artworkId\":" + userArtworkId + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.submissionId").exists());
        assertThat(submissionRepository.findByChallengeIdAndArtworkId(c.getId(), userArtworkId)).isPresent();
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void submitNotOwnArtwork_returnsForbidden() throws Exception {
        Challenge c = persistChallenge(ChallengeStatus.ACTIVE, LocalDateTime.now().minusDays(30));
        mockMvc.perform(post("/api/challenges/" + c.getId() + "/submissions")
                        .contentType("application/json")
                        .content("{\"artworkId\":" + otherUserArtworkId + "}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("NOT_OWNER"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void submitToUpcomingChallenge_returnsForbidden() throws Exception {
        Challenge c = persistChallenge(ChallengeStatus.UPCOMING, LocalDateTime.now().plusDays(1));
        mockMvc.perform(post("/api/challenges/" + c.getId() + "/submissions")
                        .contentType("application/json")
                        .content("{\"artworkId\":" + userArtworkId + "}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("CHALLENGE_NOT_ACTIVE"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void submitArtworkPublishedBeforeChallengeStart_returnsForbidden() throws Exception {
        Challenge c = persistChallenge(ChallengeStatus.ACTIVE, LocalDateTime.now().plusDays(1));
        mockMvc.perform(post("/api/challenges/" + c.getId() + "/submissions")
                        .contentType("application/json")
                        .content("{\"artworkId\":" + userArtworkId + "}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ARTWORK_TOO_OLD"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void submitSamePairTwice_returnsConflict() throws Exception {
        Challenge c = persistChallenge(ChallengeStatus.ACTIVE, LocalDateTime.now().minusDays(30));
        mockMvc.perform(post("/api/challenges/" + c.getId() + "/submissions")
                        .contentType("application/json")
                        .content("{\"artworkId\":" + userArtworkId + "}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/challenges/" + c.getId() + "/submissions")
                        .contentType("application/json")
                        .content("{\"artworkId\":" + userArtworkId + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ALREADY_SUBMITTED"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void submitToSecondChallengeWhileInOther_returnsConflict() throws Exception {
        Challenge c1 = persistChallenge(ChallengeStatus.ACTIVE, LocalDateTime.now().minusDays(30));
        Challenge c2 = persistChallenge(ChallengeStatus.ACTIVE, LocalDateTime.now().minusDays(30));
        mockMvc.perform(post("/api/challenges/" + c1.getId() + "/submissions")
                        .contentType("application/json")
                        .content("{\"artworkId\":" + userArtworkId + "}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/challenges/" + c2.getId() + "/submissions")
                        .contentType("application/json")
                        .content("{\"artworkId\":" + userArtworkId + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("IN_OTHER_CHALLENGE"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void withdrawOwnSubmission_returnsNoContent() throws Exception {
        Challenge c = persistChallenge(ChallengeStatus.ACTIVE, LocalDateTime.now().minusDays(30));
        Artwork a = artworkRepository.findById(userArtworkId).orElseThrow();
        Long userId = userRepository.findByUsername("user").orElseThrow().getId();
        submissionRepository.save(new ChallengeSubmission(null, c, a, userId, LocalDateTime.now()));

        mockMvc.perform(delete("/api/challenges/" + c.getId() + "/submissions/" + userArtworkId))
                .andExpect(status().isNoContent());
        assertThat(submissionRepository.findByChallengeIdAndArtworkId(c.getId(), userArtworkId)).isEmpty();
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void eligibleArtworksOnlyShowsUsersOwnNotInActiveChallenges() throws Exception {
        Challenge c = persistChallenge(ChallengeStatus.ACTIVE, LocalDateTime.now().minusDays(30));
        mockMvc.perform(get("/api/challenges/" + c.getId() + "/eligible-artworks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + userArtworkId + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + otherUserArtworkId + ")]").doesNotExist());
    }
}
