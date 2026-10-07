package com.bancoxyz.cuentas.exception;

/** La operación no es válida para el estado actual de la cuenta u operación (409). */
public class OperacionNoPermitidaException extends RuntimeException {
    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
