package com.bancoxyz.bffcajeros.config;

/** El canal rechaza la operación antes de llamar al microservicio (por ejemplo, límite por operación). */
public class OperacionRechazadaException extends RuntimeException {
    public OperacionRechazadaException(String mensaje) {
        super(mensaje);
    }
}
