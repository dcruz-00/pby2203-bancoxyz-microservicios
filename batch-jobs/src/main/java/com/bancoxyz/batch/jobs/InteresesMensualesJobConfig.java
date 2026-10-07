package com.bancoxyz.batch.jobs;

import java.time.LocalDate;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import com.bancoxyz.batch.exception.DatoInvalidoException;
import com.bancoxyz.batch.listeners.RegistroRechazadoListener;
import com.bancoxyz.batch.listeners.ResumenEjecucionListener;
import com.bancoxyz.batch.model.CuentaInteres;
import com.bancoxyz.batch.processors.InteresesMensualesItemProcessor;
import com.bancoxyz.batch.readers.InteresesMensualesItemReader;
import com.bancoxyz.batch.support.ArchivosLegacy;
import com.bancoxyz.batch.support.FalloSimuladoProcessor;
import com.bancoxyz.batch.writers.InteresesMensualesItemWriter;

/**
 * Proceso 2: cálculo de intereses mensuales (período = mes de la fecha de proceso).
 *
 * Paso de un solo hilo y reanudable: el lector guarda su posición en cada commit,
 * así que si el job falla, la reejecución continúa desde el último bloque
 * confirmado. Además, "la primera aparición de una cuenta es la válida" solo es
 * determinista si el archivo se lee en orden.
 */
@Configuration
public class InteresesMensualesJobConfig {

    public static final String NOMBRE = "interesesMensualesJob";

    @Bean
    public Job interesesMensualesJob(JobRepository jobRepository, Step interesesMensualesStep) {
        return new JobBuilder(NOMBRE, jobRepository)
                .listener(new ResumenEjecucionListener())
                .start(interesesMensualesStep)
                .build();
    }

    @Bean
    public Step interesesMensualesStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DataSource dataSource,
            JdbcTemplate jdbcTemplate,
            ArchivosLegacy archivos,
            ItemProcessor<CuentaInteres, CuentaInteres> interesesProcessor,
            @Value("${batch.chunk}") int chunk,
            @Value("${batch.limite-rechazos}") int limiteRechazos) {

        JdbcBatchItemWriter<CuentaInteres> writer = InteresesMensualesItemWriter.writer(dataSource);

        return new StepBuilder("interesesMensualesStep", jobRepository)
                .<CuentaInteres, CuentaInteres>chunk(chunk, transactionManager)
                .reader(InteresesMensualesItemReader.reader(archivos.recurso(ArchivosLegacy.INTERESES)))
                .processor(interesesProcessor)
                .writer(writer)
                .faultTolerant()
                .skip(DatoInvalidoException.class)
                .skip(FlatFileParseException.class)
                .noRollback(DatoInvalidoException.class)
                .skipLimit(limiteRechazos)
                .listener(new RegistroRechazadoListener(jdbcTemplate, NOMBRE))
                .retry(TransientDataAccessException.class)
                .retryLimit(3)
                .build();
    }

    /** Una instancia por ejecución del paso, con la fecha de proceso del job. */
    @Bean
    @StepScope
    public ItemProcessor<CuentaInteres, CuentaInteres> interesesProcessor(JdbcTemplate jdbcTemplate,
            @Value("#{jobParameters['fechaProceso']}") LocalDate fechaProceso,
            @Value("${batch.simular-fallo}") boolean simularFallo,
            @Value("${batch.chunk}") int chunk) {
        InteresesMensualesItemProcessor processor = new InteresesMensualesItemProcessor(jdbcTemplate, fechaProceso);
        // Con la simulación activa, falla a mitad del cuarto bloque (con chunk 100, en el registro 350)
        return new FalloSimuladoProcessor<>(processor, simularFallo, chunk * 3 + chunk / 2);
    }
}
