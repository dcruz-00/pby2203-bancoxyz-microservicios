package com.bancoxyz.cuentas.exception;

/** La cuenta existe pero está cerrada y no admite operaciones (409). */
public class CuentaInactivaException extends RuntimeException {
    public CuentaInactivaException(String mensaje) {
        super(mensaje);
    }
}
