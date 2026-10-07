package com.bancoxyz.batch.readers;

import com.bancoxyz.batch.model.CuentaInteres;
import com.bancoxyz.batch.support.LectorCampos;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.core.io.Resource;

/** Lee intereses_trimestrales.csv (cuenta_id, nombre, saldo, edad, tipo). */
public class InteresesMensualesItemReader {

    private InteresesMensualesItemReader() {
    }

    /**
     * Paso de un solo hilo: el lector guarda su posición en cada commit, así que
     * si el job falla se reanuda desde el último bloque confirmado.
     */
    public static FlatFileItemReader<CuentaInteres> reader(Resource archivo) {
        return new FlatFileItemReaderBuilder<CuentaInteres>()
                .name("interesesMensualesItemReader")
                .resource(archivo)
                .linesToSkip(1)
                .delimited()
                .names("cuenta_id", "nombre", "saldo", "edad", "tipo")
                .fieldSetMapper(fieldSet -> {
                    CuentaInteres cuenta = new CuentaInteres();
                    cuenta.setCuentaId(LectorCampos.largo(fieldSet.readString("cuenta_id")));
                    cuenta.setNombre(LectorCampos.texto(fieldSet.readString("nombre")));
                    cuenta.setSaldo(LectorCampos.decimal(fieldSet.readString("saldo")));
                    cuenta.setEdad(LectorCampos.entero(fieldSet.readString("edad")));
                    cuenta.setTipo(LectorCampos.texto(fieldSet.readString("tipo")));
                    return cuenta;
                })
                .build();
    }
}
