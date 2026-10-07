package com.bancoxyz.cuentas.repository;

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

    /** Operación registrada, usada para reconocer reintentos de pagos. */
    public record Operacion(UUID idOperacion, String tipo, Long cuentaId, Long cuentaDestinoId, Double monto,
                            String estado) {
    }

    public void crearPendiente(UUID idOperacion, Long cuentaId, Double monto) {
        jdbcTemplate.update(
                "INSERT INTO operaciones (id_operacion, tipo, cuenta_id, monto, estado) "
                        + "VALUES (?, 'RETIRO', ?, ?, 'PENDIENTE')",
                idOperacion, cuentaId, monto);
    }

    /** Registra un depósito, pago o transferencia ya aplicado (lo solicita pagos de forma síncrona). */
    public void crearAplicada(UUID idOperacion, String tipo, Long cuentaId, Long cuentaDestinoId, Double monto) {
        jdbcTemplate.update(
                "INSERT INTO operaciones (id_operacion, tipo, cuenta_id, cuenta_destino, monto, estado) "
                        + "VALUES (?, ?, ?, ?, ?, 'APLICADA')",
                idOperacion, tipo, cuentaId, cuentaDestinoId, monto);
    }

    public Optional<Operacion> buscar(UUID idOperacion) {
        return jdbcTemplate.query(
                "SELECT id_operacion, tipo, cuenta_id, cuenta_destino, monto, estado FROM operaciones "
                        + "WHERE id_operacion = ?",
                (rs, rowNum) -> new Operacion(
                        rs.getObject("id_operacion", UUID.class),
                        rs.getString("tipo"),
                        rs.getLong("cuenta_id"),
                        rs.getObject("cuenta_destino", Long.class),
                        rs.getDouble("monto"),
                        rs.getString("estado")),
                idOperacion)
                .stream()
                .findFirst();
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
