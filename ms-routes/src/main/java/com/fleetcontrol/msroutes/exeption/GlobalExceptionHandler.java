package com.fleetcontrol.msroutes.exeption;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

public class GlobalExceptionHandler {

  private static final String VALIDATION_ERROR = "VALIDATION_ERROR";
  private static final String VALIDATION_MESSAGE = "The request contains invalid values";

  @ExceptionHandler(ServiceUnavailableException.class)
  public ResponseEntity<ApiError> handleUnavailable(ServiceUnavailableException ex) {
    return ResponseEntity.status(ex.getStatus())
            .body(ApiError.of(ex.getCode(), ex.getMessage(), null, ex.getService()));
  }

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<ApiError> handleApi(ApiException ex) {
    return ResponseEntity.status(ex.getStatus())
            .body(ApiError.of(ex.getCode(), ex.getMessage(), null, null));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex) {
    List<ApiError.Detail> details =
            ex.getBindingResult().getFieldErrors().stream()
                    .map(e -> new ApiError.Detail(e.getField(), e.getDefaultMessage()))
                    .toList();
    return validationError(VALIDATION_MESSAGE, details);
  }

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

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return validationError(
            VALIDATION_MESSAGE, List.of(new ApiError.Detail(ex.getName(), "Value invalid")));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException ex) {
    return validationError("The request body is invalid", List.of());
  }

  private ResponseEntity<ApiError> validationError(String message, List<ApiError.Detail> details) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiError.of(VALIDATION_ERROR, message, details, null));
  }
}
