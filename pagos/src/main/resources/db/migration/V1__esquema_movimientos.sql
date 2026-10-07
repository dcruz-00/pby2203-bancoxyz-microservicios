CREATE TABLE movimientos (
    id              BIGSERIAL PRIMARY KEY,
    operacion_id    VARCHAR(64) NOT NULL UNIQUE,
    cuenta_id       BIGINT NOT NULL,
    monto           NUMERIC(12,2) NOT NULL,
    fecha_operacion TIMESTAMP NOT NULL,
    fecha_registro  TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_movimientos_cuenta_id ON movimientos(cuenta_id);