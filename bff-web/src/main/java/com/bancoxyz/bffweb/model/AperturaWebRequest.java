package com.bancoxyz.bffweb.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/** La validación se adelanta en el BFF para no llamar a cuentas con datos inválidos. */
public record AperturaWebRequest(
        @NotNull(message = "El cliente es obligatorio")
        Long clienteId,

        @NotNull(message = "El tipo es obligatorio")
        @Pattern(regexp = "ahorro|corriente", message = "El tipo debe ser ahorro o corriente")
        String tipo,

        @NotNull(message = "El saldo inicial es obligatorio")
        @PositiveOrZero(message = "El saldo inicial no puede ser negativo")
        Double saldoInicial) {
}
