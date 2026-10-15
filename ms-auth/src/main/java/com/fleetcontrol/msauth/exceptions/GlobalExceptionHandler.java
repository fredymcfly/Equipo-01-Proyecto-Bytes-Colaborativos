package com.fleetcontrol.msauth.exceptions;

import com.fleetcontrol.msauth.constants.DatabaseConstants;
import com.fleetcontrol.msauth.constants.ErrorConstants;
import com.fleetcontrol.msauth.models.dto.response.ErrorResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Translates every exception into the common error contract of the platform.
 *
 * <p>It extends {@link ResponseEntityExceptionHandler} so that the standard Spring MVC errors (405,
 * 415, unreadable body...) keep their status and use the same JSON format, instead of being caught
 * by the generic handler and turned into a 500.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  /** Errors raised on purpose by the services (409, 401...). */
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
    return ResponseEntity.status(ex.getStatus())
        .body(ErrorResponse.of(ex.getCode(), ex.getMessage()));
  }

  /**
   * Unique-constraint violations that slipped past the existence checks (concurrent requests). The
   * violated constraint tells whether the email or the username is duplicated.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
    String cause = String.valueOf(NestedExceptionUtils.getMostSpecificCause(ex).getMessage());
    if (cause.contains(DatabaseConstants.Constraint.USERS_USERNAME)) {
      return conflict(ErrorConstants.Message.USERNAME_ALREADY_EXISTS);
    }
    if (cause.contains(DatabaseConstants.Constraint.USERS_EMAIL)) {
      return conflict(ErrorConstants.Message.EMAIL_ALREADY_EXISTS);
    }
    return handleUnexpected(ex);
  }

  /** Any other error: it is logged and the client only gets a generic message. */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    log.error("Unexpected error", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(
            ErrorResponse.of(
                ErrorConstants.Code.INTERNAL_ERROR, ErrorConstants.Message.INTERNAL_ERROR));
  }

  /** Bean Validation errors of the request body, with one detail per invalid field. */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<ErrorResponse.FieldDetail> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                error -> new ErrorResponse.FieldDetail(error.getField(), error.getDefaultMessage()))
            .toList();
    return ResponseEntity.status(status)
        .body(ErrorResponse.validation(ErrorConstants.Message.VALIDATION_ERROR, details));
  }

  /** Missing or malformed JSON body. */
  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return ResponseEntity.status(status)
        .body(
            ErrorResponse.of(
                ErrorConstants.Code.VALIDATION_ERROR, ErrorConstants.Message.MALFORMED_BODY));
  }

  /**
   * Any other standard Spring MVC error (404, 405, 415...). A 400 uses {@code VALIDATION_ERROR};
   * the rest use the name of the HTTP status as code, for example {@code METHOD_NOT_ALLOWED}.
   */
  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
      Exception ex,
      Object body,
      HttpHeaders headers,
      HttpStatusCode statusCode,
      WebRequest request) {
    HttpStatus status = HttpStatus.valueOf(statusCode.value());
    String code =
        status == HttpStatus.BAD_REQUEST ? ErrorConstants.Code.VALIDATION_ERROR : status.name();
    String message =
        body instanceof ProblemDetail problem && problem.getDetail() != null
            ? problem.getDetail()
            : status.getReasonPhrase();
    return ResponseEntity.status(statusCode).headers(headers).body(ErrorResponse.of(code, message));
  }

  private ResponseEntity<ErrorResponse> conflict(String message) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.of(ErrorConstants.Code.USER_ALREADY_EXISTS, message));
  }
}
