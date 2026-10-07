package com.bancoxyz.bffweb.model;

import java.time.Instant;
import java.time.LocalDate;

public record ClienteWebDTO(
        Long id,
        String rut,
        String nombre,
        String email,
        String telefono,
        LocalDate fechaNacimiento,
        String estado,
        Instant fechaRegistro
) {}
