package com.bancoxyz.cuentas.model;

import java.time.LocalDate;

public record CuentaDTO(
        Long cuentaId,
        Long clienteId,
        String nombre,
        Double saldo,
        Integer edad,
        String tipo,
        String estado,
        Double interesGenerado,
        LocalDate fechaCalculo
) {}
