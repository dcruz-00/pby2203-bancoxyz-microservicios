package com.bancoxyz.clientes.model;

import java.time.Instant;
import java.time.LocalDate;

public record ClienteDTO(
        Long id,
        String rut,
        String nombre,
        String email,
        String telefono,
        LocalDate fechaNacimiento,
        String estado,
        Instant fechaRegistro
) {}
