package com.fleetcontrol.msroutes.config;

import feign.Request;
import feign.RetryableException;
import feign.Retryer;

/**
 * Retries a call that could not reach the service, but only when it is a GET. A PATCH or a POST may
 * have been applied before the failure, so repeating it could change the vehicle twice.
 */
public class GetOnlyRetryer implements Retryer {

  private final Retryer delegate;

  /**
   * Creates the retryer.
   *
   * @param period initial pause between attempts, in milliseconds
   * @param maxPeriod longest pause between attempts, in milliseconds
   * @param maxAttempts attempts including the first one
   */
  public GetOnlyRetryer(long period, long maxPeriod, int maxAttempts) {
    this(new Retryer.Default(period, maxPeriod, maxAttempts));
  }

  private GetOnlyRetryer(Retryer delegate) {
    this.delegate = delegate;
  }

  @Override
  public void continueOrPropagate(RetryableException e) {
    if (e.method() != Request.HttpMethod.GET) {
      throw e;
    }
    delegate.continueOrPropagate(e);
  }

  @Override
  public Retryer clone() {
    return new GetOnlyRetryer(delegate.clone());
  }
}
