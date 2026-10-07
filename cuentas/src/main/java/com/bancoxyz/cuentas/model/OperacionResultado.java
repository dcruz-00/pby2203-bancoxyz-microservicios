package com.bancoxyz.cuentas.model;

import java.util.UUID;

/**
 * Resultado de una operación aplicada. Incluye los titulares para que pagos
 * pueda publicar el evento de la transacción con el cliente correspondiente.
 *
 * @param repetida true si la operación ya se había aplicado antes (reintento)
 */
public record OperacionResultado(
        UUID idOperacion,
        TipoOperacion tipo,
        Long cuentaId,
        Long clienteId,
        Double saldo,
        Long cuentaDestinoId,
        Long clienteDestinoId,
        boolean repetida) {
}
