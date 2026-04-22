package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.JwtResponse;
import hr.tvz.artdrop.artdropapp.dto.LoginRequest;
import hr.tvz.artdrop.artdropapp.dto.RegisterRequest;
import hr.tvz.artdrop.artdropapp.model.Authority;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.AuthorityJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import hr.tvz.artdrop.artdropapp.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
public class AuthServiceImpl implements AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserJpaRepository userJpaRepository;
    private final AuthorityJpaRepository authorityJpaRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            UserJpaRepository userJpaRepository,
            AuthorityJpaRepository authorityJpaRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userJpaRepository = userJpaRepository;
        this.authorityJpaRepository = authorityJpaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public JwtResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );
        String token = jwtTokenProvider.generateToken(authentication);
        return new JwtResponse(token);
    }

    @Override
    public JwtResponse signup(RegisterRequest registerRequest) {
        if (userJpaRepository.existsByUsername(registerRequest.username())
                || userJpaRepository.existsByEmail(registerRequest.email())) {
            throw new IllegalArgumentException("Username or email already exists");
        }

        Authority roleUser = authorityJpaRepository.findByName("ROLE_USER")
                .orElseGet(() -> authorityJpaRepository.save(new Authority(null, "ROLE_USER")));

        String slug = generateUniqueSlug(registerRequest.username());
        LocalDateTime now = LocalDateTime.now();

        User user = new User();
        user.setUsername(registerRequest.username());
        user.setEmail(registerRequest.email());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.password()));
        user.setDisplayName(registerRequest.displayName());
        user.setSlug(slug);
        user.setBio(null);
        user.setAvatarUrl(null);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setAuthorities(Set.of(roleUser));
        userJpaRepository.save(user);

        return login(new LoginRequest(registerRequest.username(), registerRequest.password()));
    }

    private String generateUniqueSlug(String username) {
        String base = username.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (base.isBlank()) {
            base = "user";
        }
        if (!userJpaRepository.existsBySlug(base)) {
            return base;
        }

        int suffix = 2;
        while (userJpaRepository.existsBySlug(base + "-" + suffix)) {
            suffix++;
        }
        return base + "-" + suffix;
    }
}
