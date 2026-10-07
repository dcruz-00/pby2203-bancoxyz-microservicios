package com.bancoxyz.bffmovil.model;

import java.util.UUID;

/** Resultado liviano de una operación hecha desde la app. */
public record OperacionMovilDTO(UUID idOperacion, String estado, Double saldo) {
}
