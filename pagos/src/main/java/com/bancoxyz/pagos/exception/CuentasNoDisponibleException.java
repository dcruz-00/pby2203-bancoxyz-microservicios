package com.bancoxyz.pagos.exception;

/** cuentas no respondió tras los reintentos, o el circuito está abierto. */
public class CuentasNoDisponibleException extends RuntimeException {
    public CuentasNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
