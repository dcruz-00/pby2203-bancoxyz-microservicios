package com.bancoxyz.clientes.model;

/** Evento del tópico transaccion-completada (lo publica pagos). */
public record TransaccionCompletadaEvento(
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
