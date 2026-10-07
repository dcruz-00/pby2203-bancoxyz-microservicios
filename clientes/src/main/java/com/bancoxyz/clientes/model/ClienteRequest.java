package com.bancoxyz.clientes.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Datos para registrar o actualizar el perfil de un cliente. */
public record ClienteRequest(
        @NotBlank(message = "El RUT es obligatorio")
        @Pattern(regexp = "\\d{7,8}-[\\dkK]", message = "El RUT debe tener el formato 12345678-9, sin puntos")
        String rut,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre admite hasta 100 caracteres")
        String nombre,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no es válido")
        String email,

        @Pattern(regexp = "\\+?\\d{8,15}", message = "El teléfono debe tener entre 8 y 15 dígitos")
        String telefono,

        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate fechaNacimiento) {
}
