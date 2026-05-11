package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.SaleState;
import hr.tvz.artdrop.artdropapp.model.SaleType;
import hr.tvz.artdrop.artdropapp.model.User;
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

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class ReservationControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ArtworkJpaRepository artworkRepo;
    @Autowired private UserJpaRepository userRepo;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        User u = userRepo.findByUsername("user").orElseThrow();
        artworkRepo.findAll().stream()
                .filter(a -> u.getId().equals(a.getReservedByUserId()))
                .forEach(a -> {
                    a.setSaleState(SaleState.AVAILABLE);
                    a.setReservedByUserId(null);
                    a.setReservedUntil(null);
                    artworkRepo.save(a);
                });
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getReturns204WhenNoActiveReservation() throws Exception {
        mockMvc.perform(get("/api/reservations/me"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getReturnsDtoWhenActiveReservationExists() throws Exception {
        User u = userRepo.findByUsername("user").orElseThrow();
        Artwork a = artworkRepo.findAll().stream()
                .filter(x -> x.getSaleType() == SaleType.ORIGINAL
                        && !x.getAuthor().getId().equals(u.getId()))
                .findFirst().orElseThrow();
        a.setSaleState(SaleState.RESERVED);
        a.setReservedByUserId(u.getId());
        a.setReservedUntil(LocalDateTime.now().plusMinutes(10));
        artworkRepo.save(a);

        mockMvc.perform(get("/api/reservations/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.artworkId").value(a.getId()))
                .andExpect(jsonPath("$.title").value(a.getTitle()));
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void deleteReleasesReservation() throws Exception {
        User u = userRepo.findByUsername("user").orElseThrow();
        Artwork a = artworkRepo.findAll().stream()
                .filter(x -> x.getSaleType() == SaleType.ORIGINAL
                        && !x.getAuthor().getId().equals(u.getId()))
                .findFirst().orElseThrow();
        a.setSaleState(SaleState.RESERVED);
        a.setReservedByUserId(u.getId());
        a.setReservedUntil(LocalDateTime.now().plusMinutes(10));
        artworkRepo.save(a);

        mockMvc.perform(delete("/api/reservations/me"))
                .andExpect(status().isNoContent());

        Artwork after = artworkRepo.findById(a.getId()).orElseThrow();
        assertThat(after.getSaleState()).isEqualTo(SaleState.AVAILABLE);
        assertThat(after.getReservedByUserId()).isNull();
    }
}
