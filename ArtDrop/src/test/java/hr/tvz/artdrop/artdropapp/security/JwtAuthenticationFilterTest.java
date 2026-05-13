package hr.tvz.artdrop.artdropapp.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private final JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
    private final UserDetailsService userDetailsService = mock(UserDetailsService.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(tokenProvider, userDetailsService);

    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void setup() {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_noCookies_continuesWithoutAuth() throws Exception {
        when(request.getCookies()).thenReturn(null);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_noJwtCookie_continuesWithoutAuth() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("other", "value") });

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_invalidJwtCookie_continuesWithoutAuth() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("jwt", "bad-token") });
        when(tokenProvider.validateToken("bad-token")).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_validJwtCookie_setsAuthentication() throws Exception {
        UserDetails userDetails = new User("joe", "x", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("jwt", "good-token") });
        when(tokenProvider.validateToken("good-token")).thenReturn(true);
        when(tokenProvider.getUsernameFromToken("good-token")).thenReturn("joe");
        when(userDetailsService.loadUserByUsername("joe")).thenReturn(userDetails);

        filter.doFilter(request, response, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(userDetails);
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilter_validTokenButUserMissing_continuesWithoutAuth() throws Exception {
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("jwt", "good-token") });
        when(tokenProvider.validateToken("good-token")).thenReturn(true);
        when(tokenProvider.getUsernameFromToken("good-token")).thenReturn("ghost");
        when(userDetailsService.loadUserByUsername(eq("ghost")))
                .thenThrow(new UsernameNotFoundException("nope"));

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }
}
