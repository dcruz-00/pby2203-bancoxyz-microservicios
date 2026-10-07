package com.bancoxyz.batch.writers;

import com.bancoxyz.batch.model.Transaccion;
import org.springframework.batch.infrastructure.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;

import javax.sql.DataSource;

public class TransaccionesItemWriter {

    private TransaccionesItemWriter() {
    }

    /** Escritura idempotente: si el job se reejecuta, una transacción ya guardada no se duplica. */
    public static JdbcBatchItemWriter<Transaccion> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Transaccion>()
                .dataSource(dataSource)
                .sql("INSERT INTO transacciones_diarias (id, fecha, monto, tipo) VALUES (:id, :fecha, :monto, :tipo) "
                        + "ON CONFLICT (id) DO NOTHING")
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .assertUpdates(false)
                .build();
    }
}
