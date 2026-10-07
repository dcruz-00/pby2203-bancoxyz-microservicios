package com.bancoxyz.batch.writers;

import javax.sql.DataSource;

import org.springframework.batch.infrastructure.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;

import com.bancoxyz.batch.model.CuentaInteres;

public class InteresesMensualesItemWriter {

    private InteresesMensualesItemWriter() {
    }

    /** Una fila por cuenta y período; reejecutar el mismo período no duplica el cálculo. */
    public static JdbcBatchItemWriter<CuentaInteres> writer(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<CuentaInteres>()
                .dataSource(dataSource)
                .sql("INSERT INTO intereses_calculados "
                        + "(cuenta_id, periodo, nombre, saldo, edad, tipo, tasa_aplicada, interes_generado, fecha_calculo) "
                        + "VALUES (:cuentaId, :periodo, :nombre, :saldo, :edad, :tipo, :tasaAplicada, :interesGenerado, "
                        + ":fechaCalculo) ON CONFLICT (cuenta_id, periodo) DO NOTHING")
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .assertUpdates(false)
                .build();
    }
}
