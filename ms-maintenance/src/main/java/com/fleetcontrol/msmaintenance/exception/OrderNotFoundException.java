package com.fleetcontrol.msmaintenance.exception;

import com.fleetcontrol.msmaintenance.constants.ErrorConstants;

/** Thrown when a maintenance order does not exist. */
public class OrderNotFoundException extends ApiException {

  private static final long serialVersionUID = 1L;

  /** Creates the exception with the default message. */
  public OrderNotFoundException() {
    super(ErrorConstants.Code.ORDER_NOT_FOUND, ErrorConstants.Message.ORDER_NOT_FOUND);
  }
}
