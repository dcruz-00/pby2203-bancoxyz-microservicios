package com.bancoxyz.cuentas.exception;

public class CuentaNoEncontradaException extends RuntimeException {
    public CuentaNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}