package hr.tvz.artdrop.artdropapp;

import tools.jackson.databind.ObjectMapper;
import hr.tvz.artdrop.artdropapp.dto.RevokeSellerCommand;
import hr.tvz.artdrop.artdropapp.dto.SubmitSellerApplicationCommand;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.SaleStatus;
import hr.tvz.artdrop.artdropapp.model.SellerApplication;
import hr.tvz.artdrop.artdropapp.model.SellerApplicationStatus;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.SellerApplicationJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class SellerFlowIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private SellerApplicationJpaRepository applicationRepository;
    @Autowired private UserJpaRepository userRepository;
    @Autowired private ArtworkJpaRepository artworkRepository;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:sellertest;DB_CLOSE_DELAY=-1;MODE=LEGACY");
    }

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        // Ensure clean state across tests sharing the in-memory DB.
        applicationRepository.deleteAll();
        userRepository.findByUsername("user").ifPresent(u -> {
            if (u.getAuthorities().removeIf(a -> "ROLE_SELLER".equals(a.getName()))) {
                userRepository.save(u);
            }
        });
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void submitCreatesPendingApplication() throws Exception {
        applicationRepository.deleteAll();
        SubmitSellerApplicationCommand cmd =
                new SubmitSellerApplicationCommand("I want to sell because of my long-standing art career.");
        mockMvc.perform(post("/api/users/me/seller-application")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void doublePendingReturnsConflict() throws Exception {
        applicationRepository.deleteAll();
        SubmitSellerApplicationCommand cmd =
                new SubmitSellerApplicationCommand("First application with enough characters to pass validation.");
        mockMvc.perform(post("/api/users/me/seller-application")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/users/me/seller-application")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ALREADY_PENDING"));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void cooldownBlocksReapplyAfterRecentRejection() throws Exception {
        applicationRepository.deleteAll();
        Long userId = userRepository.findByUsername("user").orElseThrow().getId();
        SellerApplication recent = new SellerApplication();
        recent.setUserId(userId);
        recent.setMessage("Previously rejected message of sufficient length to pass validation.");
        recent.setStatus(SellerApplicationStatus.REJECTED);
        recent.setSubmittedAt(LocalDateTime.now().minusDays(1));
        recent.setDecidedAt(LocalDateTime.now().minusDays(1));
        applicationRepository.save(recent);

        SubmitSellerApplicationCommand cmd =
                new SubmitSellerApplicationCommand("Trying again before the cooldown elapses, please consider me.");
        MvcResult res = mockMvc.perform(post("/api/users/me/seller-application")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("COOLDOWN_ACTIVE"))
                .andReturn();
        assertThat(res.getResponse().getContentAsString()).contains("canReapplyAt");
    }

    @Test
    @WithMockUser(username = "mateo", roles = {"ADMIN"})
    void approveGrantsRoleSeller() throws Exception {
        applicationRepository.deleteAll();
        Long userId = userRepository.findByUsername("user").orElseThrow().getId();
        SellerApplication pending = new SellerApplication();
        pending.setUserId(userId);
        pending.setMessage("Pending application from user awaiting admin decision and review.");
        pending.setStatus(SellerApplicationStatus.PENDING);
        pending.setSubmittedAt(LocalDateTime.now());
        pending = applicationRepository.save(pending);

        mockMvc.perform(post("/api/admin/seller-applications/" + pending.getId() + "/approve")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        var refreshed = userRepository.findByUsername("user").orElseThrow();
        assertThat(refreshed.getAuthorities()).anyMatch(a -> "ROLE_SELLER".equals(a.getName()));
    }

    @Test
    @WithMockUser(username = "mateo", roles = {"ADMIN"})
    void revokeUnlistsArtworks() throws Exception {
        applicationRepository.deleteAll();
        Long userId = userRepository.findByUsername("user").orElseThrow().getId();

        // Approve first to grant ROLE_SELLER and create the approved row to revoke.
        SellerApplication pending = new SellerApplication();
        pending.setUserId(userId);
        pending.setMessage("Pending application from user awaiting admin decision and review.");
        pending.setStatus(SellerApplicationStatus.PENDING);
        pending.setSubmittedAt(LocalDateTime.now());
        pending = applicationRepository.save(pending);
        mockMvc.perform(post("/api/admin/seller-applications/" + pending.getId() + "/approve")
                .contentType("application/json").content("{}"))
                .andExpect(status().isOk());

        // Ensure user has at least one listed artwork.
        Artwork a = artworkRepository.findAll().stream()
                .filter(art -> art.getAuthor() != null && userId.equals(art.getAuthor().getId()))
                .findFirst().orElseThrow();
        a.setPrice(new BigDecimal("100.00"));
        a.setSaleStatus(SaleStatus.AVAILABLE);
        artworkRepository.save(a);
        long listedBefore = artworkRepository.countListedByAuthorId(userId);
        assertThat(listedBefore).isGreaterThanOrEqualTo(1);

        RevokeSellerCommand cmd = new RevokeSellerCommand("Reason for revoking seller status in this test.");
        mockMvc.perform(post("/api/admin/users/" + userId + "/revoke-seller")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(cmd)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unlistedCount").value((int) listedBefore));

        long listedAfter = artworkRepository.countListedByAuthorId(userId);
        assertThat(listedAfter).isZero();

        var refreshed = userRepository.findByUsername("user").orElseThrow();
        assertThat(refreshed.getAuthorities()).noneMatch(auth -> "ROLE_SELLER".equals(auth.getName()));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void nonSellerCannotSetPriceOnArtworkUpdate() throws Exception {
        Long userId = userRepository.findByUsername("user").orElseThrow().getId();
        // Strip ROLE_SELLER if present from earlier test ordering.
        var u = userRepository.findByUsername("user").orElseThrow();
        u.getAuthorities().removeIf(auth -> "ROLE_SELLER".equals(auth.getName()));
        userRepository.save(u);

        Artwork a = artworkRepository.findAll().stream()
                .filter(art -> art.getAuthor() != null && userId.equals(art.getAuthor().getId()))
                .findFirst().orElseThrow();

        String body = "{\"price\":250.00,\"saleStatus\":\"AVAILABLE\"}";
        mockMvc.perform(patch("/api/artworks/" + a.getId())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN_SALE_GATE"));
    }
}
