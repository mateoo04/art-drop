package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.model.FeedSeen;
import hr.tvz.artdrop.artdropapp.repository.FeedSeenJpaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedSeenServiceTest {

    private final FeedSeenJpaRepository repo = mock(FeedSeenJpaRepository.class);
    private final FeedSeenService svc = new FeedSeenService(repo, 30, 14);

    @Test
    void recordSeen_doesNothingWhenViewerNull() {
        svc.recordSeen(null, List.of(1L, 2L));
        verify(repo, never()).saveAll(any());
    }

    @Test
    void recordSeen_doesNothingWhenIdsNull() {
        svc.recordSeen(1L, null);
        verify(repo, never()).saveAll(any());
    }

    @Test
    void recordSeen_doesNothingWhenIdsEmpty() {
        svc.recordSeen(1L, List.of());
        verify(repo, never()).saveAll(any());
    }

    @Test
    void recordSeen_skipsNullArtworkIds() {
        svc.recordSeen(1L, Arrays.asList((Long) null));
        verify(repo, never()).saveAll(any());
    }

    @Test
    void recordSeen_dedupesAlreadySeenWithinWindow() {
        when(repo.existsByViewerIdAndArtworkIdAndSeenAtAfter(anyLong(), anyLong(), any()))
                .thenReturn(true);

        svc.recordSeen(1L, List.of(10L, 11L));

        verify(repo, never()).saveAll(any());
    }

    @Test
    void recordSeen_insertsNewSeenRows() {
        when(repo.existsByViewerIdAndArtworkIdAndSeenAtAfter(anyLong(), anyLong(), any()))
                .thenReturn(false);
        ArgumentCaptor<Iterable<FeedSeen>> captor = ArgumentCaptor.forClass(Iterable.class);

        svc.recordSeen(1L, List.of(10L, 11L, 10L));

        verify(repo).saveAll(captor.capture());
        List<FeedSeen> saved = (List<FeedSeen>) captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(FeedSeen::getArtworkId).containsExactly(10L, 11L);
        assertThat(saved).allMatch(s -> s.getViewerId().equals(1L));
    }

    @Test
    void recordSeen_mixedDedupeAndInsert() {
        when(repo.existsByViewerIdAndArtworkIdAndSeenAtAfter(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(10L), any())).thenReturn(true);
        when(repo.existsByViewerIdAndArtworkIdAndSeenAtAfter(org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(11L), any())).thenReturn(false);
        ArgumentCaptor<Iterable<FeedSeen>> captor = ArgumentCaptor.forClass(Iterable.class);

        svc.recordSeen(1L, List.of(10L, 11L));

        verify(repo).saveAll(captor.capture());
        List<FeedSeen> saved = (List<FeedSeen>) captor.getValue();
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getArtworkId()).isEqualTo(11L);
    }

    @Test
    void cleanupOldSeenRows_deletesOlderThanRetention() {
        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);

        svc.cleanupOldSeenRows();

        verify(repo).deleteOlderThan(captor.capture());
        assertThat(captor.getValue()).isBefore(LocalDateTime.now());
        assertThat(captor.getValue()).isAfter(LocalDateTime.now().minusDays(15));
    }
}
