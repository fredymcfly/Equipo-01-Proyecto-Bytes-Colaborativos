package com.fleetcontrol.msalerts.exception;

import com.fleetcontrol.msalerts.constants.ErrorConstants;
import com.fleetcontrol.msalerts.models.dto.response.ErrorDetail;
import com.fleetcontrol.msalerts.models.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Translates exceptions into the shared error contract. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** Maps any domain error to its HTTP status and contract body. */
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ErrorResponse> handleApiException(ApiException exception) {
    return ResponseEntity.status(exception.status())
        .body(errorBody(exception.code(), exception.getMessage()));
  }

  /** Maps a request that breaks a rule between its fields to 400 with the field. */
  @ExceptionHandler(InvalidRequestException.class)
  public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException exception) {
    return validationError(exception.details());
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

  /** Maps a required parameter that is missing to 400. */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParameter(
      MissingServletRequestParameterException exception) {
    return validationError(
        List.of(new ErrorDetail(exception.getParameterName(), ErrorConstants.Reason.REQUIRED)));
  }

  /** Maps unreadable bodies, such as malformed JSON or unknown enum values, to 400. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableBody(
      HttpMessageNotReadableException exception) {
    return validationError(List.of());
  }

  /** Maps parameters that cannot be converted, such as an unknown enum value or a bad date. */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception) {
    return validationError(
        List.of(new ErrorDetail(exception.getName(), ErrorConstants.Reason.INVALID_VALUE)));
  }

  private ResponseEntity<ErrorResponse> validationError(List<ErrorDetail> details) {
    ErrorResponse body =
        new ErrorResponse(
            ErrorConstants.Code.VALIDATION_ERROR,
            ErrorConstants.Message.VALIDATION_ERROR,
            details,
            null,
            Instant.now());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  private ErrorResponse errorBody(String code, String message) {
    return new ErrorResponse(code, message, null, null, Instant.now());
  }

  private String lastPathNode(ConstraintViolation<?> violation) {
    String path = violation.getPropertyPath().toString();
    int lastDot = path.lastIndexOf('.');
    return lastDot >= 0 ? path.substring(lastDot + 1) : path;
  }
}
