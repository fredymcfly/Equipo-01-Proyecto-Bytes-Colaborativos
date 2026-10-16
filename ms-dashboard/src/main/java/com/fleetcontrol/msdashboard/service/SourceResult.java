package com.fleetcontrol.msdashboard.service;

/**
 * Outcome of a call to a source service.
 *
 * @param service name of the service that was called
 * @param data answer of the service, or {@code null} when it did not answer
 */
public record SourceResult<T>(String service, T data) {

  /** Returns {@code true} when the service answered. */
  public boolean isAvailable() {
    return data != null;
  }
}
