package com.bancoxyz.coreapi.model;

public record RetiroRealizadoEvento(String idOperacion, Long cuentaId, Double monto, String fechaHora) {
}