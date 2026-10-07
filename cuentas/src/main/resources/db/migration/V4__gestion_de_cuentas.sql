-- Gestión de cuentas (EFT): la tabla heredada del Batch pasa a ser la tabla de cuentas,
-- con titular (cliente), estado y fecha de apertura.
ALTER TABLE intereses_calculados RENAME TO cuentas;

ALTER TABLE cuentas ADD COLUMN cliente_id BIGINT;
ALTER TABLE cuentas ADD COLUMN estado VARCHAR(10) NOT NULL DEFAULT 'ACTIVA'
    CHECK (estado IN ('ACTIVA', 'CERRADA'));
ALTER TABLE cuentas ADD COLUMN fecha_apertura TIMESTAMP NOT NULL DEFAULT now();
-- El saldo nunca puede quedar negativo, aunque dos operaciones lleguen al mismo tiempo
ALTER TABLE cuentas ADD CONSTRAINT cuentas_saldo_no_negativo CHECK (saldo >= 0);

CREATE INDEX idx_cuentas_cliente_id ON cuentas (cliente_id);

-- Números para las cuentas nuevas (las heredadas usan 101 a 108)
CREATE SEQUENCE cuentas_numero_seq START WITH 1001;

-- Titulares de las cuentas heredadas: coinciden con los clientes iniciales del microservicio clientes
UPDATE cuentas SET cliente_id = 1 WHERE cuenta_id = 101;
UPDATE cuentas SET cliente_id = 2 WHERE cuenta_id = 102;
UPDATE cuentas SET cliente_id = 3 WHERE cuenta_id = 103;
UPDATE cuentas SET cliente_id = 4 WHERE cuenta_id = 104;
UPDATE cuentas SET cliente_id = 5 WHERE cuenta_id = 107;
UPDATE cuentas SET cliente_id = 6 WHERE cuenta_id = 108;

-- Operaciones: además de los retiros de la saga, registra los depósitos, pagos y
-- transferencias que solicita el microservicio pagos. El id de operación lo genera
-- quien solicita la operación, lo que hace idempotentes los reintentos.
ALTER TABLE operaciones ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'RETIRO'
    CHECK (tipo IN ('RETIRO', 'DEPOSITO', 'PAGO', 'TRANSFERENCIA'));
ALTER TABLE operaciones ADD COLUMN cuenta_destino BIGINT REFERENCES cuentas (cuenta_id);
ALTER TABLE operaciones DROP CONSTRAINT operaciones_estado_check;
ALTER TABLE operaciones ADD CONSTRAINT operaciones_estado_check
    CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'REVERTIDA', 'APLICADA'));
