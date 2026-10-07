package com.bancoxyz.batch.writers;

import com.bancoxyz.batch.model.CuentaAnual;
import org.springframework.batch.infrastructure.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;

import javax.sql.DataSource;

public class CuentasAnualesItemWriter {

    private CuentasAnualesItemWriter() {
    }

    /** Escritura idempotente: si el job se reejecuta, un movimiento ya guardado no se duplica. */
    public static JdbcBatchItemWriter<CuentaAnual> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<CuentaAnual>()
                .dataSource(dataSource)
                .sql("INSERT INTO cuentas_anuales (cuenta_id, fecha, transaccion, monto, descripcion) "
                        + "VALUES (:cuentaId, :fecha, :transaccion, :monto, :descripcion) "
                        + "ON CONFLICT (cuenta_id, fecha, transaccion, monto, descripcion) DO NOTHING")
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .assertUpdates(false)
                .build();
    }
}
