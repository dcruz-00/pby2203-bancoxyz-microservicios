package com.bancoxyz.pagos.event;

import java.time.Instant;

public record MovimientoFallidoEvent(
        String idOperacion,
        Long cuentaId,
        Double monto,
        Instant fechaHora,
        String motivo
) {}