package com.bancoxyz.bffweb.model;

import java.time.Instant;

public record NotificacionWebDTO(Long id, String tipo, String mensaje, Instant fecha) {
}
