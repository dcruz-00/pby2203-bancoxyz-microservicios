package com.bancoxyz.movimientos.repository;

import com.bancoxyz.movimientos.event.RetiroRealizadoEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

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
                Timestamp.from(evento.fechaHora())
        );
    }
}