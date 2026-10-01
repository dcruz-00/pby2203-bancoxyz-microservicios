package com.bancoxyz.movimientos.repository;

import com.bancoxyz.movimientos.event.RetiroRealizadoEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

import com.bancoxyz.movimientos.model.MovimientoDTO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class MovimientoRepository {

    private final JdbcTemplate jdbcTemplate;

    public MovimientoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existePorIdOperacion(String idOperacion) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM movimientos WHERE operacion_id = ?",
                Integer.class, idOperacion);
        return count != null && count > 0;
    }

    public void guardar(RetiroRealizadoEvent evento) {
        jdbcTemplate.update(
                "INSERT INTO movimientos (operacion_id, cuenta_id, monto, fecha_operacion) VALUES (?, ?, ?, ?)",
                evento.idOperacion(),
                evento.cuentaId(),
                evento.monto(),
                Timestamp.from(evento.fechaHora()));
    }

    private static final String SELECT_MOVIMIENTOS = "SELECT id, operacion_id, cuenta_id, monto, fecha_operacion, fecha_registro FROM movimientos";

    /**
     * Lista los movimientos, más recientes primero; si cuentaId es null, trae
     * todos.
     */
    public List<MovimientoDTO> listar(Long cuentaId) {
        if (cuentaId == null) {
            return jdbcTemplate.query(SELECT_MOVIMIENTOS + " ORDER BY id DESC", this::mapear);
        }
        return jdbcTemplate.query(SELECT_MOVIMIENTOS + " WHERE cuenta_id = ? ORDER BY id DESC",
                this::mapear, cuentaId);
    }

    public Optional<MovimientoDTO> buscarPorIdOperacion(String idOperacion) {
        return jdbcTemplate.query(SELECT_MOVIMIENTOS + " WHERE operacion_id = ?", this::mapear, idOperacion)
                .stream()
                .findFirst();
    }

    private MovimientoDTO mapear(ResultSet rs, int rowNum) throws SQLException {
        return new MovimientoDTO(
                rs.getLong("id"),
                rs.getString("operacion_id"),
                rs.getLong("cuenta_id"),
                rs.getDouble("monto"),
                rs.getTimestamp("fecha_operacion").toInstant(),
                rs.getTimestamp("fecha_registro").toInstant());
    }
}