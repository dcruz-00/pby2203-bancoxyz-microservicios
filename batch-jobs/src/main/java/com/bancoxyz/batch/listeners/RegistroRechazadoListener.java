package com.bancoxyz.batch.listeners;

import com.bancoxyz.batch.exception.DatoInvalidoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Registra cada registro omitido (skip) en el log y en la tabla
 * registros_rechazados, con su código y motivo. Así ningún registro se pierde en
 * silencio: leídos = escritos + rechazados.
 *
 * Un solo listener sirve para los tres jobs porque se tipa con Object (el builder
 * acepta SkipListener<? super I, ? super O>).
 */
public class RegistroRechazadoListener implements SkipListener<Object, Object> {

    private static final Logger log = LoggerFactory.getLogger(RegistroRechazadoListener.class);

    private final JdbcTemplate jdbcTemplate;
    private final String job;

    public RegistroRechazadoListener(JdbcTemplate jdbcTemplate, String job) {
        this.jdbcTemplate = jdbcTemplate;
        this.job = job;
    }

    /** Línea que no se pudo convertir (fecha con formato desconocido, número inválido...). */
    @Override
    public void onSkipInRead(Throwable t) {
        if (t instanceof FlatFileParseException error) {
            String causa = error.getCause() != null ? error.getCause().getMessage() : error.getMessage();
            registrar(error.getLineNumber(), error.getInput(), "FORMATO_INVALIDO", causa);
        } else {
            registrar(null, null, "ERROR_LECTURA", t.getMessage());
        }
    }

    /** Registro leído que no cumple las reglas del proceso. */
    @Override
    public void onSkipInProcess(Object item, Throwable t) {
        String codigo = t instanceof DatoInvalidoException dato ? dato.getCodigo() : "ERROR_PROCESO";
        registrar(null, String.valueOf(item), codigo, t.getMessage());
    }

    @Override
    public void onSkipInWrite(Object item, Throwable t) {
        registrar(null, String.valueOf(item), "ERROR_ESCRITURA", t.getMessage());
    }

    private void registrar(Integer linea, String contenido, String codigo, String motivo) {
        log.warn("[{}] Registro rechazado ({}){}: {}", job, codigo, linea == null ? "" : " en línea " + linea, motivo);
        jdbcTemplate.update(
                "INSERT INTO registros_rechazados (job, linea, contenido, codigo, motivo) VALUES (?, ?, ?, ?, ?)",
                job, linea, acotar(contenido, 500), codigo, acotar(motivo == null ? codigo : motivo, 300));
    }

    private static String acotar(String texto, int largo) {
        return texto == null || texto.length() <= largo ? texto : texto.substring(0, largo);
    }
}
