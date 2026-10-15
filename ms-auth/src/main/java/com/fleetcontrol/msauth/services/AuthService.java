package com.fleetcontrol.msauth.services;

import com.fleetcontrol.msauth.config.properties.JwtProperties;
import com.fleetcontrol.msauth.constants.ErrorConstants;
import com.fleetcontrol.msauth.exceptions.ApiException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/** Business logic of registration, login and token validation. */
@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final JwtProperties jwt;

  /**
   * Registers a new user with the {@code MANAGER} role.
   *
   * <p>The request already carries the email in lowercase. Concurrent duplicates that slip past the
   * checks are stopped by the {@code UNIQUE} constraints of the database.
   *
   * @param request the registration data
   * @return the created user, without the password
   * @throws UserAlreadyExistsException if the email or the username is already registered
   */
  public RegisterResponse register(RegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new UserAlreadyExistsException(ErrorConstants.Message.EMAIL_ALREADY_EXISTS);
    }
    if (userRepository.existsByUsername(request.username())) {
      throw new UserAlreadyExistsException(ErrorConstants.Message.USERNAME_ALREADY_EXISTS);
    }

    User user =
        userRepository.save(
            User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(UserRole.MANAGER)
                .build());

    return new RegisterResponse(
        user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.getCreatedAt());
  }

  /**
   * Authenticates a user and issues a JWT.
   *
   * @param request the email and password
   * @return the token and its metadata
   * @throws InvalidCredentialsException if the email or the password is wrong
   */
  public LoginResponse login(LoginRequest request) {
    try {
      authenticationManager.authenticate(
          new UsernamePasswordAuthenticationToken(request.email(), request.password()));
    } catch (AuthenticationException e) {
      throw new InvalidCredentialsException();
    }

    User user =
        userRepository.findByEmail(request.email()).orElseThrow(InvalidCredentialsException::new);

    return new LoginResponse(
        jwtService.generateToken(user),
        jwt.tokenPrefix(),
        jwtService.getExpiresInSeconds(),
        user.getId(),
        user.getRole());
  }

  /**
   * Validates the token of an {@code Authorization} header. It never throws: a failure is returned
   * as a response with {@code valid} set to false, and the controller answers with 401.
   *
   * @param authorization the raw {@code Authorization} header, or null if it is missing
   * @return the user data when the token is valid, or the error otherwise
   */
  public ValidateResponse validate(String authorization) {
    try {
      JwtService.TokenData data = jwtService.parseToken(extractToken(authorization));
      return ValidateResponse.success(data.userId(), data.username(), data.role());
    } catch (ApiException e) {
      return ValidateResponse.failure(e.getCode(), e.getMessage());
    }
  }

  private String extractToken(String authorization) {
    String prefix = jwt.tokenPrefix() + " ";
    if (authorization == null || !authorization.startsWith(prefix)) {
      throw new InvalidTokenException();
    }
    return authorization.substring(prefix.length()).trim();
  }
}
