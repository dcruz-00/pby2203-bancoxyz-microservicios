package com.bancoxyz.pagos.event;

/** Evento del tópico transaccion-completada (lo consume clientes para notificar). */
public record TransaccionCompletadaEvent(
        String idOperacion,
        String tipo,
        Long cuentaId,
        Long clienteId,
        Double monto,
        Double saldo,
        Long cuentaDestinoId,
        Long clienteDestinoId,
        String fechaHora) {
}
