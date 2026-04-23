package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.UpdateProfileCommand;
import hr.tvz.artdrop.artdropapp.dto.UserProfileDTO;

import java.util.Optional;

public interface UserService {

    Optional<UserProfileDTO> findMe(String username);

    Optional<UserProfileDTO> updateMe(String username, UpdateProfileCommand command);
}
