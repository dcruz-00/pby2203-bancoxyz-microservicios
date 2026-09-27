package com.bancoxyz.coreapi.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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

    public record OperacionRevertida(Long cuentaId, Double monto) {
    }

    /**
     * Marca la operación como CONFIRMADA solo si sigue PENDIENTE. Devuelve true si
     * cambió.
     */
    public boolean confirmar(UUID idOperacion) {
        return jdbcTemplate.update(
                "UPDATE operaciones SET estado = 'CONFIRMADA' WHERE id_operacion = ? AND estado = 'PENDIENTE'",
                idOperacion) == 1;
    }

    /**
     * Marca la operación como REVERTIDA solo si sigue PENDIENTE, y devuelve su
     * cuenta y monto guardados.
     */
    public Optional<OperacionRevertida> revertir(UUID idOperacion) {
        List<OperacionRevertida> filas = jdbcTemplate.query(
                "UPDATE operaciones SET estado = 'REVERTIDA' WHERE id_operacion = ? AND estado = 'PENDIENTE' "
                        + "RETURNING cuenta_id, monto",
                (rs, rowNum) -> new OperacionRevertida(rs.getLong("cuenta_id"), rs.getDouble("monto")),
                idOperacion);
        return filas.stream().findFirst();
    }
}