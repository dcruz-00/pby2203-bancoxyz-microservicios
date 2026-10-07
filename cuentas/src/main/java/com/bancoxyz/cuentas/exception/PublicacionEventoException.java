package com.bancoxyz.cuentas.exception;

/** No se pudo publicar un evento en Kafka dentro del tiempo máximo. */
public class PublicacionEventoException extends RuntimeException {
    public PublicacionEventoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}