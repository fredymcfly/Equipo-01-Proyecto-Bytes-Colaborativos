package com.fleetcontrol.msauth.models.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fleetcontrol.msauth.constants.ErrorConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Common error contract of the platform. {@code details} only appears in validation errors.
 *
 * @param error machine-readable error code
 * @param message human-readable message
 * @param details invalid fields, only for {@code VALIDATION_ERROR}
 * @param timestamp instant in which the error happened (UTC)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
    @Schema(example = "VALIDATION_ERROR") String error,
    @Schema(example = "The request contains invalid fields") String message,
    List<FieldDetail> details,
    @Schema(example = "2026-10-05T10:30:00Z") Instant timestamp) {

  /**
   * Invalid field of a validation error.
   *
   * @param field name of the field
   * @param reason why the value is not valid
   */
  public record FieldDetail(
      @Schema(example = "password") String field,
      @Schema(example = "must be between 8 and 72 characters long") String reason) {}

  /** Builds an error without details. */
  public static ErrorResponse of(String error, String message) {
    return new ErrorResponse(error, message, null, Instant.now());
  }

  /** Builds a {@code VALIDATION_ERROR} with the list of invalid fields. */
  public static ErrorResponse validation(String message, List<FieldDetail> details) {
    return new ErrorResponse(ErrorConstants.Code.VALIDATION_ERROR, message, details, Instant.now());
  }
}
