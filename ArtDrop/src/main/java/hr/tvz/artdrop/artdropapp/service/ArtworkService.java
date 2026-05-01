package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ArtworkCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkCommentCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.dto.ArtworkReviewCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkUpdateCommand;

import java.util.List;
import java.util.Optional;

public interface ArtworkService {

    List<ArtworkDTO> findAll(String viewerUsername);

    List<ArtworkDTO> findAll(String viewerUsername, int limit, int offset);

    Optional<ArtworkDTO> findById(Long id, String viewerUsername);

    List<ArtworkDTO> findByMedium(String val, String viewerUsername);

    List<ArtworkDTO> findByMedium(String val, String viewerUsername, int limit, int offset);

    List<ArtworkDTO> findByAuthorId(Long authorId, String viewerUsername);

    List<ArtworkDTO> findCircleFeed(Long viewerId, int limit, int offset);

    Optional<ArtworkDTO> findOneByTitle(String title, String viewerUsername);

    boolean createArtwork(ArtworkCommand command);

    enum LikeResult { LIKED, ALREADY_LIKED, UNLIKED, NOT_LIKED, NOT_FOUND, UNAUTHENTICATED }

    LikeResult like(Long artworkId, String username);

    LikeResult unlike(Long artworkId, String username);

    boolean createArtworkComment(ArtworkCommentCommand command);

    boolean createArtworkReview(ArtworkReviewCommand command);

    Optional<ArtworkDTO> updateArtwork(Long id, ArtworkUpdateCommand command);

    boolean deleteByTitle(String title);

    List<String> findDistinctMediums();
}
