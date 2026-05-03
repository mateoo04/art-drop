package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.AdminChallengeRowDTO;
import hr.tvz.artdrop.artdropapp.dto.AdminChallengeUpsertDTO;
import hr.tvz.artdrop.artdropapp.model.ChallengeStatus;
import org.springframework.data.domain.Page;

import java.util.Optional;

public interface AdminChallengeService {
    Page<AdminChallengeRowDTO> list(String query, ChallengeStatus status, int page, int size, String sort);

    Optional<AdminChallengeRowDTO> get(Long id);

    AdminChallengeRowDTO create(AdminChallengeUpsertDTO dto, String adminUsername);

    Optional<AdminChallengeRowDTO> update(Long id, AdminChallengeUpsertDTO dto);

    boolean delete(Long id);

    Optional<AdminChallengeRowDTO> setStatus(Long id, ChallengeStatus status);
}
