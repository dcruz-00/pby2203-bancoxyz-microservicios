package com.bancoxyz.cuentas.exception;

/** La solicitud es inconsistente aunque cada campo sea válido por separado (400). */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
