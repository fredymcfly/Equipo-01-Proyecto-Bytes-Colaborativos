package com.fleetcontrol.msfuel.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Respuesta de error del contrato común.
 *
 * <p>{@code details} solo se serializa en {@link ErrorCode#VALIDATION_ERROR} y {@code service} solo
 * en {@link ErrorCode#SERVICE_UNAVAILABLE}, por eso el include es NON_NULL.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
    String error, String message, List<ApiFieldError> details, Instant timestamp, String service) {

  /** Error sin campos de detalle, el caso habitual. */
  public static ApiErrorResponse of(ErrorCode errorCode) {
    return new ApiErrorResponse(
        errorCode.name(), errorCode.getDefaultMessage(), null, Instant.now(), null);
  }

  /** Error con un mensaje más concreto que el de su código, el caso de las reglas de negocio. */
  public static ApiErrorResponse of(ErrorCode errorCode, String message) {
    return new ApiErrorResponse(errorCode.name(), message, null, Instant.now(), null);
  }

  /** Error de validación, que sí lleva el detalle campo a campo. */
  public static ApiErrorResponse of(ErrorCode errorCode, List<ApiFieldError> details) {
    return new ApiErrorResponse(
        errorCode.name(), errorCode.getDefaultMessage(), details, Instant.now(), null);
  }

  /** Error de dependencia caída, que además dice qué servicio no responde. */
  public static ApiErrorResponse ofDependency(ErrorCode errorCode, String service) {
    return new ApiErrorResponse(
        errorCode.name(), errorCode.getDefaultMessage(), null, Instant.now(), service);
  }

  /** Campo rechazado de la petición y el motivo. */
  public record ApiFieldError(String field, String reason) {}
}
