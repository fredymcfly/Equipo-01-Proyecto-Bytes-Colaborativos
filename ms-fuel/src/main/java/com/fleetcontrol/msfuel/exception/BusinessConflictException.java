package com.fleetcontrol.msfuel.exception;

/** La petición entra en conflicto con el estado actual del recurso. */
public class BusinessConflictException extends ApiException {

  /**
   * Crea el error de conflicto de negocio.
   *
   * @param errorCode código del contrato común
   */
  public BusinessConflictException(ErrorCode errorCode) {
    super(errorCode, errorCode.getDefaultMessage());
  }
}
