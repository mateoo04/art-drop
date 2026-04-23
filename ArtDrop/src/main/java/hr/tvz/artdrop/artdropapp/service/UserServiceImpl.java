package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.UpdateProfileCommand;
import hr.tvz.artdrop.artdropapp.dto.UserProfileDTO;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserJpaRepository userRepository;
    private final ArtworkJpaRepository artworkRepository;

    public UserServiceImpl(UserJpaRepository userRepository, ArtworkJpaRepository artworkRepository) {
        this.userRepository = userRepository;
        this.artworkRepository = artworkRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfileDTO> findMe(String username) {
        return userRepository.findByUsername(username).map(user -> toProfile(user, true));
    }

    @Override
    @Transactional
    public Optional<UserProfileDTO> updateMe(String username, UpdateProfileCommand command) {
        Optional<User> maybeUser = userRepository.findByUsername(username);
        if (maybeUser.isEmpty()) {
            return Optional.empty();
        }
        User user = maybeUser.get();
        if (command.displayName() != null && !command.displayName().isBlank()) {
            user.setDisplayName(command.displayName().trim());
        }
        if (command.bio() != null) {
            user.setBio(command.bio().isBlank() ? null : command.bio());
        }
        if (command.avatarUrl() != null) {
            user.setAvatarUrl(command.avatarUrl().isBlank() ? null : command.avatarUrl());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        return Optional.of(toProfile(user, true));
    }

    private UserProfileDTO toProfile(User user, boolean isSelf) {
        int artworkCount = (int) artworkRepository.countByAuthor_Id(user.getId());
        return new UserProfileDTO(
                user.getId(),
                user.getUsername(),
                user.getSlug(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                artworkCount,
                isSelf ? 0 : null,
                isSelf ? 0 : null,
                isSelf
        );
    }
}
