package hr.tvz.artdrop.artdropapp.controller;

import hr.tvz.artdrop.artdropapp.dto.HomeFeedResponse;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import hr.tvz.artdrop.artdropapp.security.JwtAuthenticationFilter;
import hr.tvz.artdrop.artdropapp.service.FeedRankingService;
import hr.tvz.artdrop.artdropapp.service.FeedSeenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = HomeFeedController.class)
@AutoConfigureMockMvc(addFilters = false)
class HomeFeedControllerSliceTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private FeedRankingService feedRankingService;
    @MockitoBean private FeedSeenService feedSeenService;
    @MockitoBean private UserJpaRepository userRepository;
    @MockitoBean private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void homeFeedAnonymousReturnsOkWithItems() throws Exception {
        when(feedRankingService.getHomeFeed(eq(null), eq(null), any(), any(), eq(20)))
                .thenReturn(new HomeFeedResponse(List.of(), null, false));

        mockMvc.perform(get("/api/feed/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void homeFeedPassesMediumAndCursorParams() throws Exception {
        when(feedRankingService.getHomeFeed(any(), any(), eq("PAINTING"), eq("c1"), eq(5)))
                .thenReturn(new HomeFeedResponse(List.of(), "next", true));

        mockMvc.perform(get("/api/feed/home")
                        .param("medium", "PAINTING")
                        .param("cursor", "c1")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextCursor").value("next"))
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    void seenWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/feed/seen")
                        .contentType("application/json")
                        .content("{\"artworkIds\":[1,2,3]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void seenWithAuthenticatedUserReturnsNoContent() throws Exception {
        User u = new User();
        u.setId(42L);
        u.setUsername("alice");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(u));

        mockMvc.perform(post("/api/feed/seen")
                        .contentType("application/json")
                        .content("{\"artworkIds\":[1,2,3]}")
                        .principal(new org.springframework.security.authentication.TestingAuthenticationToken("alice", "n/a")))
                .andExpect(status().isNoContent());

        verify(feedSeenService).recordSeen(eq(42L), eq(List.of(1L, 2L, 3L)));
    }

    @Test
    void seenWithUnknownUserReturnsUnauthorized() throws Exception {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/feed/seen")
                        .contentType("application/json")
                        .content("{\"artworkIds\":[1]}")
                        .principal(new org.springframework.security.authentication.TestingAuthenticationToken("ghost", "n/a")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void seenWithMissingArtworkIdsReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/feed/seen")
                        .contentType("application/json")
                        .content("{}")
                        .principal(new org.springframework.security.authentication.TestingAuthenticationToken("alice", "n/a")))
                .andExpect(status().isBadRequest());
    }
}
