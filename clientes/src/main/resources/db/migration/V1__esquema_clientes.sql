-- Perfiles de cliente
CREATE TABLE clientes (
    id               BIGSERIAL    PRIMARY KEY,
    rut              VARCHAR(12)  NOT NULL UNIQUE,
    nombre           VARCHAR(100) NOT NULL,
    email            VARCHAR(120) NOT NULL,
    telefono         VARCHAR(20),
    fecha_nacimiento DATE,
    estado           VARCHAR(10)  NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    fecha_registro   TIMESTAMP    NOT NULL DEFAULT now()
);

-- Notificaciones generadas a partir de los eventos de Kafka (transacciones y alertas).
-- La clave única (id_evento, cliente_id) descarta los eventos repetidos: Kafka puede
-- entregar un mensaje más de una vez.
CREATE TABLE notificaciones (
    id         BIGSERIAL    PRIMARY KEY,
    cliente_id BIGINT       NOT NULL REFERENCES clientes (id),
    tipo       VARCHAR(30)  NOT NULL,
    mensaje    VARCHAR(300) NOT NULL,
    id_evento  VARCHAR(64)  NOT NULL,
    fecha      TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (id_evento, cliente_id)
);

CREATE INDEX idx_notificaciones_cliente_id ON notificaciones (cliente_id);
