package com.bancoxyz.batch.processors;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.batch.infrastructure.item.ItemProcessor;

import com.bancoxyz.batch.exception.DatoInvalidoException;
import com.bancoxyz.batch.model.Transaccion;

/**
 * Reglas del reporte de transacciones diarias. Un registro que no las cumple es
 * una anomalía: se rechaza con su código y queda en registros_rechazados.
 *
 * La misma instancia atiende a todas las particiones (en paralelo), por eso el
 * conjunto de ids vistos es concurrente.
 */
public class TransaccionesItemProcessor implements ItemProcessor<Transaccion, Transaccion> {

    private static final Set<String> TIPOS_VALIDOS = Set.of("debito", "credito");

    private final Set<Long> vistos = ConcurrentHashMap.newKeySet();

    /** Se llama al iniciar cada ejecución del job (ver ResumenEjecucionListener). */
    public void reiniciar() {
        vistos.clear();
    }

    @Override
    public Transaccion process(Transaccion item) {
        String ref = "Transacción id=" + item.getId();
        if (item.getId() == null) {
            throw new DatoInvalidoException("ID_VACIO", "Transacción sin id");
        }
        if (item.getFecha() == null) {
            throw new DatoInvalidoException("FECHA_VACIA", ref + ": fecha vacía");
        }
        if (item.getMonto() == null) {
            throw new DatoInvalidoException("MONTO_VACIO", ref + ": monto vacío");
        }
        if (item.getMonto() <= 0) {
            throw new DatoInvalidoException("MONTO_NO_POSITIVO", ref + ": monto negativo o cero (" + item.getMonto() + ")");
        }
        String tipo = item.getTipo() == null ? null : item.getTipo().toLowerCase();
        if (tipo == null || !TIPOS_VALIDOS.contains(tipo)) {
            throw new DatoInvalidoException("TIPO_INVALIDO", ref + ": tipo inválido (" + item.getTipo() + ")");
        }
        // El id es la clave de la transacción: un id repetido es un registro duplicado
        if (!vistos.add(item.getId())) {
            throw new DatoInvalidoException("DUPLICADO", ref + ": id duplicado");
        }

        item.setTipo(tipo);
        return item;
    }
}
