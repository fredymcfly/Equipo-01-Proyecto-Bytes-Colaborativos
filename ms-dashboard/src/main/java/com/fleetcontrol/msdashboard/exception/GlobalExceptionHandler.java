package com.fleetcontrol.msdashboard.exception;

import com.fleetcontrol.msdashboard.constants.ErrorConstants;
import com.fleetcontrol.msdashboard.models.dto.response.ErrorDetail;
import com.fleetcontrol.msdashboard.models.dto.response.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
        .body(errorBody(exception.code(), exception.getMessage(), null, null));
  }

  /** Maps a request that breaks a rule between its parameters to 400 with the field. */
  @ExceptionHandler(InvalidRequestException.class)
  public ResponseEntity<ErrorResponse> handleInvalidRequest(InvalidRequestException exception) {
    return validationError(exception.details());
  }

  /** Maps a source service that does not answer to 503, naming the service. */
  @ExceptionHandler(ServiceUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleServiceUnavailable(
      ServiceUnavailableException exception) {
    return ResponseEntity.status(exception.status())
        .body(errorBody(exception.code(), exception.getMessage(), null, exception.service()));
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

  /** Maps parameters that cannot be converted, such as an unknown metric or a bad date, to 400. */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(
      MethodArgumentTypeMismatchException exception) {
    return validationError(
        List.of(new ErrorDetail(exception.getName(), ErrorConstants.Reason.INVALID_VALUE)));
  }

  private ResponseEntity<ErrorResponse> validationError(List<ErrorDetail> details) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            errorBody(
                ErrorConstants.Code.VALIDATION_ERROR,
                ErrorConstants.Message.VALIDATION_ERROR,
                details,
                null));
  }

  private ErrorResponse errorBody(
      String code, String message, List<ErrorDetail> details, String service) {
    return new ErrorResponse(code, message, details, service, Instant.now());
  }

  private String lastPathNode(ConstraintViolation<?> violation) {
    String path = violation.getPropertyPath().toString();
    int lastDot = path.lastIndexOf('.');
    return lastDot >= 0 ? path.substring(lastDot + 1) : path;
  }
}
