package com.fleetcontrol.msmaintenance.exception;

import com.fleetcontrol.msmaintenance.constants.ErrorConstants;
import com.fleetcontrol.msmaintenance.models.dto.response.ErrorDetail;
import java.util.List;

/** Thrown when a request is well formed but a business-level validation fails. */
public class ValidationFailedException extends ApiException {

  private static final long serialVersionUID = 1L;

  private final transient List<ErrorDetail> details;

  /** Creates the exception with the field-level details of the failure. */
  public ValidationFailedException(List<ErrorDetail> details) {
    super(ErrorConstants.Code.VALIDATION_ERROR, ErrorConstants.Message.VALIDATION_ERROR);
    this.details = List.copyOf(details);
  }

  /** Field-level details of the failure. */
  public List<ErrorDetail> details() {
    return details;
  }
}
