package com.fleetcontrol.msmaintenance.exception;

import com.fleetcontrol.msmaintenance.constants.ErrorConstants;

/** Thrown when an order is not in the state required by the action. */
public class InvalidOrderStateException extends ApiException {

  private static final long serialVersionUID = 1L;

  /** Creates the exception with the default message. */
  public InvalidOrderStateException() {
    super(ErrorConstants.Code.INVALID_ORDER_STATE, ErrorConstants.Message.INVALID_ORDER_STATE);
  }
}
