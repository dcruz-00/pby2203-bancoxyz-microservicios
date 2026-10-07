package com.bancoxyz.cuentas.model;

/**
 * Evento del tópico alerta-seguridad.
 *
 * @param idEvento identificador único, para que el consumidor descarte duplicados
 * @param tipo     motivo de la alerta (por ejemplo, MONTO_ELEVADO o CUENTA_CERRADA)
 */
public record AlertaSeguridadEvento(
        String idEvento,
        String tipo,
        Long cuentaId,
        Long clienteId,
        Double monto,
        String detalle,
        String fechaHora) {
}
