package com.bancoxyz.bffmovil.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PagoMovilRequest(
        @NotNull(message = "La cuenta es obligatoria")
        Long cuentaId,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor que cero")
        Double monto,

        @NotBlank(message = "El comercio es obligatorio")
        String comercio) {
}
