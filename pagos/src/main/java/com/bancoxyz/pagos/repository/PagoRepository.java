package com.bancoxyz.pagos.repository;

import com.bancoxyz.pagos.model.PagoDTO;
import com.bancoxyz.pagos.model.TipoPago;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PagoRepository {

    private static final String SELECT_PAGOS = "SELECT id_operacion, tipo, cuenta_id, cuenta_destino, monto, comercio, "
            + "estado, saldo_resultante, motivo, fecha_creacion FROM pagos";

    private final JdbcTemplate jdbcTemplate;

    public PagoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void crearPendiente(UUID idOperacion, TipoPago tipo, Long cuentaId, Long cuentaDestinoId, Double monto,
                               String comercio) {
        jdbcTemplate.update("INSERT INTO pagos (id_operacion, tipo, cuenta_id, cuenta_destino, monto, comercio, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'PENDIENTE')", idOperacion, tipo.name(), cuentaId, cuentaDestinoId, monto,
                comercio);
    }

    public void completar(UUID idOperacion, Double saldoResultante) {
        jdbcTemplate.update("UPDATE pagos SET estado = 'COMPLETADO', saldo_resultante = ?, fecha_actualizacion = now() "
                + "WHERE id_operacion = ?", saldoResultante, idOperacion);
    }

    /** Marca el pago como RECHAZADO o FALLIDO, con el motivo. */
    public void finalizarSinAplicar(UUID idOperacion, String estado, String motivo) {
        String motivoAcotado = motivo == null ? null : motivo.substring(0, Math.min(motivo.length(), 300));
        jdbcTemplate.update("UPDATE pagos SET estado = ?, motivo = ?, fecha_actualizacion = now() "
                + "WHERE id_operacion = ?", estado, motivoAcotado, idOperacion);
    }

    public Optional<PagoDTO> buscar(UUID idOperacion) {
        return jdbcTemplate.query(SELECT_PAGOS + " WHERE id_operacion = ?", this::mapear, idOperacion)
                .stream()
                .findFirst();
    }

    /** Pagos más recientes primero; si cuentaId es null, de todas las cuentas. */
    public List<PagoDTO> listar(Long cuentaId, int limite) {
        if (cuentaId == null) {
            return jdbcTemplate.query(SELECT_PAGOS + " ORDER BY fecha_creacion DESC LIMIT ?", this::mapear, limite);
        }
        return jdbcTemplate.query(SELECT_PAGOS + " WHERE cuenta_id = ? OR cuenta_destino = ? "
                + "ORDER BY fecha_creacion DESC LIMIT ?", this::mapear, cuentaId, cuentaId, limite);
    }

    private PagoDTO mapear(ResultSet rs, int rowNum) throws SQLException {
        return new PagoDTO(
                rs.getObject("id_operacion", UUID.class),
                rs.getString("tipo"),
                rs.getLong("cuenta_id"),
                rs.getObject("cuenta_destino", Long.class),
                rs.getDouble("monto"),
                rs.getString("comercio"),
                rs.getString("estado"),
                rs.getObject("saldo_resultante") == null ? null : rs.getDouble("saldo_resultante"),
                rs.getString("motivo"),
                rs.getTimestamp("fecha_creacion").toInstant());
    }
}
