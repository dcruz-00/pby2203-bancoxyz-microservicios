package com.bancoxyz.coreapi.model;

public record RespuestaMovimientoEvento(String idOperacion, Long cuentaId, Double monto,
                                        String fechaHora, String motivo) {
}