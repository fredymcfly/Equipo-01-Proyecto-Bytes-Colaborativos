package com.fleetcontrol.msroutes.exception;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Handles exceptions and maps them to the common API error contract. */
@ControllerAdvice
public class GlobalExceptionHandler {

  private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
  private static final String VALIDATION_MESSAGE = "The request contains invalid values";

  /**
   * Handles errors caused by an unavailable downstream service.
   *
   * @param ex exception containing the unavailable service
   * @return response containing the service-unavailable error
   */
  @ExceptionHandler(ServiceUnavailableException.class)
  public ResponseEntity<ApiError> handleUnavailable(ServiceUnavailableException ex) {
    return ResponseEntity.status(ex.getStatus())
        .body(ApiError.of(ex.getCode(), ex.getMessage(), null, ex.getService()));
  }

  /**
   * Handles application errors with a defined HTTP status and error code.
   *
   * @param ex application exception
   * @return response containing the application error
   */
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiError> handleApi(ApiException ex) {
    return ResponseEntity.status(ex.getStatus())
        .body(ApiError.of(ex.getCode(), ex.getMessage(), null, null));
  }

  /**
   * Handles validation errors raised from request bodies.
   *
   * @param ex validation exception
   * @return response containing the validation details
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex) {
    List<ApiError.Detail> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(e -> new ApiError.Detail(e.getField(), e.getDefaultMessage()))
            .toList();
    return validationError(VALIDATION_MESSAGE, details);
  }

  /**
   * Handles validation errors raised from request parameters.
   *
   * @param ex validation exception
   * @return response containing the validation details
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiError> handleParamValidation(ConstraintViolationException ex) {
    List<ApiError.Detail> details =
        ex.getConstraintViolations().stream()
            .map(
                v -> {
                  String path = v.getPropertyPath().toString();
                  return new ApiError.Detail(
                      path.substring(path.lastIndexOf('.') + 1), v.getMessage());
                })
            .toList();
    return validationError(VALIDATION_MESSAGE, details);
  }

  /**
   * Handles request parameters with an incompatible type.
   *
   * @param ex type mismatch exception
   * @return response containing the validation error
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return validationError(
        VALIDATION_MESSAGE, List.of(new ApiError.Detail(ex.getName(), "Value invalid")));
  }

  /**
   * Handles malformed or unreadable request bodies.
   *
   * @param ex unreadable body exception
   * @return response containing the validation error
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
    return validationError("The request body is invalid", List.of());
  }

  /**
   * Handles requests without a required query parameter.
   *
   * @param ex missing parameter exception
   * @return response containing the validation error
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex) {
    return validationError(
        VALIDATION_MESSAGE,
        List.of(new ApiError.Detail(ex.getParameterName(), "Parameter is required")));
  }

  private ResponseEntity<ApiError> validationError(String message, List<ApiError.Detail> details) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiError.of(VALIDATION_ERROR, message, details, null));
  }
}
