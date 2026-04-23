package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.UpdateProfileCommand;
import hr.tvz.artdrop.artdropapp.dto.UserProfileDTO;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.model.UserFollow;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserFollowJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserJpaRepository userRepository;
    private final ArtworkJpaRepository artworkRepository;
    private final UserFollowJpaRepository followRepository;

    public UserServiceImpl(
            UserJpaRepository userRepository,
            ArtworkJpaRepository artworkRepository,
            UserFollowJpaRepository followRepository
    ) {
        this.userRepository = userRepository;
        this.artworkRepository = artworkRepository;
        this.followRepository = followRepository;
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

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfileDTO> findBySlug(String slug, String viewerUsername) {
        return userRepository.findBySlug(slug).map(user -> {
            boolean isSelf = viewerUsername != null && viewerUsername.equals(user.getUsername());
            return toProfile(user, isSelf);
        });
    }

    @Override
    @Transactional
    public CircleAction joinCircle(String viewerUsername, String targetSlug) {
        Optional<User> viewer = userRepository.findByUsername(viewerUsername);
        Optional<User> target = userRepository.findBySlug(targetSlug);
        if (viewer.isEmpty() || target.isEmpty()) {
            return CircleAction.NOT_FOUND;
        }
        if (viewer.get().getId().equals(target.get().getId())) {
            return CircleAction.SELF;
        }
        if (!followRepository.existsByFollowerIdAndFolloweeId(viewer.get().getId(), target.get().getId())) {
            try {
                followRepository.save(new UserFollow(null, viewer.get().getId(), target.get().getId(), LocalDateTime.now()));
            } catch (DataIntegrityViolationException ignored) {
                // race: another request added it concurrently — idempotent success
            }
        }
        return CircleAction.OK;
    }

    @Override
    @Transactional
    public CircleAction leaveCircle(String viewerUsername, String targetSlug) {
        Optional<User> viewer = userRepository.findByUsername(viewerUsername);
        Optional<User> target = userRepository.findBySlug(targetSlug);
        if (viewer.isEmpty() || target.isEmpty()) {
            return CircleAction.NOT_FOUND;
        }
        if (viewer.get().getId().equals(target.get().getId())) {
            return CircleAction.SELF;
        }
        followRepository.deleteByFollowerAndFollowee(viewer.get().getId(), target.get().getId());
        return CircleAction.OK;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Boolean> circleStatus(String viewerUsername, String targetSlug) {
        Optional<User> viewer = userRepository.findByUsername(viewerUsername);
        Optional<User> target = userRepository.findBySlug(targetSlug);
        if (viewer.isEmpty() || target.isEmpty()) {
            return Optional.empty();
        }
        if (viewer.get().getId().equals(target.get().getId())) {
            return Optional.of(false);
        }
        return Optional.of(followRepository.existsByFollowerIdAndFolloweeId(viewer.get().getId(), target.get().getId()));
    }

    private UserProfileDTO toProfile(User user, boolean isSelf) {
        int artworkCount = (int) artworkRepository.countByAuthor_Id(user.getId());
        Integer circleSize = isSelf ? (int) followRepository.countByFolloweeId(user.getId()) : null;
        Integer followingCount = isSelf ? (int) followRepository.countByFollowerId(user.getId()) : null;
        return new UserProfileDTO(
                user.getId(),
                user.getUsername(),
                user.getSlug(),
                user.getDisplayName(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                artworkCount,
                circleSize,
                followingCount,
                isSelf
        );
    }
}
