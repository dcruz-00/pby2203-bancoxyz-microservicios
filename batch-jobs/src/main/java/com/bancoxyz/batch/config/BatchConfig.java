package com.bancoxyz.batch.config;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
public class BatchConfig {

    /** Hilos para los pasos multihilo y para ejecutar las particiones en paralelo. */
    @Bean
    public TaskExecutor batchTaskExecutor(@Value("${batch.hilos}") int hilos) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(hilos);
        executor.setMaxPoolSize(hilos);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("batch-hilo-");
        executor.initialize();
        return executor;
    }
}
