package com.bancoxyz.clientes.repository;

import com.bancoxyz.clientes.model.NotificacionDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificacionRepository {

    private final JdbcTemplate jdbcTemplate;

    public NotificacionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Guarda la notificación. Si el mismo evento ya se había registrado para el
     * cliente (entrega repetida de Kafka), no hace nada y devuelve false.
     */
    public boolean guardar(Long clienteId, String tipo, String mensaje, String idEvento) {
        return jdbcTemplate.update(
                "INSERT INTO notificaciones (cliente_id, tipo, mensaje, id_evento) VALUES (?, ?, ?, ?) "
                        + "ON CONFLICT (id_evento, cliente_id) DO NOTHING",
                clienteId, tipo, mensaje, idEvento) == 1;
    }

    /** Notificaciones del cliente, más recientes primero. */
    public List<NotificacionDTO> listar(Long clienteId, int limite) {
        return jdbcTemplate.query(
                "SELECT id, cliente_id, tipo, mensaje, id_evento, fecha FROM notificaciones "
                        + "WHERE cliente_id = ? ORDER BY id DESC LIMIT ?",
                (rs, rowNum) -> new NotificacionDTO(
                        rs.getLong("id"),
                        rs.getLong("cliente_id"),
                        rs.getString("tipo"),
                        rs.getString("mensaje"),
                        rs.getString("id_evento"),
                        rs.getTimestamp("fecha").toInstant()),
                clienteId, limite);
    }
}
