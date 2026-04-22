package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.ArtworkCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkCommentCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkDTO;
import hr.tvz.artdrop.artdropapp.dto.ArtworkLikeCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkReviewCommand;
import hr.tvz.artdrop.artdropapp.dto.ArtworkUpdateCommand;

import java.util.List;
import java.util.Optional;

public interface ArtworkService {

    List<ArtworkDTO> findAll();

    Optional<ArtworkDTO> findById(Long id);

    List<ArtworkDTO> findByMedium(String val);

    Optional<ArtworkDTO> findOneByTitle(String title);

    boolean createArtwork(ArtworkCommand command);

    boolean createArtworkLike(ArtworkLikeCommand command);

    boolean createArtworkComment(ArtworkCommentCommand command);

    boolean createArtworkReview(ArtworkReviewCommand command);

    Optional<ArtworkDTO> updateArtwork(Long id, ArtworkUpdateCommand command);

    boolean deleteByTitle(String title);
}
