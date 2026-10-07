-- Operaciones de retiro de la saga: cada retiro queda PENDIENTE hasta que movimientos responde
CREATE TABLE operaciones (
    id_operacion   UUID          PRIMARY KEY,
    cuenta_id      BIGINT        NOT NULL REFERENCES intereses_calculados (cuenta_id),
    monto          NUMERIC(12,2) NOT NULL,
    estado         VARCHAR(20)   NOT NULL
                   CHECK (estado IN ('PENDIENTE', 'CONFIRMADA', 'REVERTIDA')),
    fecha_creacion TIMESTAMP     NOT NULL DEFAULT now()
);