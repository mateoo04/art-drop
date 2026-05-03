package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.AdminUserDetailDTO;
import hr.tvz.artdrop.artdropapp.dto.AdminUserSummaryDTO;
import hr.tvz.artdrop.artdropapp.dto.ListedArtworkCountDTO;
import hr.tvz.artdrop.artdropapp.dto.RevokeSellerCommand;
import hr.tvz.artdrop.artdropapp.dto.SellerApplicationDTO;
import hr.tvz.artdrop.artdropapp.dto.SellerApplicationDecisionCommand;
import hr.tvz.artdrop.artdropapp.dto.SubmitSellerApplicationCommand;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SellerApplicationService {

    enum SubmitError { ALREADY_SELLER, ALREADY_PENDING, COOLDOWN_ACTIVE, USER_NOT_FOUND }

    sealed interface SubmitResult permits SubmitOk, SubmitFailure {}
    record SubmitOk(SellerApplicationDTO application) implements SubmitResult {}
    record SubmitFailure(SubmitError error, LocalDateTime canReapplyAt) implements SubmitResult {}

    enum DecideError { NOT_FOUND, NOT_PENDING }
    sealed interface DecideResult permits DecideOk, DecideFailure {}
    record DecideOk(SellerApplicationDTO application) implements DecideResult {}
    record DecideFailure(DecideError error) implements DecideResult {}

    enum RevokeError { USER_NOT_FOUND, NOT_A_SELLER }
    sealed interface RevokeResult permits RevokeOk, RevokeFailure {}
    record RevokeOk(int unlistedCount) implements RevokeResult {}
    record RevokeFailure(RevokeError error) implements RevokeResult {}

    Optional<SellerApplicationDTO> findMyLatest(String username);

    SubmitResult submit(String username, SubmitSellerApplicationCommand command);

    Page<SellerApplicationDTO> listApplications(String statusFilter, int page, int size);

    Page<AdminUserSummaryDTO> searchUsers(
            String query,
            List<String> sellerStatuses,
            String role,
            String sort,
            int page,
            int size
    );

    Optional<AdminUserDetailDTO> getUserDetail(Long userId);

    DecideResult approve(Long applicationId, String adminUsername, SellerApplicationDecisionCommand command);

    DecideResult reject(Long applicationId, String adminUsername, SellerApplicationDecisionCommand command);

    RevokeResult revoke(Long userId, String adminUsername, RevokeSellerCommand command);

    ListedArtworkCountDTO countListedArtworks(Long userId);

    /** @return "OK", "NOT_FOUND", or "SELF_DEACTIVATE" */
    String promoteToAdmin(Long userId, String actingUsername);

    /** @return "OK" or "NOT_FOUND" */
    String grantSellerRole(Long userId, String actingUsername);

    /** @return "OK", "NOT_FOUND", or "SELF_DEACTIVATE" */
    String deactivateUser(Long userId, String actingUsername);

    /** @return "OK" or "NOT_FOUND" */
    String reactivateUser(Long userId, String actingUsername);
}
