package com.fleetcontrol.msauth.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fleetcontrol.msauth.config.properties.JwtProperties;
import com.fleetcontrol.msauth.constants.ErrorConstants;
import com.fleetcontrol.msauth.exceptions.InvalidCredentialsException;
import com.fleetcontrol.msauth.exceptions.InvalidTokenException;
import com.fleetcontrol.msauth.exceptions.UserAlreadyExistsException;
import com.fleetcontrol.msauth.models.dto.request.LoginRequest;
import com.fleetcontrol.msauth.models.dto.request.RegisterRequest;
import com.fleetcontrol.msauth.models.dto.response.LoginResponse;
import com.fleetcontrol.msauth.models.dto.response.RegisterResponse;
import com.fleetcontrol.msauth.models.dto.response.ValidateResponse;
import com.fleetcontrol.msauth.models.entities.User;
import com.fleetcontrol.msauth.models.enums.UserRole;
import com.fleetcontrol.msauth.repositories.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private AuthenticationManager authenticationManager;
  @Mock private JwtService jwtService;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    JwtProperties jwt = new JwtProperties("x".repeat(32), "Bearer", 3_600_000);
    authService =
        new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService, jwt);
  }

  private static RegisterRequest registerRequest() {
    return new RegisterRequest("carlos_ruiz", "Carlos@FleetControl.com", "FleetPass123");
  }

  private static User savedUser() {
    return User.builder()
        .id(UUID.randomUUID())
        .username("carlos_ruiz")
        .email("carlos@fleetcontrol.com")
        .password("hash")
        .role(UserRole.MANAGER)
        .createdAt(Instant.now())
        .build();
  }

  @Test
  void registerCreatesManagerWithHashedPassword() {
    when(userRepository.existsByEmail("carlos@fleetcontrol.com")).thenReturn(false);
    when(userRepository.existsByUsername("carlos_ruiz")).thenReturn(false);
    when(passwordEncoder.encode("FleetPass123")).thenReturn("hash");
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            invocation -> {
              User user = invocation.getArgument(0);
              user.setId(UUID.randomUUID());
              user.setCreatedAt(Instant.now());
              return user;
            });

    RegisterResponse response = authService.register(registerRequest());

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertThat(captor.getValue().getPassword()).isEqualTo("hash");
    assertThat(captor.getValue().getRole()).isEqualTo(UserRole.MANAGER);
    assertThat(captor.getValue().getEmail()).isEqualTo("carlos@fleetcontrol.com");
    assertThat(response.role()).isEqualTo(UserRole.MANAGER);
    assertThat(response.username()).isEqualTo("carlos_ruiz");
  }

  @Test
  void registerFailsWhenTheEmailAlreadyExists() {
    when(userRepository.existsByEmail("carlos@fleetcontrol.com")).thenReturn(true);

    assertThatThrownBy(() -> authService.register(registerRequest()))
        .isInstanceOf(UserAlreadyExistsException.class)
        .hasMessage(ErrorConstants.Message.EMAIL_ALREADY_EXISTS);
    verify(userRepository, never()).save(any());
  }

  @Test
  void registerFailsWhenTheUsernameAlreadyExists() {
    when(userRepository.existsByEmail("carlos@fleetcontrol.com")).thenReturn(false);
    when(userRepository.existsByUsername("carlos_ruiz")).thenReturn(true);

    assertThatThrownBy(() -> authService.register(registerRequest()))
        .isInstanceOf(UserAlreadyExistsException.class)
        .hasMessage(ErrorConstants.Message.USERNAME_ALREADY_EXISTS);
    verify(userRepository, never()).save(any());
  }

  @Test
  void loginReturnsTokenForValidCredentials() {
    User user = savedUser();
    when(userRepository.findByEmail("carlos@fleetcontrol.com")).thenReturn(Optional.of(user));
    when(jwtService.generateToken(user)).thenReturn("the-token");
    when(jwtService.getExpiresInSeconds()).thenReturn(3600L);

    LoginResponse response =
        authService.login(new LoginRequest("Carlos@FleetControl.com", "FleetPass123"));

    assertThat(response.token()).isEqualTo("the-token");
    assertThat(response.tokenType()).isEqualTo("Bearer");
    assertThat(response.expiresIn()).isEqualTo(3600);
    assertThat(response.userId()).isEqualTo(user.getId());
    assertThat(response.role()).isEqualTo(UserRole.MANAGER);
  }

  @Test
  void loginFailsWithInvalidCredentials() {
    when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

    assertThatThrownBy(
            () -> authService.login(new LoginRequest("carlos@fleetcontrol.com", "wrong")))
        .isInstanceOf(InvalidCredentialsException.class)
        .hasMessage(ErrorConstants.Message.INVALID_CREDENTIALS);
    verify(jwtService, never()).generateToken(any());
  }

  @Test
  void validateReturnsUserDataForValidToken() {
    UUID id = UUID.randomUUID();
    when(jwtService.parseToken("the-token"))
        .thenReturn(new JwtService.TokenData(id, "carlos_ruiz", UserRole.MANAGER));

    ValidateResponse response = authService.validate("Bearer the-token");

    assertThat(response.valid()).isTrue();
    assertThat(response.userId()).isEqualTo(id);
    assertThat(response.username()).isEqualTo("carlos_ruiz");
    assertThat(response.role()).isEqualTo(UserRole.MANAGER);
    assertThat(response.error()).isNull();
  }

  @Test
  void validateFailsWithoutTheHeader() {
    ValidateResponse response = authService.validate(null);

    assertThat(response.valid()).isFalse();
    assertThat(response.error()).isEqualTo(ErrorConstants.Code.UNAUTHORIZED);
    assertThat(response.timestamp()).isNotNull();
  }

  @Test
  void validateFailsWithoutTheBearerPrefix() {
    ValidateResponse response = authService.validate("the-token");

    assertThat(response.valid()).isFalse();
    assertThat(response.error()).isEqualTo(ErrorConstants.Code.UNAUTHORIZED);
  }

  @Test
  void validateReportsAnExpiredToken() {
    when(jwtService.parseToken("old-token"))
        .thenThrow(new com.fleetcontrol.msauth.exceptions.TokenExpiredException());

    ValidateResponse response = authService.validate("Bearer old-token");

    assertThat(response.valid()).isFalse();
    assertThat(response.error()).isEqualTo(ErrorConstants.Code.TOKEN_EXPIRED);
    assertThat(response.message()).isEqualTo(ErrorConstants.Message.TOKEN_EXPIRED);
  }

  @Test
  void validateReportsAnInvalidToken() {
    when(jwtService.parseToken("bad-token")).thenThrow(new InvalidTokenException());

    ValidateResponse response = authService.validate("Bearer bad-token");

    assertThat(response.valid()).isFalse();
    assertThat(response.error()).isEqualTo(ErrorConstants.Code.UNAUTHORIZED);
  }
}
