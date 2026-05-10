package hr.tvz.artdrop.artdropapp;

import tools.jackson.databind.ObjectMapper;
import hr.tvz.artdrop.artdropapp.dto.LoginRequest;
import hr.tvz.artdrop.artdropapp.dto.RegisterRequest;
import hr.tvz.artdrop.artdropapp.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class AuthControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void loginValidCredentialsReturnsToken() throws Exception {
        LoginRequest req = new LoginRequest("user", "admin6060");
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void loginWrongPasswordReturnsUnauthorized() throws Exception {
        LoginRequest req = new LoginRequest("user", "wrong-password");
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginBlankUsernameReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signupNewUserReturnsCreated() throws Exception {
        String unique = "lab9_" + System.nanoTime();
        RegisterRequest req = new RegisterRequest(unique, unique + "@artdrop.local", "password123", "Lab9 User");
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void signupExistingEmailReturnsConflict() throws Exception {
        // 'user@artdrop.local' is in the dev seed (db/dev/R__seed.sql)
        RegisterRequest req = new RegisterRequest("dup_" + System.nanoTime(), "user@artdrop.local", "password123", "Dup User");
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void signupInvalidEmailReturnsBadRequest() throws Exception {
        RegisterRequest req = new RegisterRequest("nu_" + System.nanoTime(), "not-an-email", "password123", "Lab9 User");
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signupShortPasswordReturnsBadRequest() throws Exception {
        RegisterRequest req = new RegisterRequest("nu_" + System.nanoTime(), "x@y.io", "short", "Lab9 User");
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
