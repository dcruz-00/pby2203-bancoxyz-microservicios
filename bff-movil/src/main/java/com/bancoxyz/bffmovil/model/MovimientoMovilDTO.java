package com.bancoxyz.bffmovil.model;

import java.time.Instant;

/** Movimiento resumido para la app: tipo, monto, estado y fecha. */
public record MovimientoMovilDTO(String tipo, Double monto, String estado, Instant fecha) {
}
