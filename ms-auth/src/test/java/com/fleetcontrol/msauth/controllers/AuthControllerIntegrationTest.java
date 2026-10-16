package com.fleetcontrol.msauth.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fleetcontrol.msauth.config.TestcontainersConfiguration;
import com.fleetcontrol.msauth.repositories.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuthControllerIntegrationTest {

  private static final String REGISTER = "/api/auth/register";
  private static final String LOGIN = "/api/auth/login";
  private static final String VALIDATE = "/api/auth/validate";

  @Autowired private MockMvc mockMvc;
  @Autowired private UserRepository userRepository;

  @BeforeEach
  void cleanDatabase() {
    userRepository.deleteAll();
  }

  private static String registerBody(String username, String email, String password) {
    return """
        {"username": "%s", "email": "%s", "password": "%s"}
        """
        .formatted(username, email, password);
  }

  private void register(String username, String email, String password) throws Exception {
    mockMvc
        .perform(
            post(REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody(username, email, password)))
        .andExpect(status().isCreated());
  }

  private String loginToken(String email, String password) throws Exception {
    String body =
        mockMvc
            .perform(
                post(LOGIN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"email": "%s", "password": "%s"}
                        """
                            .formatted(email, password)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(body, "$.token");
  }

  @Test
  void registerCreatesManagerAndNeverReturnsPassword() throws Exception {
    mockMvc
        .perform(
            post(REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("carlos_ruiz", "Carlos@FleetControl.com", "FleetPass123")))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", not(nullValue())))
        .andExpect(jsonPath("$.username").value("carlos_ruiz"))
        .andExpect(jsonPath("$.email").value("carlos@fleetcontrol.com"))
        .andExpect(jsonPath("$.role").value("MANAGER"))
        .andExpect(jsonPath("$.createdAt", not(nullValue())))
        .andExpect(jsonPath("$.password").doesNotExist());
  }

  @Test
  void registerWithDuplicatedEmailReturns409() throws Exception {
    register("carlos_ruiz", "carlos@fleetcontrol.com", "FleetPass123");

    mockMvc
        .perform(
            post(REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("other_user", "CARLOS@fleetcontrol.com", "FleetPass123")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("USER_ALREADY_EXISTS"))
        .andExpect(jsonPath("$.timestamp", not(nullValue())));
  }

  @Test
  void registerWithDuplicatedUsernameReturns409() throws Exception {
    register("carlos_ruiz", "carlos@fleetcontrol.com", "FleetPass123");

    mockMvc
        .perform(
            post(REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("carlos_ruiz", "other@fleetcontrol.com", "FleetPass123")))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.error").value("USER_ALREADY_EXISTS"))
        .andExpect(jsonPath("$.message").value("A user with this username already exists"));
  }

  @Test
  void registerWithWeakPasswordReturns400WithDetails() throws Exception {
    mockMvc
        .perform(
            post(REGISTER)
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerBody("carlos_ruiz", "carlos@fleetcontrol.com", "weak")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.details", hasSize(2)))
        .andExpect(jsonPath("$.details[0].field").value("password"))
        .andExpect(jsonPath("$.timestamp", not(nullValue())));
  }

  @Test
  void loginReturnsJwtWithOneHourLifetime() throws Exception {
    register("carlos_ruiz", "carlos@fleetcontrol.com", "FleetPass123");

    mockMvc
        .perform(
            post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email": "CARLOS@fleetcontrol.com", "password": "FleetPass123"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token", not(nullValue())))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(3600))
        .andExpect(jsonPath("$.userId", not(nullValue())))
        .andExpect(jsonPath("$.role").value("MANAGER"));
  }

  @Test
  void loginWithWrongPasswordReturns401() throws Exception {
    register("carlos_ruiz", "carlos@fleetcontrol.com", "FleetPass123");

    mockMvc
        .perform(
            post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email": "carlos@fleetcontrol.com", "password": "WrongPass123"}
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"))
        .andExpect(jsonPath("$.message").value("Invalid email or password"));
  }

  @Test
  void loginWithAnUnknownEmailReturnsTheSame401() throws Exception {
    mockMvc
        .perform(
            post(LOGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"email": "nobody@fleetcontrol.com", "password": "FleetPass123"}
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
  }

  @Test
  void validateAcceptsTokenIssuedByLogin() throws Exception {
    register("carlos_ruiz", "carlos@fleetcontrol.com", "FleetPass123");
    String token = loginToken("carlos@fleetcontrol.com", "FleetPass123");

    mockMvc
        .perform(post(VALIDATE).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.valid").value(true))
        .andExpect(jsonPath("$.userId", not(nullValue())))
        .andExpect(jsonPath("$.username").value("carlos_ruiz"))
        .andExpect(jsonPath("$.role").value("MANAGER"))
        .andExpect(jsonPath("$.error").doesNotExist());
  }

  @Test
  void validateWithoutTheHeaderReturns401() throws Exception {
    mockMvc
        .perform(post(VALIDATE))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.valid").value(false))
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.timestamp", not(nullValue())));
  }

  @Test
  void validateWithGarbageTokenReturns401() throws Exception {
    mockMvc
        .perform(post(VALIDATE).header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.valid").value(false))
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
  }

  @Test
  void anyOtherPathRequiresAuthenticationAndAnswersWithTheCommonContract() throws Exception {
    mockMvc
        .perform(get("/api/anything"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
        .andExpect(jsonPath("$.timestamp", not(nullValue())));
  }

  @Test
  void healthEndpointIsPublic() throws Exception {
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
  }
}
