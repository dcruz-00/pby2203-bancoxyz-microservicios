package com.bancoxyz.batch.jobs;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
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
import com.bancoxyz.batch.model.Transaccion;
import com.bancoxyz.batch.partition.TransaccionesPartitioner;
import com.bancoxyz.batch.processors.TransaccionesItemProcessor;
import com.bancoxyz.batch.readers.TransaccionesItemReader;
import com.bancoxyz.batch.support.ArchivosLegacy;
import com.bancoxyz.batch.writers.TransaccionesItemWriter;

/**
 * Proceso 1: reporte de transacciones diarias.
 *
 * Paso 1 (particionado): el archivo se divide en N rangos de registros que se
 * procesan en paralelo; cada partición valida, rechaza las anomalías y guarda las
 * transacciones válidas. Si el job falla, solo se reprocesan las particiones que
 * no terminaron, desde su último commit.
 * Paso 2: resumen por día (cantidad, totales de crédito y débito, monto máximo).
 */
@Configuration
public class TransaccionesJobConfig {

        public static final String NOMBRE = "transaccionesJob";

        @Bean
        public TransaccionesItemProcessor transaccionesProcessor() {
                return new TransaccionesItemProcessor();
        }

        @Bean
        public Job transaccionesJob(JobRepository jobRepository, Step transaccionesPartitionStep,
                        Step resumenTransaccionesStep, TransaccionesItemProcessor transaccionesProcessor) {
                return new JobBuilder(NOMBRE, jobRepository)
                                .listener(new ResumenEjecucionListener(transaccionesProcessor::reiniciar))
                                .start(transaccionesPartitionStep)
                                .next(resumenTransaccionesStep)
                                .build();
        }

        @Bean
        public Step transaccionesPartitionStep(JobRepository jobRepository,
                        Step transaccionesMinionStep,
                        TaskExecutor batchTaskExecutor,
                        ArchivosLegacy archivos,
                        @Value("${batch.particiones}") int particiones) {

                Partitioner partitioner = new TransaccionesPartitioner(
                                archivos.recurso(ArchivosLegacy.MOVIMIENTOS_DIARIOS));

                return new StepBuilder("transaccionesPartitionStep", jobRepository)
                                .partitioner("transaccionesMinionStep", partitioner)
                                .step(transaccionesMinionStep)
                                .taskExecutor(batchTaskExecutor)
                                .gridSize(particiones)
                                .build();
        }

        @Bean
        public Step transaccionesMinionStep(JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        DataSource dataSource,
                        JdbcTemplate jdbcTemplate,
                        ItemStreamReader<Transaccion> transaccionesPartitionReader,
                        TransaccionesItemProcessor transaccionesProcessor,
                        @Value("${batch.chunk}") int chunk,
                        @Value("${batch.limite-rechazos}") int limiteRechazos) {

                JdbcBatchItemWriter<Transaccion> writer = TransaccionesItemWriter.writer(dataSource);

                return new StepBuilder("transaccionesMinionStep", jobRepository)
                                .<Transaccion, Transaccion>chunk(chunk, transactionManager)
                                .reader(transaccionesPartitionReader)
                                .processor(transaccionesProcessor)
                                .writer(writer)
                                .faultTolerant()
                                // Datos inválidos: se omiten y se registran (no detienen el job)
                                .skip(DatoInvalidoException.class)
                                .skip(FlatFileParseException.class)
                                // Evita que un rechazo deshaga el chunk y vuelva a procesar sus registros
                                .noRollback(DatoInvalidoException.class)
                                .skipLimit(limiteRechazos)
                                .listener(new RegistroRechazadoListener(jdbcTemplate, NOMBRE))
                                // Fallas temporales de la base de datos: se reintenta la escritura
                                .retry(TransientDataAccessException.class)
                                .retryLimit(3)
                                .build();
        }

        @Bean
        @StepScope
        public ItemStreamReader<Transaccion> transaccionesPartitionReader(
                        ArchivosLegacy archivos,
                        @Value("#{stepExecutionContext['fromItem']}") int fromItem,
                        @Value("#{stepExecutionContext['toItem']}") int toItem) {

                return TransaccionesItemReader.reader(archivos.recurso(ArchivosLegacy.MOVIMIENTOS_DIARIOS),
                                fromItem, toItem);
        }

        @Bean
        public Step resumenTransaccionesStep(JobRepository jobRepository,
                        PlatformTransactionManager transactionManager,
                        JdbcTemplate jdbcTemplate) {
                return new StepBuilder("resumenTransaccionesStep", jobRepository)
                                .tasklet((contribution, chunkContext) -> {
                                        int dias = jdbcTemplate.update("""
                                                        INSERT INTO resumen_transacciones_diarias
                                                            (fecha, cantidad, total_credito, total_debito, monto_maximo)
                                                        SELECT fecha, COUNT(*),
                                                               COALESCE(SUM(monto) FILTER (WHERE tipo = 'credito'), 0),
                                                               COALESCE(SUM(monto) FILTER (WHERE tipo = 'debito'), 0),
                                                               MAX(monto)
                                                        FROM transacciones_diarias
                                                        GROUP BY fecha
                                                        ON CONFLICT (fecha) DO UPDATE SET
                                                            cantidad = EXCLUDED.cantidad,
                                                            total_credito = EXCLUDED.total_credito,
                                                            total_debito = EXCLUDED.total_debito,
                                                            monto_maximo = EXCLUDED.monto_maximo
                                                        """);
                                        contribution.incrementWriteCount(dias);
                                        return RepeatStatus.FINISHED;
                                }, transactionManager)
                                .build();
        }
}
