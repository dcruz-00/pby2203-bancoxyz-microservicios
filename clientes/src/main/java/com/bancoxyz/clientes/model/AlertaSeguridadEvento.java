package com.bancoxyz.clientes.model;

/** Evento del tópico alerta-seguridad (lo publica cuentas). */
public record AlertaSeguridadEvento(
        String idEvento,
        String tipo,
        Long cuentaId,
        Long clienteId,
        Double monto,
        String detalle,
        String fechaHora) {
}
