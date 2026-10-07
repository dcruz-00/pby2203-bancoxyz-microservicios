package com.bancoxyz.pagos.model;

import java.time.Instant;

public record MovimientoDTO(
        Long id,
        String idOperacion,
        Long cuentaId,
        Double monto,
        Instant fechaOperacion,
        Instant fechaRegistro
) {}