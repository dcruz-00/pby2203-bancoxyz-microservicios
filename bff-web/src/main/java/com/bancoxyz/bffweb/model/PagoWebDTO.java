package com.bancoxyz.bffweb.model;

import java.time.Instant;
import java.util.UUID;

public record PagoWebDTO(
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
