package com.bancoxyz.cuentas.model;

import java.time.LocalDate;

/** Datos del cliente que cuentas necesita al abrir una cuenta (respuesta de clientes). */
public record ClienteResumen(Long id, String nombre, LocalDate fechaNacimiento, String estado) {
}
