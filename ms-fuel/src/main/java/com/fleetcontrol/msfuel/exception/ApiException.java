package com.fleetcontrol.msfuel.exception;

/**
 * Error de la API con su código del contrato común.
 *
 * <p>Los subtipos añaden el dato que necesita su caso: el servicio caído en {@link
 * DependencyUnavailableException}, por ejemplo.
 */
public class ApiException extends RuntimeException {

  private final ErrorCode errorCode;

  /**
   * Crea el error con el mensaje por defecto de su código.
   *
   * @param errorCode código del contrato común
   * @param message
   */
  public ApiException(ErrorCode errorCode, String message) {
    super(errorCode.getDefaultMessage());
    this.errorCode = errorCode;
  }

  /** Código del contrato común que se devuelve al cliente. */
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
