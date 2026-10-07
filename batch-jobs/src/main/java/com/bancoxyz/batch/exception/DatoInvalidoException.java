package com.bancoxyz.batch.exception;

/**
 * Un registro del CSV no cumple las reglas de consistencia del proceso. El Step
 * la omite (skip): el registro queda en registros_rechazados con su código y
 * motivo, y el job continúa.
 *
 * @see com.bancoxyz.batch.listeners.RegistroRechazadoListener
 */
public class DatoInvalidoException extends RuntimeException {

    private final String codigo;

    /**
     * @param codigo categoría del rechazo (por ejemplo, MONTO_VACIO o DUPLICADO),
     *               para poder contar los rechazos por tipo
     */
    public DatoInvalidoException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
