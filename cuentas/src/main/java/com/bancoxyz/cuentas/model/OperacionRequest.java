package com.bancoxyz.cuentas.model;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

/**
 * Operación que solicita el microservicio pagos. El id de operación lo genera
 * pagos: si reintenta la misma solicitud, cuentas la reconoce y no la aplica dos
 * veces.
 */
public record OperacionRequest(
        @NotNull(message = "El id de operación es obligatorio")
        UUID idOperacion,

        @NotNull(message = "El tipo es obligatorio")
        TipoOperacion tipo,

        @NotNull(message = "La cuenta es obligatoria")
        Long cuentaId,

        // Solo para transferencias
        Long cuentaDestinoId,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor que cero")
        @Digits(integer = 10, fraction = 2,
                message = "El monto admite hasta 10 dígitos enteros y 2 decimales")
        Double monto) {
}
