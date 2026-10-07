package com.bancoxyz.batch.jobs;

import javax.sql.DataSource;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.bancoxyz.batch.exception.DatoInvalidoException;
import com.bancoxyz.batch.listeners.RegistroRechazadoListener;
import com.bancoxyz.batch.listeners.ResumenEjecucionListener;
import com.bancoxyz.batch.model.CuentaAnual;
import com.bancoxyz.batch.processors.CuentasAnualesItemProcessor;
import com.bancoxyz.batch.readers.CuentasAnualesItemReader;
import com.bancoxyz.batch.support.ArchivosLegacy;
import com.bancoxyz.batch.writers.CuentasAnualesItemWriter;

/**
 * Proceso 3: generación de estados de cuenta anuales.
 *
 * Paso 1 (multihilo): varios hilos validan y guardan los movimientos del año en
 * paralelo, tomando registros de un lector compartido y sincronizado.
 * Paso 2: compila el estado de cuenta de cada cuenta y año (totales por tipo de
 * movimiento y saldo neto), que se usa en la auditoría anual.
 */
@Configuration
public class CuentasAnualesJobConfig {

    public static final String NOMBRE = "cuentasAnualesJob";

    @Bean
    public CuentasAnualesItemProcessor cuentasAnualesProcessor() {
        return new CuentasAnualesItemProcessor();
    }

    @Bean
    public Job cuentasAnualesJob(JobRepository jobRepository, Step cuentasAnualesStep, Step estadosCuentaStep,
            CuentasAnualesItemProcessor cuentasAnualesProcessor) {
        return new JobBuilder(NOMBRE, jobRepository)
                .listener(new ResumenEjecucionListener(cuentasAnualesProcessor::reiniciar))
                .start(cuentasAnualesStep)
                .next(estadosCuentaStep)
                .build();
    }

    @Bean
    public Step cuentasAnualesStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DataSource dataSource,
            JdbcTemplate jdbcTemplate,
            TaskExecutor batchTaskExecutor,
            ArchivosLegacy archivos,
            CuentasAnualesItemProcessor cuentasAnualesProcessor,
            @Value("${batch.chunk}") int chunk,
            @Value("${batch.limite-rechazos}") int limiteRechazos) {

        ItemStreamReader<CuentaAnual> reader = new SynchronizedItemStreamReaderBuilder<CuentaAnual>()
                .delegate(CuentasAnualesItemReader.reader(archivos.recurso(ArchivosLegacy.ESTADOS_ANUALES)))
                .build();
        JdbcBatchItemWriter<CuentaAnual> writer = CuentasAnualesItemWriter.writer(dataSource);

        return new StepBuilder("cuentasAnualesStep", jobRepository)
                .<CuentaAnual, CuentaAnual>chunk(chunk, transactionManager)
                .reader(reader)
                .processor(cuentasAnualesProcessor)
                .writer(writer)
                .faultTolerant()
                .skip(DatoInvalidoException.class)
                .skip(FlatFileParseException.class)
                .noRollback(DatoInvalidoException.class)
                .skipLimit(limiteRechazos)
                .listener(new RegistroRechazadoListener(jdbcTemplate, NOMBRE))
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .taskExecutor(batchTaskExecutor)
                .build();
    }

    @Bean
    public Step estadosCuentaStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JdbcTemplate jdbcTemplate) {
        return new StepBuilder("estadosCuentaStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    int estados = jdbcTemplate.update("""
                            INSERT INTO estados_cuenta_anuales
                                (cuenta_id, anio, total_depositos, total_retiros, total_compras, total_pagos,
                                 cantidad_movimientos, saldo_neto)
                            SELECT cuenta_id, EXTRACT(YEAR FROM fecha)::int,
                                   COALESCE(SUM(monto) FILTER (WHERE transaccion = 'deposito'), 0),
                                   COALESCE(SUM(monto) FILTER (WHERE transaccion = 'retiro'), 0),
                                   COALESCE(SUM(monto) FILTER (WHERE transaccion = 'compra'), 0),
                                   COALESCE(SUM(monto) FILTER (WHERE transaccion = 'pago'), 0),
                                   COUNT(*),
                                   COALESCE(SUM(monto) FILTER (WHERE transaccion = 'deposito'), 0)
                                     - COALESCE(SUM(monto) FILTER (WHERE transaccion <> 'deposito'), 0)
                            FROM cuentas_anuales
                            GROUP BY cuenta_id, EXTRACT(YEAR FROM fecha)
                            ON CONFLICT (cuenta_id, anio) DO UPDATE SET
                                total_depositos = EXCLUDED.total_depositos,
                                total_retiros = EXCLUDED.total_retiros,
                                total_compras = EXCLUDED.total_compras,
                                total_pagos = EXCLUDED.total_pagos,
                                cantidad_movimientos = EXCLUDED.cantidad_movimientos,
                                saldo_neto = EXCLUDED.saldo_neto
                            """);
                    contribution.incrementWriteCount(estados);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
