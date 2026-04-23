package hr.tvz.artdrop.artdropapp.security;

import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserJpaRepository userJpaRepository;

    public CustomUserDetailsService(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        User user = userJpaRepository.findByUsername(identifier)
                .or(() -> userJpaRepository.findByEmail(identifier))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));

        Set<GrantedAuthority> authorities = user.getAuthorities()
                .stream()
                .map(authority -> new SimpleGrantedAuthority(authority.getName()))
                .collect(Collectors.toSet());

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authorities)
                .build();
    }
}
