package com.bancoxyz.bffweb.model;

import java.time.LocalDate;

/** Canal web: todos los datos de la cuenta. */
public record CuentaWebDTO(
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
