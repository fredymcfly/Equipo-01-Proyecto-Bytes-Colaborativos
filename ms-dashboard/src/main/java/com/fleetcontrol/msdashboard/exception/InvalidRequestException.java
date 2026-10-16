package com.fleetcontrol.msdashboard.exception;

import com.fleetcontrol.msdashboard.constants.ErrorConstants;
import com.fleetcontrol.msdashboard.models.dto.response.ErrorDetail;
import java.io.Serial;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Thrown when the combination of parameters of a request is not valid. */
public class InvalidRequestException extends ApiException {

  @Serial private static final long serialVersionUID = 1L;

  private final transient List<ErrorDetail> details;

  /** Creates the exception for the field that breaks a rule and the reason why. */
  public InvalidRequestException(String field, String reason) {
    super(
        HttpStatus.BAD_REQUEST,
        ErrorConstants.Code.VALIDATION_ERROR,
        ErrorConstants.Message.VALIDATION_ERROR);
    this.details = List.of(new ErrorDetail(field, reason));
  }

  /** Field-level details written in the response. */
  public List<ErrorDetail> details() {
    return details;
  }
}
