package com.bancoxyz.bffmovil.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransferenciaMovilRequest(
        @NotNull(message = "La cuenta de origen es obligatoria")
        Long cuentaOrigenId,

        @NotNull(message = "La cuenta de destino es obligatoria")
        Long cuentaDestinoId,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor que cero")
        Double monto) {
}
