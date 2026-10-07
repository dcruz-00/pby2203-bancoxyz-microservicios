package com.bancoxyz.pagos.model;

import java.util.UUID;

/** Respuesta de cuentas a una operación aplicada. */
public record OperacionCuentasResultado(UUID idOperacion, TipoPago tipo, Long cuentaId, Long clienteId, Double saldo,
                                        Long cuentaDestinoId, Long clienteDestinoId, boolean repetida) {
}
