package com.bancoxyz.cuentas.model;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record AperturaCuentaRequest(
        @NotNull(message = "El cliente es obligatorio")
        Long clienteId,

        @NotNull(message = "El tipo es obligatorio")
        @Pattern(regexp = "ahorro|corriente", message = "El tipo debe ser ahorro o corriente")
        String tipo,

        @NotNull(message = "El saldo inicial es obligatorio")
        @PositiveOrZero(message = "El saldo inicial no puede ser negativo")
        @Digits(integer = 10, fraction = 2,
                message = "El saldo admite hasta 10 dígitos enteros y 2 decimales")
        Double saldoInicial) {
}
