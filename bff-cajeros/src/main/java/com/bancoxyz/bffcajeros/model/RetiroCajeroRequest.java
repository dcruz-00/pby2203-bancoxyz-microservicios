package com.bancoxyz.bffcajeros.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RetiroCajeroRequest(
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor que cero")
        Double monto) {
}
