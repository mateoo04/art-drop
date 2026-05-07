package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ArtistSummaryDTO;
import hr.tvz.artdrop.artdropapp.dto.UpdateProfileCommand;
import hr.tvz.artdrop.artdropapp.dto.UserProfileDTO;

import java.util.List;
import java.util.Optional;

public interface UserService {

    Optional<UserProfileDTO> findMe(String username);

    Optional<UserProfileDTO> updateMe(String username, UpdateProfileCommand command);

    Optional<UserProfileDTO> findBySlug(String slug, String viewerUsername);

    List<ArtistSummaryDTO> searchUsers(String query, int limit, int offset);

    enum CircleAction { OK, NOT_FOUND, SELF }

    CircleAction joinCircle(String viewerUsername, String targetSlug);

    CircleAction leaveCircle(String viewerUsername, String targetSlug);

    Optional<Boolean> circleStatus(String viewerUsername, String targetSlug);
}
