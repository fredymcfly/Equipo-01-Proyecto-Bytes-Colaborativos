package com.fleetcontrol.msmaintenance.exception;

import com.fleetcontrol.msmaintenance.constants.ErrorConstants;

/** Thrown when a vehicle already has an active plan of the same type. */
public class PlanAlreadyExistsException extends ApiException {

  private static final long serialVersionUID = 1L;

  /** Creates the exception with the default message. */
  public PlanAlreadyExistsException() {
    super(ErrorConstants.Code.PLAN_ALREADY_EXISTS, ErrorConstants.Message.PLAN_ALREADY_EXISTS);
  }
}
