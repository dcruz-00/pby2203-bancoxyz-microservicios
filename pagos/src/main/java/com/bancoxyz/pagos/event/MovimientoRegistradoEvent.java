package com.bancoxyz.pagos.event;

import java.time.Instant;

public record MovimientoRegistradoEvent(
        String idOperacion,
        Long cuentaId,
        Double monto,
        Instant fechaHora
) {}