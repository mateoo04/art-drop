package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ArtworkCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.dto.ArtworkUpdateCommand;

import java.util.List;
import java.util.Optional;

public interface ArtworkService {

    List<ArtworkDTO> findAll(String viewerUsername);

    List<ArtworkDTO> findAll(String viewerUsername, int limit, int offset);

    Optional<ArtworkDTO> findById(Long id, String viewerUsername);

    List<ArtworkDTO> findByMedium(String val, String viewerUsername);

    List<ArtworkDTO> findByMedium(String val, String viewerUsername, int limit, int offset);

    List<ArtworkDTO> searchArtworks(String query, String viewerUsername, int limit, int offset);

    List<ArtworkDTO> findByAuthorId(Long authorId, String viewerUsername);

    List<ArtworkDTO> findByAuthorId(Long authorId, String viewerUsername, int limit, int offset);

    List<ArtworkDTO> findCircleFeed(Long viewerId, int limit, int offset);

    List<ArtworkDTO> findByIdsOrdered(List<Long> ids, String viewerUsername);

    Optional<ArtworkDTO> findOneByTitle(String title, String viewerUsername);

    boolean createArtwork(ArtworkCommand command);

    enum CreateOutcome {
        CREATED,
        CONFLICT,
        FORBIDDEN_SALE_GATE,
        UNAUTHENTICATED,
        CHALLENGE_NOT_FOUND,
        CHALLENGE_NOT_ACTIVE,
        CHALLENGE_USER_ALREADY_HAS_ENTRY
    }

    record CreateResult(CreateOutcome outcome, ArtworkDTO artwork) {}

    CreateResult createArtwork(ArtworkCommand command, String authorUsername);

    enum LikeResult { LIKED, ALREADY_LIKED, UNLIKED, NOT_LIKED, NOT_FOUND, UNAUTHENTICATED }

    LikeResult like(Long artworkId, String username);

    LikeResult unlike(Long artworkId, String username);

    enum UpdateOutcome { OK, NOT_FOUND, FORBIDDEN_SALE_GATE }

    record UpdateResult(UpdateOutcome outcome, ArtworkDTO artwork) {}

    UpdateResult updateArtwork(Long id, ArtworkUpdateCommand command, String editorUsername);

    boolean deleteByTitle(String title);

    List<String> findDistinctMediums();
}
