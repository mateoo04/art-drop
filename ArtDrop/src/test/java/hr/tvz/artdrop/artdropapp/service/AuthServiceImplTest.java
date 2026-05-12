package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.JwtResponse;
import hr.tvz.artdrop.artdropapp.dto.LoginRequest;
import hr.tvz.artdrop.artdropapp.dto.RegisterRequest;
import hr.tvz.artdrop.artdropapp.exception.DuplicateEmailException;
import hr.tvz.artdrop.artdropapp.model.Authority;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.AuthorityJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import hr.tvz.artdrop.artdropapp.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private final AuthenticationManager authManager = mock(AuthenticationManager.class);
    private final JwtTokenProvider tokenProvider = mock(JwtTokenProvider.class);
    private final UserJpaRepository userRepo = mock(UserJpaRepository.class);
    private final AuthorityJpaRepository authorityRepo = mock(AuthorityJpaRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    private final AuthServiceImpl svc = new AuthServiceImpl(
            authManager, tokenProvider, userRepo, authorityRepo, passwordEncoder
    );

    @Test
    void login_returnsJwtFromTokenProvider() {
        Authentication auth = new UsernamePasswordAuthenticationToken("joe", "pw");
        when(authManager.authenticate(any())).thenReturn(auth);
        when(tokenProvider.generateToken(auth)).thenReturn("the-token");

        JwtResponse response = svc.login(new LoginRequest("joe", "pw"));

        assertThat(response.accessToken()).isEqualTo("the-token");
    }

    @Test
    void signup_throwsWhenEmailExists() {
        when(userRepo.existsByEmail("joe@example.com")).thenReturn(true);

        assertThatThrownBy(() -> svc.signup(new RegisterRequest("joe", "joe@example.com", "password1", "Joe")))
                .isInstanceOf(DuplicateEmailException.class)
                .hasMessageContaining("email already exists");
    }

    @Test
    void signup_createsUserWithFreshUsernameAndSlug() {
        when(userRepo.existsByEmail("joe@example.com")).thenReturn(false);
        when(authorityRepo.findByName("ROLE_USER")).thenReturn(Optional.of(new Authority(1L, "ROLE_USER")));
        when(userRepo.existsByUsername("joe")).thenReturn(false);
        when(userRepo.existsBySlug("joe")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed");
        when(authManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("joe", "password1"));
        when(tokenProvider.generateToken(any())).thenReturn("tok");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        JwtResponse response = svc.signup(new RegisterRequest("joe", "joe@example.com", "password1", "Joe"));

        assertThat(response.accessToken()).isEqualTo("tok");
        verify(userRepo).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("joe");
        assertThat(saved.getSlug()).isEqualTo("joe");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getDisplayName()).isEqualTo("Joe");
    }

    @Test
    void signup_createsRoleUserAuthorityWhenMissing() {
        when(userRepo.existsByEmail(anyString())).thenReturn(false);
        when(authorityRepo.findByName("ROLE_USER")).thenReturn(Optional.empty());
        when(authorityRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepo.existsByUsername(anyString())).thenReturn(false);
        when(userRepo.existsBySlug(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(authManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("joe", "pw"));
        when(tokenProvider.generateToken(any())).thenReturn("t");

        svc.signup(new RegisterRequest("joe", "joe@example.com", "password1", "Joe"));

        verify(authorityRepo).save(any(Authority.class));
    }

    @Test
    void signup_incrementsUsernameSuffixUntilUnique() {
        when(userRepo.existsByEmail(anyString())).thenReturn(false);
        when(authorityRepo.findByName("ROLE_USER")).thenReturn(Optional.of(new Authority(1L, "ROLE_USER")));
        when(userRepo.existsByUsername("jacob")).thenReturn(true);
        when(userRepo.existsByUsername("jacob2")).thenReturn(true);
        when(userRepo.existsByUsername("jacob3")).thenReturn(false);
        when(userRepo.existsBySlug(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(authManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("jacob3", "pw"));
        when(tokenProvider.generateToken(any())).thenReturn("t");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        svc.signup(new RegisterRequest("jacob", "jacob@example.com", "password1", "Jacob"));

        verify(userRepo).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("jacob3");
    }

    @Test
    void signup_incrementsSlugSuffixUntilUnique() {
        when(userRepo.existsByEmail(anyString())).thenReturn(false);
        when(authorityRepo.findByName("ROLE_USER")).thenReturn(Optional.of(new Authority(1L, "ROLE_USER")));
        when(userRepo.existsByUsername("jacob")).thenReturn(false);
        when(userRepo.existsBySlug("jacob")).thenReturn(true);
        when(userRepo.existsBySlug("jacob-2")).thenReturn(true);
        when(userRepo.existsBySlug("jacob-3")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(authManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("jacob", "pw"));
        when(tokenProvider.generateToken(any())).thenReturn("t");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        svc.signup(new RegisterRequest("jacob", "j@example.com", "password1", "Jacob"));

        verify(userRepo).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("jacob-3");
    }

    @Test
    void signup_slugUsesFallbackWhenUsernameHasNoAlphanumerics() {
        when(userRepo.existsByEmail(anyString())).thenReturn(false);
        when(authorityRepo.findByName("ROLE_USER")).thenReturn(Optional.of(new Authority(1L, "ROLE_USER")));
        when(userRepo.existsByUsername("@@@")).thenReturn(false);
        when(userRepo.existsBySlug("user")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(authManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("@@@", "pw"));
        when(tokenProvider.generateToken(any())).thenReturn("t");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        svc.signup(new RegisterRequest("@@@", "x@example.com", "password1", "X"));

        verify(userRepo).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("user");
    }

    @Test
    void signup_slugNormalizesUppercaseAndSpecialChars() {
        when(userRepo.existsByEmail(anyString())).thenReturn(false);
        when(authorityRepo.findByName("ROLE_USER")).thenReturn(Optional.of(new Authority(1L, "ROLE_USER")));
        when(userRepo.existsByUsername("Joe.Smith")).thenReturn(false);
        when(userRepo.existsBySlug("joe-smith")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(authManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken("Joe.Smith", "pw"));
        when(tokenProvider.generateToken(any())).thenReturn("t");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

        svc.signup(new RegisterRequest("Joe.Smith", "joe@example.com", "password1", "Joe"));

        verify(userRepo).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("joe-smith");
    }
}
