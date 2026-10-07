-- Depósitos, pagos de servicios y transferencias procesados por este microservicio.
-- El saldo vive en cuentas; aquí queda el registro de cada solicitud y su resultado.
CREATE TABLE pagos (
    id_operacion        UUID          PRIMARY KEY,
    tipo                VARCHAR(20)   NOT NULL CHECK (tipo IN ('DEPOSITO', 'PAGO', 'TRANSFERENCIA')),
    cuenta_id           BIGINT        NOT NULL,
    cuenta_destino      BIGINT,
    monto               NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    comercio            VARCHAR(100),
    -- PENDIENTE: enviado a cuentas; COMPLETADO: aplicado; RECHAZADO: cuentas lo rechazó
    -- (saldo insuficiente, cuenta cerrada, etc.); FALLIDO: cuentas no respondió
    estado              VARCHAR(12)   NOT NULL
                        CHECK (estado IN ('PENDIENTE', 'COMPLETADO', 'RECHAZADO', 'FALLIDO')),
    saldo_resultante    NUMERIC(12,2),
    motivo              VARCHAR(300),
    fecha_creacion      TIMESTAMP     NOT NULL DEFAULT now(),
    fecha_actualizacion TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_pagos_cuenta_id ON pagos (cuenta_id);
