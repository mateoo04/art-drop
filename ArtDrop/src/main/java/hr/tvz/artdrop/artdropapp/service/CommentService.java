package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.CommentDTO;
import hr.tvz.artdrop.artdropapp.dto.CreateCommentCommand;

import java.util.List;
import java.util.Optional;

public interface CommentService {

    List<CommentDTO> listForArtwork(Long artworkId, String viewerUsername, int limit, int offset);

    Optional<CommentDTO> create(Long artworkId, String authorUsername, CreateCommentCommand command);

    enum DeleteResult { OK, NOT_FOUND, FORBIDDEN }

    DeleteResult delete(Long commentId, String requesterUsername);
}
