package com.bancoxyz.cuentas.repository;

import com.bancoxyz.cuentas.exception.CuentaInactivaException;
import com.bancoxyz.cuentas.exception.CuentaNoEncontradaException;
import com.bancoxyz.cuentas.exception.SaldoInsuficienteException;
import com.bancoxyz.cuentas.model.CuentaDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class CuentaRepository {

    private static final String SELECT_CUENTAS = "SELECT cuenta_id, cliente_id, nombre, saldo, edad, tipo, estado, "
            + "interes_generado, fecha_calculo FROM cuentas";

    private final JdbcTemplate jdbcTemplate;

    public CuentaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CuentaDTO> findAll() {
        return jdbcTemplate.query(SELECT_CUENTAS + " ORDER BY cuenta_id", this::mapear);
    }

    public List<CuentaDTO> findByClienteId(Long clienteId) {
        return jdbcTemplate.query(SELECT_CUENTAS + " WHERE cliente_id = ? ORDER BY cuenta_id", this::mapear, clienteId);
    }

    public Optional<CuentaDTO> findById(Long cuentaId) {
        return jdbcTemplate.query(SELECT_CUENTAS + " WHERE cuenta_id = ?", this::mapear, cuentaId)
                .stream()
                .findFirst();
    }

    public CuentaDTO obtener(Long cuentaId) {
        return findById(cuentaId)
                .orElseThrow(() -> new CuentaNoEncontradaException("No existe la cuenta " + cuentaId));
    }

    /** Crea una cuenta ACTIVA con el siguiente número disponible y devuelve ese número. */
    public Long crear(Long clienteId, String nombre, Integer edad, String tipo, Double saldoInicial) {
        Long cuentaId = jdbcTemplate.queryForObject("SELECT nextval('cuentas_numero_seq')", Long.class);
        jdbcTemplate.update("INSERT INTO cuentas (cuenta_id, cliente_id, nombre, saldo, edad, tipo, estado) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'ACTIVA')", cuentaId, clienteId, nombre, saldoInicial, edad, tipo);
        return cuentaId;
    }

    /** Cierra la cuenta solo si está activa y con saldo cero. Devuelve true si cambió. */
    public boolean cerrar(Long cuentaId) {
        return jdbcTemplate.update(
                "UPDATE cuentas SET estado = 'CERRADA' WHERE cuenta_id = ? AND estado = 'ACTIVA' AND saldo = 0",
                cuentaId) == 1;
    }

    /**
     * Descuenta el monto en una sola sentencia condicionada (cuenta activa y saldo
     * suficiente). Así, dos cargos simultáneos nunca dejan el saldo negativo: el
     * segundo no encuentra saldo suficiente y no modifica nada.
     */
    public CuentaDTO debitar(Long cuentaId, Double monto) {
        int filas = jdbcTemplate.update(
                "UPDATE cuentas SET saldo = saldo - ? WHERE cuenta_id = ? AND estado = 'ACTIVA' AND saldo >= ?",
                monto, cuentaId, monto);
        if (filas == 0) {
            CuentaDTO cuenta = obtener(cuentaId);
            verificarActiva(cuenta);
            throw new SaldoInsuficienteException("Saldo insuficiente en cuenta " + cuentaId);
        }
        return obtener(cuentaId);
    }

    /** Abona el monto a una cuenta activa. */
    public CuentaDTO acreditar(Long cuentaId, Double monto) {
        int filas = jdbcTemplate.update(
                "UPDATE cuentas SET saldo = saldo + ? WHERE cuenta_id = ? AND estado = 'ACTIVA'",
                monto, cuentaId);
        if (filas == 0) {
            verificarActiva(obtener(cuentaId));
        }
        return obtener(cuentaId);
    }

    /** Compensación de la saga: devuelve el monto aunque la cuenta se haya cerrado entretanto. */
    public void devolver(Long cuentaId, Double monto) {
        jdbcTemplate.update("UPDATE cuentas SET saldo = saldo + ? WHERE cuenta_id = ?", monto, cuentaId);
    }

    private void verificarActiva(CuentaDTO cuenta) {
        if (!"ACTIVA".equals(cuenta.estado())) {
            throw new CuentaInactivaException("La cuenta " + cuenta.cuentaId() + " está cerrada");
        }
    }

    private CuentaDTO mapear(ResultSet rs, int rowNum) throws SQLException {
        Date fechaCalculo = rs.getDate("fecha_calculo");
        return new CuentaDTO(
                rs.getLong("cuenta_id"),
                rs.getObject("cliente_id", Long.class),
                rs.getString("nombre"),
                rs.getDouble("saldo"),
                rs.getObject("edad", Integer.class),
                rs.getString("tipo"),
                rs.getString("estado"),
                rs.getObject("interes_generado") == null ? null : rs.getDouble("interes_generado"),
                fechaCalculo == null ? null : fechaCalculo.toLocalDate());
    }
}
