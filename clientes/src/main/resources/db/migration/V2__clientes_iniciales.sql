-- Titulares de las cuentas heredadas (101 a 108 en el microservicio cuentas).
-- Los ids se fijan para que coincidan con cuentas.cliente_id.
INSERT INTO clientes (id, rut, nombre, email, telefono, fecha_nacimiento) VALUES
    (1, '11111111-1', 'John Doe',     'john.doe@correo.cl',     '+56911111111', '1996-05-10'),
    (2, '22222222-2', 'Jane Smith',   'jane.smith@correo.cl',   '+56922222222', '2001-02-14'),
    (3, '33333333-3', 'Bob Johnson',  'bob.johnson@correo.cl',  '+56933333333', '1996-08-20'),
    (4, '44444444-4', 'Alice Brown',  'alice.brown@correo.cl',  '+56944444444', '1981-03-03'),
    (5, '55555555-5', 'Diana Prince', 'diana.prince@correo.cl', '+56955555555', '1986-07-07'),
    (6, '66666666-6', 'Steve Rogers', 'steve.rogers@correo.cl', '+56966666666', '1946-07-04');

-- Los clientes nuevos continúan desde el 7
SELECT setval('clientes_id_seq', 6);
