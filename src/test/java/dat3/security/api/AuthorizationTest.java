package dat3.security.api;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import dat3.security.TestUtils;
import dat3.security.dto.LoginRequest;
import dat3.security.dto.LoginResponse;
import dat3.security.entity.Role;
import dat3.security.entity.UserWithRoles;
import dat3.security.repository.UserWithRolesRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

//@Disabled  //Comment out this line to run the tests if you are changing anything in the security features
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthorizationTest {

  @Autowired
  MockMvc mockMvc;
  @Autowired
  UserWithRolesRepository userWithRolesRepository;
  @Autowired
  PasswordEncoder passwordEncoder;


  private final ObjectMapper objectMapper = new ObjectMapper();

  public static String userJwtToken;
  public static String adminJwtToken;
  public static String user_adminJwtToken;
  public static String user_noRolesJwtToken;

  void LoginAndGetTokens() throws Exception {
    user_adminJwtToken = loginAndGetToken("u1","secret");
    userJwtToken = loginAndGetToken("u2","secret");
    adminJwtToken = loginAndGetToken("u3","secret");
    user_noRolesJwtToken = loginAndGetToken("u4","secret");
  }

  @BeforeEach
  void setUp() throws Exception {
    TestUtils.setupTestUsers(passwordEncoder,userWithRolesRepository);
    if(user_adminJwtToken==null) {
      LoginAndGetTokens();
    }
  }

  String loginAndGetToken(String user,String pw) throws Exception {
    LoginRequest loginRequest = new LoginRequest(user,pw);
    MvcResult response = mockMvc.perform(post("/api/auth/login")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(loginRequest)))
            .andExpect(status().isOk())
            .andReturn();
    LoginResponse loginResponse = objectMapper.readValue(response.getResponse().getContentAsString(), LoginResponse.class);
    return loginResponse.getToken();
  }


  @Test
  void testRolesAdmin() throws Exception {
    mockMvc.perform(get("/api/security-tests/admin")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminJwtToken)
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value("u3"))
            .andExpect(jsonPath("$.message").value("Admin"));
  }
  @Test
  void testEndpointAdminWrongRole() throws Exception {
    mockMvc.perform(get("/api/security-tests/admin")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + userJwtToken)
                    .contentType("application/json"))
            .andExpect(status().isForbidden());
  }
  @Test
  void testRolesAdminNotLoggedIn() throws Exception {
    mockMvc.perform(get("/api/security-tests/admin")
                    .contentType("application/json"))
            .andExpect(status().isUnauthorized());
  }
  @Test
  void testAuthenticatedNoRoles() throws Exception {
    mockMvc.perform(get("/api/security-tests/authenticated")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + user_noRolesJwtToken)
                    .contentType("application/json"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.userName").value("u4"))
            .andExpect(jsonPath("$.message").value("Authenticated user"));
  }
}