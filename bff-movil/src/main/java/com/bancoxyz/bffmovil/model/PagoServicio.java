package com.bancoxyz.bffmovil.model;

import java.time.Instant;
import java.util.UUID;

/** Respuesta de pagos (se leen solo los campos que el canal móvil necesita). */
public record PagoServicio(UUID idOperacion, String tipo, Double monto, String estado, Double saldoResultante,
                           Instant fechaCreacion) {
}
