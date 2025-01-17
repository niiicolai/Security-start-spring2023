package dat3.security.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import dat3.security.TestUtils;
import dat3.security.dto.LoginRequest;
import dat3.security.repository.UserWithRolesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//@Disabled  //Comment out this line to run the tests if you are changing anything in the security features
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")  //Ensures that we use the in-memory database
@Transactional
public class AuthenticationTest {

  @Autowired
  MockMvc mockMvc;
  @Autowired
  UserWithRolesRepository userWithRolesRepository;
  @Autowired
  PasswordEncoder passwordEncoder;

  private final ObjectMapper objectMapper = new ObjectMapper();
  public boolean isDataInitialized = false;

  @BeforeEach
  void setUp() throws Exception {
    if(!isDataInitialized) {
      isDataInitialized = true;
      TestUtils.setupTestUsers(passwordEncoder,userWithRolesRepository);
    }
  }

  @Test
  void login() throws Exception {
    LoginRequest loginRequest = new LoginRequest("u1", "secret");
    mockMvc.perform(post("/api/auth/login")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("u1"))
            .andExpect(jsonPath("$.roles", hasSize(2)))
            .andExpect(jsonPath("$.roles", containsInAnyOrder("USER","ADMIN")))
            .andExpect(result -> {
              //Not a bulletproof test, but acceptable. First part should always be the same. A token must always contain two dots.
              String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
              assertTrue(token.startsWith("eyJhbGciOiJIUzI1NiJ9"));
              assertTrue(token.chars().filter(ch -> ch == '.').count() == 2);
            });
  }

  @Test
  void loginWithWrongPassword() throws Exception {
    LoginRequest loginRequest = new LoginRequest("u1", "wrong");
    mockMvc.perform(post("/api/auth/login")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized());

  }
  @Test
  void loginWithWrongUsername() throws Exception {
    LoginRequest loginRequest = new LoginRequest("u111111", "wrong");
    mockMvc.perform(post("/api/auth/login")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isUnauthorized());
  }
}