package com.bancoxyz.clientes.model;

import java.time.Instant;

public record NotificacionDTO(
        Long id,
        Long clienteId,
        String tipo,
        String mensaje,
        String idEvento,
        Instant fecha
) {}
