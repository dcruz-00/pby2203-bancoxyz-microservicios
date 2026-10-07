package com.bancoxyz.bffmovil.model;

/** Canal móvil: solo lo esencial de la cuenta, para reducir el tamaño de la respuesta. */
public record CuentaMovilDTO(
        Long cuentaId,
        Double saldo,
        String tipo,
        String estado
) {}
