package com.bancoxyz.pagos.exception;

import org.springframework.http.HttpStatusCode;

/**
 * cuentas rechazó la operación por una regla de negocio (saldo insuficiente,
 * cuenta inexistente o cerrada...). No es una falla técnica: no se reintenta ni
 * abre el circuito.
 */
public class RechazoCuentasException extends RuntimeException {

    private final HttpStatusCode status;

    public RechazoCuentasException(HttpStatusCode status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public HttpStatusCode getStatus() {
        return status;
    }
}
