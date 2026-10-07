package com.bancoxyz.batch.runner;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

/**
 * Ejecuta los tres procesos batch en orden, con políticas de finalización y de
 * reejecución automática.
 *
 * - Parámetro identificador: la fecha de proceso. Un job ya COMPLETADO para esa
 *   fecha no se vuelve a ejecutar (evita procesar dos veces el mismo período).
 * - Fallo técnico (base de datos caída, error inesperado): se reejecuta hasta
 *   batch.reintentos-job veces, con espera creciente. Spring Batch reanuda la
 *   misma instancia del job: se saltan los pasos y particiones ya completados y
 *   los pasos reanudables continúan desde su último commit.
 * - Fallo por calidad de datos (se superó el límite de rechazos): no se
 *   reintenta, porque el resultado sería el mismo; requiere revisar el archivo.
 * - Si algún job termina en FAILED, la aplicación sale con código 1, y Docker
 *   puede reiniciar el contenedor (restart: on-failure).
 */
@Component
public class EjecutorDeJobs implements ApplicationRunner, ExitCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(EjecutorDeJobs.class);

    private final JobOperator jobOperator;
    private final List<Job> jobs;
    private final String fechaConfigurada;
    private final int reintentos;
    private final Duration espera;
    private int codigoSalida = 0;

    public EjecutorDeJobs(JobOperator jobOperator,
                          @Qualifier("transaccionesJob") Job transaccionesJob,
                          @Qualifier("interesesMensualesJob") Job interesesMensualesJob,
                          @Qualifier("cuentasAnualesJob") Job cuentasAnualesJob,
                          @Value("${batch.fecha-proceso:}") String fechaConfigurada,
                          @Value("${batch.reintentos-job}") int reintentos,
                          @Value("${batch.espera-entre-reintentos}") Duration espera) {
        this.jobOperator = jobOperator;
        this.jobs = List.of(transaccionesJob, interesesMensualesJob, cuentasAnualesJob);
        this.fechaConfigurada = fechaConfigurada;
        this.reintentos = reintentos;
        this.espera = espera;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        LocalDate fechaProceso = fechaConfigurada == null || fechaConfigurada.isBlank()
                ? LocalDate.now()
                : LocalDate.parse(fechaConfigurada);
        JobParameters parametros = new JobParametersBuilder()
                .addLocalDate("fechaProceso", fechaProceso)
                .toJobParameters();
        log.info("Procesos batch con fecha de proceso {}", fechaProceso);

        for (Job job : jobs) {
            if (!ejecutarConReintentos(job, parametros)) {
                codigoSalida = 1;
            }
        }
        log.info("Procesos batch finalizados: {}", codigoSalida == 0 ? "todos completados" : "hubo fallos");
    }

    /** Devuelve true si el job terminó COMPLETED (o ya lo estaba para esta fecha). */
    private boolean ejecutarConReintentos(Job job, JobParameters parametros) throws InterruptedException {
        for (int intento = 1; intento <= reintentos; intento++) {
            JobExecution ejecucion;
            try {
                // Con los mismos parámetros, si la ejecución anterior falló, Spring Batch
                // crea una nueva ejecución de la misma instancia (reanudación)
                ejecucion = jobOperator.start(job, parametros);
            } catch (JobInstanceAlreadyCompleteException ex) {
                log.info("{} ya estaba COMPLETADO para estos parámetros: no se vuelve a procesar", job.getName());
                return true;
            } catch (Exception ex) {
                log.error("{}: no se pudo lanzar (intento {}/{}): {}", job.getName(), intento, reintentos,
                        ex.getMessage());
                esperar(intento);
                continue;
            }

            if (ejecucion.getStatus() == BatchStatus.COMPLETED) {
                if (intento > 1) {
                    log.info("{} COMPLETADO en el intento {} tras reanudar la ejecución fallida", job.getName(),
                            intento);
                }
                return true;
            }
            if (fallaPorCalidadDeDatos(ejecucion)) {
                log.error("{} FALLÓ por calidad de datos (se superó el límite de rechazos): no se reintenta",
                        job.getName());
                return false;
            }
            log.warn("{} terminó {} (intento {}/{})", job.getName(), ejecucion.getStatus(), intento, reintentos);
            if (intento < reintentos) {
                esperar(intento);
            }
        }
        log.error("{} no se completó tras {} intentos", job.getName(), reintentos);
        return false;
    }

    private void esperar(int intento) throws InterruptedException {
        Duration pausa = espera.multipliedBy(intento);
        log.info("Reejecución automática en {} s", pausa.toSeconds());
        Thread.sleep(pausa.toMillis());
    }

    private static boolean fallaPorCalidadDeDatos(JobExecution ejecucion) {
        return ejecucion.getAllFailureExceptions().stream().anyMatch(EjecutorDeJobs::esLimiteDeRechazos);
    }

    private static boolean esLimiteDeRechazos(Throwable error) {
        for (Throwable t = error; t != null; t = t.getCause()) {
            if (t instanceof SkipLimitExceededException) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getExitCode() {
        return codigoSalida;
    }
}
