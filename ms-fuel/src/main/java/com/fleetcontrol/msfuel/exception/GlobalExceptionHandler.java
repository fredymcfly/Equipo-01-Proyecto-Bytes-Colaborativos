package com.fleetcontrol.msfuel.exception;

import com.fleetcontrol.msfuel.exception.ApiErrorResponse.ApiFieldError;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones del servicio al cuerpo de error del contrato común. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /** La petición incumple una regla de negocio, como superar la capacidad del depósito. */
  @ExceptionHandler(InvalidRequestException.class)
  public ResponseEntity<ApiErrorResponse> handleInvalidRequest(InvalidRequestException ex) {
    return response(ex.getErrorCode(), ApiErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
  }

  /** Recurso inexistente en el servicio que lo posee. */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
    return response(ex.getErrorCode(), ApiErrorResponse.of(ex.getErrorCode()));
  }

  /** Conflicto con el estado actual del recurso. */
  @ExceptionHandler(BusinessConflictException.class)
  public ResponseEntity<ApiErrorResponse> handleConflict(BusinessConflictException ex) {
    return response(ex.getErrorCode(), ApiErrorResponse.of(ex.getErrorCode()));
  }

  /** Dependencia de otro microservicio caída. */
  @ExceptionHandler(DependencyUnavailableException.class)
  public ResponseEntity<ApiErrorResponse> handleDependency(DependencyUnavailableException ex) {
    return response(
        ex.getErrorCode(), ApiErrorResponse.ofDependency(ex.getErrorCode(), ex.getService()));
  }

  /** Campos del cuerpo de la petición que no cumplen sus validaciones. */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleInvalidBody(MethodArgumentNotValidException ex) {
    List<ApiFieldError> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> new ApiFieldError(error.getField(), error.getDefaultMessage()))
            .toList();
    return response(
        ErrorCode.VALIDATION_ERROR, ApiErrorResponse.of(ErrorCode.VALIDATION_ERROR, details));
  }

  /** Parámetros de la petición que no cumplen sus validaciones. */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleInvalidParams(ConstraintViolationException ex) {
    List<ApiFieldError> details =
        ex.getConstraintViolations().stream()
            .map(
                violation ->
                    new ApiFieldError(
                        violation.getPropertyPath().toString(), violation.getMessage()))
            .toList();
    return response(
        ErrorCode.VALIDATION_ERROR, ApiErrorResponse.of(ErrorCode.VALIDATION_ERROR, details));
  }

  private ResponseEntity<ApiErrorResponse> response(ErrorCode errorCode, ApiErrorResponse body) {
    return ResponseEntity.status(errorCode.getStatus()).body(body);
  }
}
