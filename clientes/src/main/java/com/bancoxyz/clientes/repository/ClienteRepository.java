package com.bancoxyz.clientes.repository;

import com.bancoxyz.clientes.model.ClienteDTO;
import com.bancoxyz.clientes.model.ClienteRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Repository
public class ClienteRepository {

    private static final String SELECT_CLIENTES = "SELECT id, rut, nombre, email, telefono, fecha_nacimiento, "
            + "estado, fecha_registro FROM clientes";

    private final JdbcTemplate jdbcTemplate;

    public ClienteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ClienteDTO> findAll() {
        return jdbcTemplate.query(SELECT_CLIENTES + " ORDER BY id", this::mapear);
    }

    public Optional<ClienteDTO> findById(Long id) {
        return jdbcTemplate.query(SELECT_CLIENTES + " WHERE id = ?", this::mapear, id).stream().findFirst();
    }

    public boolean existe(Long id) {
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM clientes WHERE id = ?", Integer.class, id);
        return total != null && total > 0;
    }

    /** Inserta el cliente y devuelve su id. Un RUT repetido lanza DuplicateKeyException. */
    public Long crear(ClienteRequest c) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO clientes (rut, nombre, email, telefono, fecha_nacimiento) VALUES (?, ?, ?, ?, ?) "
                        + "RETURNING id",
                Long.class, c.rut(), c.nombre(), c.email(), c.telefono(),
                c.fechaNacimiento() == null ? null : Date.valueOf(c.fechaNacimiento()));
    }

    /** Actualiza el perfil; devuelve false si el cliente no existe. */
    public boolean actualizar(Long id, ClienteRequest c) {
        return jdbcTemplate.update(
                "UPDATE clientes SET rut = ?, nombre = ?, email = ?, telefono = ?, fecha_nacimiento = ? WHERE id = ?",
                c.rut(), c.nombre(), c.email(), c.telefono(),
                c.fechaNacimiento() == null ? null : Date.valueOf(c.fechaNacimiento()), id) == 1;
    }

    private ClienteDTO mapear(ResultSet rs, int rowNum) throws SQLException {
        Date nacimiento = rs.getDate("fecha_nacimiento");
        return new ClienteDTO(
                rs.getLong("id"),
                rs.getString("rut"),
                rs.getString("nombre"),
                rs.getString("email"),
                rs.getString("telefono"),
                nacimiento == null ? null : nacimiento.toLocalDate(),
                rs.getString("estado"),
                rs.getTimestamp("fecha_registro").toInstant());
    }
}
