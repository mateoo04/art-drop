package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.Challenge;
import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChallengeJpaRepository extends JpaRepository<Challenge, Long> {
    List<Challenge> findByStatus(ChallengeStatus status);

    @Query(value = """
            SELECT * FROM challenge c
            WHERE c.search_tsv @@ websearch_to_tsquery('english', :q)
            ORDER BY CASE c.status
                       WHEN 'ACTIVE'   THEN 0
                       WHEN 'UPCOMING' THEN 1
                       ELSE 2
                     END,
                     ts_rank_cd(c.search_tsv, websearch_to_tsquery('english', :q)) DESC,
                     c.ends_at ASC
            """, nativeQuery = true)
    List<Challenge> searchChallenges(@Param("q") String q, Pageable pageable);
}
