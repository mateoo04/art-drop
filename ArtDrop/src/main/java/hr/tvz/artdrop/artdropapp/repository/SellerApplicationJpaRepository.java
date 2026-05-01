package hr.tvz.artdrop.artdropapp.repository;

import hr.tvz.artdrop.artdropapp.model.SellerApplication;
import hr.tvz.artdrop.artdropapp.model.SellerApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SellerApplicationJpaRepository extends JpaRepository<SellerApplication, Long> {

    Optional<SellerApplication> findTopByUserIdOrderBySubmittedAtDesc(Long userId);

    List<SellerApplication> findByUserIdOrderBySubmittedAtDesc(Long userId);

    boolean existsByUserIdAndStatus(Long userId, SellerApplicationStatus status);

    Page<SellerApplication> findByStatusOrderBySubmittedAtAsc(SellerApplicationStatus status, Pageable pageable);

    Page<SellerApplication> findAllByOrderBySubmittedAtDesc(Pageable pageable);

    @Query("SELECT s FROM SellerApplication s WHERE s.userId = :userId AND s.status = 'APPROVED' AND s.revokedAt IS NULL ORDER BY s.decidedAt DESC")
    Optional<SellerApplication> findCurrentApprovalForUser(Long userId);
}
