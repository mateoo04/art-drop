package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.CommentDTO;
import hr.tvz.artdrop.artdropapp.dto.CreateCommentCommand;
import hr.tvz.artdrop.artdropapp.model.Artwork;
import hr.tvz.artdrop.artdropapp.model.Comment;
import hr.tvz.artdrop.artdropapp.model.User;
import hr.tvz.artdrop.artdropapp.repository.ArtworkJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.CommentJpaRepository;
import hr.tvz.artdrop.artdropapp.repository.UserJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentServiceImplTest {

    private final CommentJpaRepository commentRepo = mock(CommentJpaRepository.class);
    private final ArtworkJpaRepository artworkRepo = mock(ArtworkJpaRepository.class);
    private final UserJpaRepository userRepo = mock(UserJpaRepository.class);

    private final CommentServiceImpl svc = new CommentServiceImpl(commentRepo, artworkRepo, userRepo);

    private static User user(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setDisplayName(username);
        u.setSlug(username);
        return u;
    }

    private static Artwork artwork(Long id) {
        Artwork a = new Artwork();
        a.setId(id);
        return a;
    }

    private static Comment comment(Long id, Artwork art, User author, Long parentId, boolean deleted) {
        Comment c = new Comment();
        c.setId(id);
        c.setArtwork(art);
        c.setAuthor(author);
        c.setText("text " + id);
        c.setParentCommentId(parentId);
        c.setCreatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        c.setUpdatedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        c.setIsDeleted(deleted);
        return c;
    }

    // ----- listForArtwork -----

    @Test
    void listForArtwork_returnsEmptyWhenNoTopLevel() {
        when(commentRepo.findTopLevelByArtwork(eq(1L), any(PageRequest.class))).thenReturn(List.of());

        assertThat(svc.listForArtwork(1L, null, 10, 0)).isEmpty();
    }

    @Test
    void listForArtwork_clampsLimitAndOffset() {
        when(commentRepo.findTopLevelByArtwork(eq(1L), any(PageRequest.class))).thenReturn(List.of());

        svc.listForArtwork(1L, null, 999, -50);

        verify(commentRepo).findTopLevelByArtwork(eq(1L), any(PageRequest.class));
    }

    @Test
    void listForArtwork_anonymousViewerHasNoIsAuthorFlag() {
        Artwork art = artwork(1L);
        User author = user(10L, "joe");
        Comment c1 = comment(100L, art, author, null, false);
        when(commentRepo.findTopLevelByArtwork(eq(1L), any(PageRequest.class))).thenReturn(List.of(c1));
        when(commentRepo.findRepliesByParentIds(List.of(100L))).thenReturn(List.of());

        List<CommentDTO> result = svc.listForArtwork(1L, null, 10, 0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isAuthor()).isFalse();
    }

    @Test
    void listForArtwork_authorViewerHasIsAuthorFlag() {
        Artwork art = artwork(1L);
        User author = user(10L, "joe");
        Comment c1 = comment(100L, art, author, null, false);
        when(commentRepo.findTopLevelByArtwork(eq(1L), any(PageRequest.class))).thenReturn(List.of(c1));
        when(commentRepo.findRepliesByParentIds(List.of(100L))).thenReturn(List.of());
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(author));

        List<CommentDTO> result = svc.listForArtwork(1L, "joe", 10, 0);

        assertThat(result.get(0).isAuthor()).isTrue();
    }

    @Test
    void listForArtwork_viewerUserNotFoundActsAsAnonymous() {
        Artwork art = artwork(1L);
        User author = user(10L, "joe");
        Comment c1 = comment(100L, art, author, null, false);
        when(commentRepo.findTopLevelByArtwork(eq(1L), any(PageRequest.class))).thenReturn(List.of(c1));
        when(commentRepo.findRepliesByParentIds(List.of(100L))).thenReturn(List.of());
        when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());

        List<CommentDTO> result = svc.listForArtwork(1L, "ghost", 10, 0);

        assertThat(result.get(0).isAuthor()).isFalse();
    }

    @Test
    void listForArtwork_includesReplyPreviewAndCount() {
        Artwork art = artwork(1L);
        User author = user(10L, "joe");
        Comment top = comment(100L, art, author, null, false);
        Comment r1 = comment(200L, art, author, 100L, false);
        Comment r2 = comment(201L, art, author, 100L, false);
        Comment r3 = comment(202L, art, author, 100L, false);
        when(commentRepo.findTopLevelByArtwork(eq(1L), any(PageRequest.class))).thenReturn(List.of(top));
        when(commentRepo.findRepliesByParentIds(List.of(100L))).thenReturn(List.of(r1, r2, r3));

        List<CommentDTO> result = svc.listForArtwork(1L, null, 10, 0);

        assertThat(result.get(0).replyCount()).isEqualTo(3);
        assertThat(result.get(0).replies()).hasSize(2);
    }

    // ----- listReplies -----

    @Test
    void listReplies_returnsMappedDtos() {
        Artwork art = artwork(1L);
        User author = user(10L, "joe");
        Comment r1 = comment(200L, art, author, 100L, false);
        when(commentRepo.findRepliesByParentId(eq(100L), any(PageRequest.class))).thenReturn(List.of(r1));

        List<CommentDTO> result = svc.listReplies(100L, null, 10, 0);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(200L);
    }

    @Test
    void listReplies_clampsBounds() {
        when(commentRepo.findRepliesByParentId(eq(100L), any(PageRequest.class))).thenReturn(List.of());

        svc.listReplies(100L, "joe", 0, -1);

        verify(commentRepo).findRepliesByParentId(eq(100L), any(PageRequest.class));
    }

    // ----- create -----

    @Test
    void create_emptyWhenArtworkMissing() {
        when(artworkRepo.findById(1L)).thenReturn(Optional.empty());
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(user(10L, "joe")));

        Optional<CommentDTO> result = svc.create(1L, "joe", new CreateCommentCommand("hi", null));

        assertThat(result).isEmpty();
        verify(commentRepo, never()).save(any());
    }

    @Test
    void create_emptyWhenAuthorMissing() {
        when(artworkRepo.findById(1L)).thenReturn(Optional.of(artwork(1L)));
        when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());

        Optional<CommentDTO> result = svc.create(1L, "ghost", new CreateCommentCommand("hi", null));

        assertThat(result).isEmpty();
    }

    @Test
    void create_topLevelCommentSucceeds() {
        Artwork art = artwork(1L);
        User author = user(10L, "joe");
        when(artworkRepo.findById(1L)).thenReturn(Optional.of(art));
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(author));
        when(commentRepo.save(any())).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(500L);
            return c;
        });

        Optional<CommentDTO> result = svc.create(1L, "joe", new CreateCommentCommand("hi", null));

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(500L);
        assertThat(result.get().parentCommentId()).isNull();
    }

    @Test
    void create_replyEmptyWhenParentMissing() {
        when(artworkRepo.findById(1L)).thenReturn(Optional.of(artwork(1L)));
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(user(10L, "joe")));
        when(commentRepo.findById(999L)).thenReturn(Optional.empty());

        Optional<CommentDTO> result = svc.create(1L, "joe", new CreateCommentCommand("hi", 999L));

        assertThat(result).isEmpty();
    }

    @Test
    void create_replyEmptyWhenParentBelongsToDifferentArtwork() {
        Artwork art1 = artwork(1L);
        Artwork art2 = artwork(2L);
        User author = user(10L, "joe");
        Comment parent = comment(100L, art2, author, null, false);
        when(artworkRepo.findById(1L)).thenReturn(Optional.of(art1));
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(author));
        when(commentRepo.findById(100L)).thenReturn(Optional.of(parent));

        Optional<CommentDTO> result = svc.create(1L, "joe", new CreateCommentCommand("hi", 100L));

        assertThat(result).isEmpty();
    }

    @Test
    void create_replyEmptyWhenParentIsItselfAReply() {
        Artwork art = artwork(1L);
        User author = user(10L, "joe");
        Comment parent = comment(100L, art, author, 50L, false);
        when(artworkRepo.findById(1L)).thenReturn(Optional.of(art));
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(author));
        when(commentRepo.findById(100L)).thenReturn(Optional.of(parent));

        Optional<CommentDTO> result = svc.create(1L, "joe", new CreateCommentCommand("hi", 100L));

        assertThat(result).isEmpty();
    }

    @Test
    void create_replyEmptyWhenParentIsDeleted() {
        Artwork art = artwork(1L);
        User author = user(10L, "joe");
        Comment parent = comment(100L, art, author, null, true);
        when(artworkRepo.findById(1L)).thenReturn(Optional.of(art));
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(author));
        when(commentRepo.findById(100L)).thenReturn(Optional.of(parent));

        Optional<CommentDTO> result = svc.create(1L, "joe", new CreateCommentCommand("hi", 100L));

        assertThat(result).isEmpty();
    }

    @Test
    void create_replySucceedsForValidParent() {
        Artwork art = artwork(1L);
        User author = user(10L, "joe");
        Comment parent = comment(100L, art, author, null, false);
        when(artworkRepo.findById(1L)).thenReturn(Optional.of(art));
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(author));
        when(commentRepo.findById(100L)).thenReturn(Optional.of(parent));
        when(commentRepo.save(any())).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(600L);
            return c;
        });

        Optional<CommentDTO> result = svc.create(1L, "joe", new CreateCommentCommand("hi", 100L));

        assertThat(result).isPresent();
        assertThat(result.get().parentCommentId()).isEqualTo(100L);
    }

    @Test
    void create_replyEmptyWhenParentArtworkIsNull() {
        Artwork art = artwork(1L);
        User author = user(10L, "joe");
        Comment parent = comment(100L, null, author, null, false);
        when(artworkRepo.findById(1L)).thenReturn(Optional.of(art));
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(author));
        when(commentRepo.findById(100L)).thenReturn(Optional.of(parent));

        Optional<CommentDTO> result = svc.create(1L, "joe", new CreateCommentCommand("hi", 100L));

        assertThat(result).isEmpty();
    }

    // ----- delete -----

    @Test
    void delete_notFoundWhenCommentMissing() {
        when(commentRepo.findById(99L)).thenReturn(Optional.empty());

        assertThat(svc.delete(99L, "joe")).isEqualTo(CommentService.DeleteResult.NOT_FOUND);
    }

    @Test
    void delete_okWhenAlreadyDeleted() {
        Artwork art = artwork(1L);
        User author = user(10L, "joe");
        Comment c = comment(100L, art, author, null, true);
        when(commentRepo.findById(100L)).thenReturn(Optional.of(c));

        assertThat(svc.delete(100L, "joe")).isEqualTo(CommentService.DeleteResult.OK);
        verify(commentRepo, never()).save(any());
    }

    @Test
    void delete_forbiddenWhenRequesterUnknown() {
        Artwork art = artwork(1L);
        User author = user(10L, "joe");
        Comment c = comment(100L, art, author, null, false);
        when(commentRepo.findById(100L)).thenReturn(Optional.of(c));
        when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThat(svc.delete(100L, "ghost")).isEqualTo(CommentService.DeleteResult.FORBIDDEN);
    }

    @Test
    void delete_forbiddenWhenAuthorIsNull() {
        Artwork art = artwork(1L);
        Comment c = comment(100L, art, null, null, false);
        when(commentRepo.findById(100L)).thenReturn(Optional.of(c));
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(user(10L, "joe")));

        assertThat(svc.delete(100L, "joe")).isEqualTo(CommentService.DeleteResult.FORBIDDEN);
    }

    @Test
    void delete_forbiddenWhenRequesterIsNotAuthor() {
        Artwork art = artwork(1L);
        User author = user(10L, "joe");
        Comment c = comment(100L, art, author, null, false);
        when(commentRepo.findById(100L)).thenReturn(Optional.of(c));
        when(userRepo.findByUsername("bob")).thenReturn(Optional.of(user(20L, "bob")));

        assertThat(svc.delete(100L, "bob")).isEqualTo(CommentService.DeleteResult.FORBIDDEN);
    }

    @Test
    void delete_okMarksDeletedWhenRequesterIsAuthor() {
        Artwork art = artwork(1L);
        User author = user(10L, "joe");
        Comment c = comment(100L, art, author, null, false);
        when(commentRepo.findById(100L)).thenReturn(Optional.of(c));
        when(userRepo.findByUsername("joe")).thenReturn(Optional.of(author));
        when(commentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(svc.delete(100L, "joe")).isEqualTo(CommentService.DeleteResult.OK);
        assertThat(c.getIsDeleted()).isTrue();
        verify(commentRepo).save(c);
    }
}
