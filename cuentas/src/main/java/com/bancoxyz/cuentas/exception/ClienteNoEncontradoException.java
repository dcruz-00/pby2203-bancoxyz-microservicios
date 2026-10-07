package com.bancoxyz.cuentas.exception;

/** El cliente indicado no existe en el microservicio clientes (422). */
public class ClienteNoEncontradoException extends RuntimeException {
    public ClienteNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
