package com.fleetcontrol.msvehicles.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Translates exceptions into the shared error contract. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** Maps any domain error to its HTTP status and contract body. */
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
    ErrorCode code = exception.code();
    return ResponseEntity.status(code.status()).body(errorBody(code, exception.getMessage()));
  }

  /** Maps request body validation failures to 400 with field-level details. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleBodyValidation(
      MethodArgumentNotValidException exception) {
    List<ErrorDetail> details =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> new ErrorDetail(error.getField(), error.getDefaultMessage()))
            .toList();
    return validationError(details);
  }

  /** Maps request parameter validation failures to 400 with field-level details. */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleParameterValidation(
      ConstraintViolationException exception) {
    List<ErrorDetail> details =
        exception.getConstraintViolations().stream()
            .map(violation -> new ErrorDetail(lastPathNode(violation), violation.getMessage()))
            .toList();
    return validationError(details);
  }

  /** Maps unreadable bodies, such as malformed JSON or unknown enum values, to 400. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableBody(
      HttpMessageNotReadableException exception) {
    return validationError(List.of());
  }

  /** Safety net for a duplicated plate under concurrent requests. */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
      DataIntegrityViolationException exception) {
    ErrorCode code = ErrorCode.VEHICLE_ALREADY_EXISTS;
    return ResponseEntity.status(code.status()).body(errorBody(code, code.defaultMessage()));
  }

  private ResponseEntity<ErrorResponse> validationError(List<ErrorDetail> details) {
    ErrorCode code = ErrorCode.VALIDATION_ERROR;
    ErrorResponse body =
        new ErrorResponse(code.name(), code.defaultMessage(), details, null, Instant.now());
    return ResponseEntity.status(code.status()).body(body);
  }

  private ErrorResponse errorBody(ErrorCode code, String message) {
    return new ErrorResponse(code.name(), message, null, null, Instant.now());
  }

  private String lastPathNode(ConstraintViolation<?> violation) {
    String path = violation.getPropertyPath().toString();
    int lastDot = path.lastIndexOf('.');
    return lastDot >= 0 ? path.substring(lastDot + 1) : path;
  }
}
