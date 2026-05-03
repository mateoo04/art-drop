package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.model.FeedSeen;
import hr.tvz.artdrop.artdropapp.repository.FeedSeenJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class FeedSeenService {

    private final FeedSeenJpaRepository feedSeenRepository;
    private final int dedupeWindowMinutes;
    private final int retentionDays;

    public FeedSeenService(
            FeedSeenJpaRepository feedSeenRepository,
            @Value("${feed.seen.dedupe-window-minutes:30}") int dedupeWindowMinutes,
            @Value("${feed.seen.retention-days:14}") int retentionDays
    ) {
        this.feedSeenRepository = feedSeenRepository;
        this.dedupeWindowMinutes = dedupeWindowMinutes;
        this.retentionDays = retentionDays;
    }

    @Transactional
    public void recordSeen(Long viewerId, Collection<Long> artworkIds) {
        if (viewerId == null || artworkIds == null || artworkIds.isEmpty()) return;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dedupeAfter = now.minusMinutes(dedupeWindowMinutes);
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(artworkIds);
        List<FeedSeen> toInsert = new ArrayList<>();
        for (Long artworkId : uniqueIds) {
            if (artworkId == null) continue;
            if (feedSeenRepository.existsByViewerIdAndArtworkIdAndSeenAtAfter(viewerId, artworkId, dedupeAfter)) {
                continue;
            }
            toInsert.add(new FeedSeen(null, viewerId, artworkId, now));
        }
        if (!toInsert.isEmpty()) {
            feedSeenRepository.saveAll(toInsert);
        }
    }

    @Scheduled(cron = "${feed.seen.cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void cleanupOldSeenRows() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        feedSeenRepository.deleteOlderThan(cutoff);
    }
}
