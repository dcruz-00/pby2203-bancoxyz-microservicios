-- Tablas de negocio usadas por core-api (esquema heredado del Batch de la Exp2)
CREATE TABLE intereses_calculados (
    cuenta_id        BIGINT PRIMARY KEY,
    nombre           VARCHAR(100)  NOT NULL,
    saldo            NUMERIC(12,2) NOT NULL,
    edad             INTEGER,
    tipo             VARCHAR(20)   NOT NULL,
    interes_generado NUMERIC(12,2),
    fecha_calculo    DATE
);

CREATE TABLE transacciones_diarias (
    id    BIGINT PRIMARY KEY,
    fecha DATE          NOT NULL,
    monto NUMERIC(12,2) NOT NULL,
    tipo  VARCHAR(20)   NOT NULL
);
