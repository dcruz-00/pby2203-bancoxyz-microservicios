package com.bancoxyz.pagos.model;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Pago de un servicio o comercio con cargo a una cuenta. */
public record PagoServicioRequest(
        @NotNull(message = "La cuenta es obligatoria")
        Long cuentaId,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2, message = "El monto admite hasta 10 dígitos enteros y 2 decimales")
        Double monto,

        @NotBlank(message = "El comercio es obligatorio")
        @Size(max = 100, message = "El comercio admite hasta 100 caracteres")
        String comercio) {
}
