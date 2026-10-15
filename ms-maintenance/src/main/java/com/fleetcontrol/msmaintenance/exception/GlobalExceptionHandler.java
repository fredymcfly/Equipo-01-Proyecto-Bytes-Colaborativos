package com.fleetcontrol.msmaintenance.exception;

import com.fleetcontrol.msmaintenance.constants.ErrorConstants;
import com.fleetcontrol.msmaintenance.models.dto.response.ErrorDetail;
import com.fleetcontrol.msmaintenance.models.dto.response.ErrorResponse;
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

  /**
   * Handles {@link VehicleNotFoundException} and transforms it into a 404 Not Found response.
   *
   * @param ex caught exception
   * @return response entity containing standard error details
   */
  @ExceptionHandler(VehicleNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleVehicleNotFound(VehicleNotFoundException ex) {
    return buildError(HttpStatus.NOT_FOUND, ex);
  }

  /**
   * Handles {@link OrderNotFoundException} and transforms it into a 404 Not Found response.
   *
   * @param ex caught exception
   * @return response entity containing standard error details
   */
  @ExceptionHandler(OrderNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex) {
    return buildError(HttpStatus.NOT_FOUND, ex);
  }

  /**
   * Handles {@link PlanAlreadyExistsException} and transforms it into a 409 Conflict response.
   *
   * @param ex caught exception
   * @return response entity containing standard error details
   */
  @ExceptionHandler(PlanAlreadyExistsException.class)
  public ResponseEntity<ErrorResponse> handlePlanAlreadyExists(PlanAlreadyExistsException ex) {
    return buildError(HttpStatus.CONFLICT, ex);
  }

  /**
   * Handles {@link InvalidOrderStateException} and transforms it into a 409 Conflict response.
   *
   * @param ex caught exception
   * @return response entity containing standard error details
   */
  @ExceptionHandler(InvalidOrderStateException.class)
  public ResponseEntity<ErrorResponse> handleInvalidOrderState(InvalidOrderStateException ex) {
    return buildError(HttpStatus.CONFLICT, ex);
  }

  /**
   * Handles {@link VehicleNotAvailableException} and transforms it into a 409 Conflict response.
   *
   * @param ex caught exception
   * @return response entity containing standard error details
   */
  @ExceptionHandler(VehicleNotAvailableException.class)
  public ResponseEntity<ErrorResponse> handleVehicleNotAvailable(VehicleNotAvailableException ex) {
    return buildError(HttpStatus.CONFLICT, ex);
  }

  /**
   * Handles {@link InvalidOdometerException} and transforms it into a 409 Conflict response.
   *
   * @param ex caught exception
   * @return response entity containing standard error details
   */
  @ExceptionHandler(InvalidOdometerException.class)
  public ResponseEntity<ErrorResponse> handleInvalidOdometer(InvalidOdometerException ex) {
    return buildError(HttpStatus.CONFLICT, ex);
  }

  /**
   * Handles {@link ServiceUnavailableException} and transforms it into a 503 response that names
   * the service that failed.
   *
   * @param ex caught exception
   * @return response entity containing standard error details
   */
  @ExceptionHandler(ServiceUnavailableException.class)
  public ResponseEntity<ErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex) {
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .body(body(ex.code(), ex.getMessage(), null, ex.service()));
  }

  /**
   * Handles {@link ValidationFailedException} and transforms it into a 400 Bad Request response
   * with field-level details.
   *
   * @param ex caught exception
   * @return response entity containing standard error details
   */
  @ExceptionHandler(ValidationFailedException.class)
  public ResponseEntity<ErrorResponse> handleValidationFailed(ValidationFailedException ex) {
    return validationError(ex.details());
  }

  /**
   * Handles request body validation failures and transforms them into a 400 Bad Request response.
   *
   * @param ex caught exception
   * @return response entity containing standard error details
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex) {
    List<ErrorDetail> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new ErrorDetail(error.getField(), error.getDefaultMessage()))
            .toList();
    return validationError(details);
  }

  /**
   * Handles request parameter validation failures and transforms them into a 400 Bad Request
   * response.
   *
   * @param ex caught exception
   * @return response entity containing standard error details
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleParameterValidation(ConstraintViolationException ex) {
    List<ErrorDetail> details =
        ex.getConstraintViolations().stream()
            .map(violation -> new ErrorDetail(lastPathNode(violation), violation.getMessage()))
            .toList();
    return validationError(details);
  }

  /**
   * Handles a missing required query parameter and transforms it into a 400 Bad Request response.
   *
   * @param ex caught exception
   * @return response entity containing standard error details
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingParameter(
      MissingServletRequestParameterException ex) {
    return validationError(
        List.of(new ErrorDetail(ex.getParameterName(), ErrorConstants.Reason.REQUIRED)));
  }

  /**
   * Handles a query parameter with an invalid value, such as a bad UUID or enum, and transforms it
   * into a 400 Bad Request response.
   *
   * @param ex caught exception
   * @return response entity containing standard error details
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return validationError(
        List.of(new ErrorDetail(ex.getName(), ErrorConstants.Reason.INVALID_VALUE)));
  }

  /**
   * Handles unreadable bodies, such as malformed JSON or unknown enum values, and transforms them
   * into a 400 Bad Request response.
   *
   * @param ex caught exception
   * @return response entity containing standard error details
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
    return validationError(List.of());
  }

  private ResponseEntity<ErrorResponse> buildError(HttpStatus status, ApiException ex) {
    return ResponseEntity.status(status).body(body(ex.code(), ex.getMessage(), null, null));
  }

  private ResponseEntity<ErrorResponse> validationError(List<ErrorDetail> details) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            body(
                ErrorConstants.Code.VALIDATION_ERROR,
                ErrorConstants.Message.VALIDATION_ERROR,
                details,
                null));
  }

  private ErrorResponse body(
      String code, String message, List<ErrorDetail> details, String service) {
    return new ErrorResponse(code, message, details, service, Instant.now());
  }

  private String lastPathNode(ConstraintViolation<?> violation) {
    String path = violation.getPropertyPath().toString();
    int lastDot = path.lastIndexOf('.');
    return lastDot >= 0 ? path.substring(lastDot + 1) : path;
  }
}
