package hr.tvz.artdrop.artdropapp.service;

import hr.tvz.artdrop.artdropapp.dto.JwtResponse;
import hr.tvz.artdrop.artdropapp.dto.LoginRequest;
import hr.tvz.artdrop.artdropapp.dto.RegisterRequest;

public interface AuthService {
    JwtResponse login(LoginRequest loginRequest);

    JwtResponse signup(RegisterRequest registerRequest);
}
