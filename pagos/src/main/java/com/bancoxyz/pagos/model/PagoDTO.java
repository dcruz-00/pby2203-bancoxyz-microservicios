package com.bancoxyz.pagos.model;

import java.time.Instant;
import java.util.UUID;

public record PagoDTO(
        UUID idOperacion,
        String tipo,
        Long cuentaId,
        Long cuentaDestinoId,
        Double monto,
        String comercio,
        String estado,
        Double saldoResultante,
        String motivo,
        Instant fechaCreacion
) {}
