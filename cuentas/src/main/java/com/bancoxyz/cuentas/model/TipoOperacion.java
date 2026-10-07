package com.bancoxyz.cuentas.model;

/** Operaciones que cuentas aplica a pedido del microservicio pagos. */
public enum TipoOperacion {
    /** Abono a la cuenta. */
    DEPOSITO,
    /** Cargo a la cuenta por el pago de un servicio o comercio. */
    PAGO,
    /** Cargo a la cuenta de origen y abono a la de destino, en una sola transacción. */
    TRANSFERENCIA
}
