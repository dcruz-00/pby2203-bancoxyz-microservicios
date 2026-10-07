-- Tablas de negocio de los tres procesos batch (definición versionada con Flyway).
-- Las claves únicas hacen idempotente la escritura: reejecutar un proceso no duplica filas.

-- 1. Reporte de transacciones diarias -------------------------------------------------
CREATE TABLE transacciones_diarias (
    id    BIGINT        PRIMARY KEY,
    fecha DATE          NOT NULL,
    monto NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    tipo  VARCHAR(20)   NOT NULL CHECK (tipo IN ('debito', 'credito'))
);

-- Resumen por día (lo calcula el segundo paso del job)
CREATE TABLE resumen_transacciones_diarias (
    fecha         DATE          PRIMARY KEY,
    cantidad      INTEGER       NOT NULL,
    total_credito NUMERIC(14,2) NOT NULL,
    total_debito  NUMERIC(14,2) NOT NULL,
    monto_maximo  NUMERIC(12,2) NOT NULL
);

-- 2. Cálculo de intereses mensuales ---------------------------------------------------
-- Una fila por cuenta y período (AAAA-MM): el mismo mes no se calcula dos veces.
CREATE TABLE intereses_calculados (
    cuenta_id        BIGINT        NOT NULL,
    periodo          CHAR(7)       NOT NULL,
    nombre           VARCHAR(100)  NOT NULL,
    saldo            NUMERIC(12,2) NOT NULL CHECK (saldo >= 0),
    edad             INTEGER       NOT NULL,
    tipo             VARCHAR(20)   NOT NULL CHECK (tipo IN ('ahorro', 'prestamo', 'hipoteca')),
    tasa_aplicada    NUMERIC(6,4)  NOT NULL,
    interes_generado NUMERIC(12,2) NOT NULL,
    fecha_calculo    DATE          NOT NULL,
    PRIMARY KEY (cuenta_id, periodo)
);

-- 3. Estados de cuenta anuales --------------------------------------------------------
-- Movimientos válidos del año (el archivo legacy no trae id: la fila completa es la clave)
CREATE TABLE cuentas_anuales (
    id          BIGSERIAL     PRIMARY KEY,
    cuenta_id   BIGINT        NOT NULL,
    fecha       DATE          NOT NULL,
    transaccion VARCHAR(20)   NOT NULL CHECK (transaccion IN ('deposito', 'retiro', 'compra', 'pago')),
    monto       NUMERIC(12,2) NOT NULL CHECK (monto > 0),
    descripcion VARCHAR(200)  NOT NULL,
    UNIQUE (cuenta_id, fecha, transaccion, monto, descripcion)
);

-- Estado de cuenta por cuenta y año (lo calcula el segundo paso del job)
CREATE TABLE estados_cuenta_anuales (
    cuenta_id            BIGINT        NOT NULL,
    anio                 INTEGER       NOT NULL,
    total_depositos      NUMERIC(14,2) NOT NULL,
    total_retiros        NUMERIC(14,2) NOT NULL,
    total_compras        NUMERIC(14,2) NOT NULL,
    total_pagos          NUMERIC(14,2) NOT NULL,
    cantidad_movimientos INTEGER       NOT NULL,
    saldo_neto           NUMERIC(14,2) NOT NULL,
    PRIMARY KEY (cuenta_id, anio)
);

-- Registros rechazados por cualquiera de los procesos, con el motivo. Permite
-- verificar la integridad: leídos = escritos + rechazados.
CREATE TABLE registros_rechazados (
    id        BIGSERIAL    PRIMARY KEY,
    job       VARCHAR(50)  NOT NULL,
    linea     INTEGER,
    contenido VARCHAR(500),
    codigo    VARCHAR(40)  NOT NULL,
    motivo    VARCHAR(300) NOT NULL,
    fecha     TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_registros_rechazados_job ON registros_rechazados (job);
