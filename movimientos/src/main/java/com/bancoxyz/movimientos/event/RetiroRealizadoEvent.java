package com.bancoxyz.movimientos.event;

import java.time.Instant;

public record RetiroRealizadoEvent(
        String idOperacion,
        Long cuentaId,
        Double monto,
        Instant fechaHora
) {}