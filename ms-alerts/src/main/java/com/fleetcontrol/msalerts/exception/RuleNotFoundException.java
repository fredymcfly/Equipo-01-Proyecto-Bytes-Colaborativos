package com.fleetcontrol.msalerts.exception;

import com.fleetcontrol.msalerts.constants.ErrorConstants;
import java.io.Serial;
import org.springframework.http.HttpStatus;

/** Thrown when an alert rule does not exist, or the given type is not a rule type. */
public class RuleNotFoundException extends ApiException {

  @Serial private static final long serialVersionUID = 1L;

  /** Creates the exception with the standard code and message. */
  public RuleNotFoundException() {
    super(
        HttpStatus.NOT_FOUND,
        ErrorConstants.Code.RULE_NOT_FOUND,
        ErrorConstants.Message.RULE_NOT_FOUND);
  }
}
