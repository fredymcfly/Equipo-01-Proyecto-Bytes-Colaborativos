package com.fleetcontrol.msfuel.exception;

/** El recurso sobre el que opera la petición no existe en el servicio que lo posee. */
public class ResourceNotFoundException extends ApiException {

  /**
   * Crea el error de recurso inexistente.
   *
   * @param errorCode código del contrato común
   */
  public ResourceNotFoundException(ErrorCode errorCode) {
    super(errorCode, errorCode.getDefaultMessage());
  }
}
