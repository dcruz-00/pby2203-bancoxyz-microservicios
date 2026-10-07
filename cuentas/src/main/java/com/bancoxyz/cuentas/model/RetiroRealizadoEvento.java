package com.bancoxyz.cuentas.model;

public record RetiroRealizadoEvento(String idOperacion, Long cuentaId, Double monto, String fechaHora) {
}