package com.bancoxyz.pagos.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Cuerpo de error (ProblemDetail) que devuelve cuentas. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ErrorCuentas(String title, String detail, Integer status) {
}
