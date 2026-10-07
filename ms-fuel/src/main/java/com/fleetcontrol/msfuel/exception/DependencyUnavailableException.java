package com.fleetcontrol.msfuel.exception;

/** Una dependencia de otro microservicio no responde. */
public class DependencyUnavailableException extends ApiException {

  private final String service;

  /**
   * Crea el error de dependencia caída.
   *
   * @param service nombre del servicio que no responde
   * @param errorCode código del contrato común
   */
  public DependencyUnavailableException(String service, ErrorCode errorCode) {
    super(errorCode, "Dependency unavailable: " + service);
    this.service = service;
  }

  /** Nombre del servicio que no responde, que viaja en la respuesta. */
  public String getService() {
    return service;
  }
}
