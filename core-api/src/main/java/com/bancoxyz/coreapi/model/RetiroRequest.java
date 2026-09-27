package com.bancoxyz.coreapi.model;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RetiroRequest(
        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2,
                message = "El monto admite hasta 10 dígitos enteros y 2 decimales")
        Double monto) {
}