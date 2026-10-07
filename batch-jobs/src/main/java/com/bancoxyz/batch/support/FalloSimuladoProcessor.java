package com.bancoxyz.batch.support;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * Solo para la demostración de la reejecución automática (batch.simular-fallo=true).
 *
 * Envuelve al processor real y, en el primer intento del job, lanza un error que
 * no se puede omitir ni reintentar al llegar al registro indicado: el paso y el
 * job terminan en FAILED. El indicador es estático, así que en la reejecución ya
 * no falla y el job se reanuda desde el último bloque confirmado.
 */
public class FalloSimuladoProcessor<I, O> implements ItemProcessor<I, O> {

    private static final AtomicBoolean YA_FALLO = new AtomicBoolean(false);

    private final ItemProcessor<I, O> delegado;
    private final boolean activo;
    private final int fallarEnRegistro;
    private int procesados;

    public FalloSimuladoProcessor(ItemProcessor<I, O> delegado, boolean activo, int fallarEnRegistro) {
        this.delegado = delegado;
        this.activo = activo;
        this.fallarEnRegistro = fallarEnRegistro;
    }

    @Override
    public O process(I item) throws Exception {
        procesados++;
        if (activo && procesados >= fallarEnRegistro && YA_FALLO.compareAndSet(false, true)) {
            throw new IllegalStateException("Fallo crítico simulado en el registro " + procesados
                    + " (por ejemplo, se perdió la conexión con un sistema externo)");
        }
        return delegado.process(item);
    }
}
