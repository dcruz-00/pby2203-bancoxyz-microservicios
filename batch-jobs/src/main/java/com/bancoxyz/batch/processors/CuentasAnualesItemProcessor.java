package com.bancoxyz.batch.processors;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.batch.infrastructure.item.ItemProcessor;

import com.bancoxyz.batch.exception.DatoInvalidoException;
import com.bancoxyz.batch.model.CuentaAnual;

/**
 * Validación de los movimientos anuales que alimentan los estados de cuenta.
 *
 * El signo del movimiento lo da su tipo (depósito suma; retiro, compra y pago
 * restan), así que el monto debe ser positivo: los montos negativos o cero son
 * errores del legacy. "depósito" (con tilde) se normaliza a "deposito".
 *
 * Paso multihilo: el conjunto de registros vistos es concurrente.
 */
public class CuentasAnualesItemProcessor implements ItemProcessor<CuentaAnual, CuentaAnual> {

    private static final Map<String, String> TIPOS = Map.of(
            "deposito", "deposito",
            "depósito", "deposito",
            "retiro", "retiro",
            "compra", "compra",
            "pago", "pago");

    private final Set<String> vistos = ConcurrentHashMap.newKeySet();

    /** Se llama al iniciar cada ejecución del job (ver ResumenEjecucionListener). */
    public void reiniciar() {
        vistos.clear();
    }

    @Override
    public CuentaAnual process(CuentaAnual item) {
        String ref = "Movimiento de la cuenta " + item.getCuentaId() + " del " + item.getFecha();
        if (item.getCuentaId() == null) {
            throw new DatoInvalidoException("ID_VACIO", "Movimiento sin cuenta");
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
        String tipo = item.getTransaccion() == null ? null : TIPOS.get(item.getTransaccion().toLowerCase());
        if (tipo == null) {
            throw new DatoInvalidoException("TIPO_INVALIDO", ref + ": tipo de transacción inválido ("
                    + item.getTransaccion() + ")");
        }
        if (item.getDescripcion() == null) {
            throw new DatoInvalidoException("DESCRIPCION_VACIA", ref + ": descripción vacía");
        }
        item.setTransaccion(tipo);

        // Sin id en el archivo: dos filas idénticas son un registro duplicado
        String firma = item.getCuentaId() + "|" + item.getFecha() + "|" + tipo + "|" + item.getMonto() + "|"
                + item.getDescripcion();
        if (!vistos.add(firma)) {
            throw new DatoInvalidoException("DUPLICADO", ref + ": registro duplicado");
        }
        return item;
    }
}
