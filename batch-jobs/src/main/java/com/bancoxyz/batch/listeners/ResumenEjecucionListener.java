package com.bancoxyz.batch.listeners;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;

/**
 * Al iniciar el job, reinicia el estado en memoria de los processors (detección
 * de duplicados). Al terminar, registra en el log los totales de cada paso y
 * verifica la integridad: todo registro leído terminó escrito o rechazado.
 */
public class ResumenEjecucionListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(ResumenEjecucionListener.class);

    private final List<Runnable> alIniciar;

    public ResumenEjecucionListener(Runnable... alIniciar) {
        this.alIniciar = List.of(alIniciar);
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        alIniciar.forEach(Runnable::run);
        log.info("==> Inicia {} (ejecución {}, parámetros {})", nombre(jobExecution), jobExecution.getId(),
                jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("==> Fin de {}: {}", nombre(jobExecution), jobExecution.getStatus());
        for (StepExecution paso : jobExecution.getStepExecutions()) {
            // Las particiones (paso:particionN) ya están sumadas en su paso administrador
            if (paso.getStepName().contains(":")) {
                continue;
            }
            long rechazados = paso.getReadSkipCount() + paso.getProcessSkipCount() + paso.getWriteSkipCount();
            long lineas = paso.getReadCount() + paso.getReadSkipCount();
            Duration duracion = paso.getStartTime() != null && paso.getEndTime() != null
                    ? Duration.between(paso.getStartTime(), paso.getEndTime())
                    : Duration.ZERO;
            log.info("    Paso {} [{}]: líneas={}, escritos={}, rechazados={} (lectura={}, proceso={}, escritura={}), "
                            + "filtrados={}, commits={}, rollbacks={}, duración={} ms",
                    paso.getStepName(), paso.getStatus(), lineas, paso.getWriteCount(), rechazados,
                    paso.getReadSkipCount(), paso.getProcessSkipCount(), paso.getWriteSkipCount(),
                    paso.getFilterCount(), paso.getCommitCount(), paso.getRollbackCount(), duracion.toMillis());
            if (paso.getReadCount() > 0 || paso.getReadSkipCount() > 0) {
                boolean integra = lineas == paso.getWriteCount() + paso.getFilterCount() + rechazados;
                log.info("    Integridad {}: líneas ({}) {} escritos + filtrados + rechazados ({})",
                        integra ? "OK" : "CON DIFERENCIAS", lineas, integra ? "=" : "!=",
                        paso.getWriteCount() + paso.getFilterCount() + rechazados);
            }
        }
        jobExecution.getAllFailureExceptions()
                .forEach(error -> log.error("    Causa del fallo: {}", error.toString()));
    }

    private static String nombre(JobExecution jobExecution) {
        return jobExecution.getJobInstance().getJobName();
    }
}
