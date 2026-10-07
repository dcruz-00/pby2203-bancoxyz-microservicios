package com.bancoxyz.pagos.model;

import java.util.UUID;

/** Solicitud a POST /api/cuentas/operaciones del microservicio cuentas. */
public record OperacionCuentasRequest(UUID idOperacion, TipoPago tipo, Long cuentaId, Long cuentaDestinoId,
                                      Double monto) {
}
