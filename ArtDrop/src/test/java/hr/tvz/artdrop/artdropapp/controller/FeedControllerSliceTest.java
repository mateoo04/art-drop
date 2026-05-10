package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import hr.tvz.artdrop.artdropapp.security.JwtAuthenticationFilter;
import hr.tvz.artdrop.artdropapp.service.ArtworkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FeedController.class)
@AutoConfigureMockMvc(addFilters = false)
class FeedControllerSliceTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserJpaRepository userRepository;
    @MockitoBean private ArtworkService artworkService;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void circleFeedUnauthenticatedReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/feed/circle"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void circleFeedAuthenticatedReturnsArray() throws Exception {
        User u = new User();
        u.setId(7L);
        u.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(u));
        when(artworkService.findCircleFeed(eq(7L), eq(20), eq(0))).thenReturn(List.of());

        mockMvc.perform(get("/api/feed/circle").principal(new org.springframework.security.authentication.TestingAuthenticationToken("alice", "n/a")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void circleFeedUnknownUserReturnsUnauthorized() throws Exception {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/feed/circle").principal(new org.springframework.security.authentication.TestingAuthenticationToken("ghost", "n/a")))
                .andExpect(status().isUnauthorized());
    }
}
