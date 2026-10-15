package com.fleetcontrol.msauth.models;

import static org.assertj.core.api.Assertions.assertThat;

import com.fleetcontrol.msauth.models.dto.request.RegisterRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RegisterRequestValidationTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  private static Set<String> invalidFields(RegisterRequest request) {
    return validator.validate(request).stream()
        .map(ConstraintViolation::getPropertyPath)
        .map(Object::toString)
        .collect(Collectors.toSet());
  }

  @Test
  void validRequestHasNoViolations() {
    assertThat(invalidFields(new RegisterRequest("carlos_ruiz", "c@x.com", "FleetPass123")))
        .isEmpty();
  }

  @Test
  void passwordShorterThanEightCharactersIsRejected() {
    assertThat(invalidFields(new RegisterRequest("carlos_ruiz", "c@x.com", "Fleet12")))
        .containsExactly("password");
  }

  @Test
  void passwordWithoutUppercaseIsRejected() {
    assertThat(invalidFields(new RegisterRequest("carlos_ruiz", "c@x.com", "fleetpass123")))
        .containsExactly("password");
  }

  @Test
  void passwordWithoutNumberIsRejected() {
    assertThat(invalidFields(new RegisterRequest("carlos_ruiz", "c@x.com", "FleetPassword")))
        .containsExactly("password");
  }

  @Test
  void usernameWithSpacesIsRejected() {
    assertThat(invalidFields(new RegisterRequest("carlos ruiz", "c@x.com", "FleetPass123")))
        .containsExactly("username");
  }

  @Test
  void invalidEmailIsRejected() {
    assertThat(invalidFields(new RegisterRequest("carlos_ruiz", "not-an-email", "FleetPass123")))
        .containsExactly("email");
  }

  @Test
  void blankFieldsAreRejected() {
    assertThat(invalidFields(new RegisterRequest("", "", "")))
        .containsExactlyInAnyOrder("username", "email", "password");
  }

  @Test
  void theEmailIsTrimmedAndLowercased() {
    RegisterRequest request = new RegisterRequest("carlos_ruiz", "  Carlos@X.COM ", "FleetPass123");

    assertThat(request.email()).isEqualTo("carlos@x.com");
  }
}
