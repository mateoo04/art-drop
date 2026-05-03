package hr.tvz.artdrop.artdropapp.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
public class FeedCacheConfig {

    @Bean
    public Cache<String, List<Long>> feedSnapshotCache(
            @Value("${feed.ranking.snapshot-ttl-minutes:5}") int ttlMinutes,
            @Value("${feed.ranking.snapshot-max-entries:10000}") int maxEntries
    ) {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(ttlMinutes))
                .maximumSize(maxEntries)
                .build();
    }
}
