package com.bancoxyz.batch.processors;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.jdbc.core.JdbcTemplate;

import com.bancoxyz.batch.exception.DatoInvalidoException;
import com.bancoxyz.batch.model.CuentaInteres;

/**
 * Cálculo de intereses mensuales sobre cuentas de ahorro y préstamos.
 *
 * Tasas mensuales (supuesto del proyecto; el legacy no documenta las suyas):
 * ahorro 0,5 %, préstamo 1,5 %, hipoteca 0,8 % (préstamo hipotecario).
 *
 * Se crea una instancia por ejecución del paso (@StepScope). Al crearse carga las
 * cuentas ya calculadas para el período: si el job se reanuda tras un fallo, una
 * cuenta escrita antes del fallo que reaparece más adelante en el archivo sigue
 * detectándose como duplicada.
 */
public class InteresesMensualesItemProcessor implements ItemProcessor<CuentaInteres, CuentaInteres> {

    private static final Map<String, Double> TASAS = Map.of(
            "ahorro", 0.005,
            "prestamo", 0.015,
            "hipoteca", 0.008);
    private static final int EDAD_MINIMA = 0;
    private static final int EDAD_MAXIMA = 120;

    private final LocalDate fechaProceso;
    private final String periodo;
    private final Set<Long> vistos;

    public InteresesMensualesItemProcessor(JdbcTemplate jdbcTemplate, LocalDate fechaProceso) {
        this.fechaProceso = fechaProceso;
        this.periodo = fechaProceso.toString().substring(0, 7);
        List<Long> yaCalculadas = jdbcTemplate.queryForList(
                "SELECT cuenta_id FROM intereses_calculados WHERE periodo = ?", Long.class, periodo);
        this.vistos = new HashSet<>(yaCalculadas);
    }

    @Override
    public CuentaInteres process(CuentaInteres item) {
        String ref = "Cuenta id=" + item.getCuentaId();
        if (item.getCuentaId() == null) {
            throw new DatoInvalidoException("ID_VACIO", "Cuenta sin id");
        }
        if (item.getNombre() == null) {
            throw new DatoInvalidoException("NOMBRE_VACIO", ref + ": nombre vacío");
        }
        if (item.getSaldo() == null) {
            throw new DatoInvalidoException("SALDO_VACIO", ref + ": saldo vacío");
        }
        if (item.getSaldo() < 0) {
            throw new DatoInvalidoException("SALDO_NEGATIVO", ref + ": saldo negativo (" + item.getSaldo() + ")");
        }
        if (item.getEdad() == null) {
            throw new DatoInvalidoException("EDAD_VACIA", ref + ": edad vacía");
        }
        if (item.getEdad() < EDAD_MINIMA || item.getEdad() > EDAD_MAXIMA) {
            throw new DatoInvalidoException("EDAD_FUERA_DE_RANGO", ref + ": edad fuera de rango (" + item.getEdad() + ")");
        }
        String tipo = item.getTipo() == null ? "" : item.getTipo().toLowerCase();
        Double tasa = TASAS.get(tipo);
        if (tasa == null) {
            throw new DatoInvalidoException("TIPO_INVALIDO", ref + ": tipo no soportado (" + item.getTipo() + ")");
        }
        // Una cuenta se calcula una sola vez por período: la primera aparición es la válida
        if (!vistos.add(item.getCuentaId())) {
            throw new DatoInvalidoException("DUPLICADO", ref + ": cuenta repetida en el período " + periodo);
        }

        item.setTipo(tipo);
        item.setTasaAplicada(tasa);
        item.setInteresGenerado(Math.round(item.getSaldo() * tasa * 100.0) / 100.0);
        item.setFechaCalculo(fechaProceso);
        item.setPeriodo(periodo);
        return item;
    }
}
