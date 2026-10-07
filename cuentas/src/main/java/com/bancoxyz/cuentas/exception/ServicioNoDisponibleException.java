package com.bancoxyz.cuentas.exception;

/** Una dependencia (por ejemplo, clientes) no responde; se aplicó el comportamiento alternativo (503). */
public class ServicioNoDisponibleException extends RuntimeException {
    public ServicioNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
