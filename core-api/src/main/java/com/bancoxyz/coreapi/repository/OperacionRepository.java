package com.bancoxyz.coreapi.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class OperacionRepository {

    private final JdbcTemplate jdbcTemplate;

    public OperacionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void crearPendiente(UUID idOperacion, Long cuentaId, Double monto) {
        jdbcTemplate.update(
                "INSERT INTO operaciones (id_operacion, cuenta_id, monto, estado) VALUES (?, ?, ?, 'PENDIENTE')",
                idOperacion, cuentaId, monto);
    }
}