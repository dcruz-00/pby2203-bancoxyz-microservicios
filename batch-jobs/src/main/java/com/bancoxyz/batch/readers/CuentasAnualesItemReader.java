package com.bancoxyz.batch.readers;

import com.bancoxyz.batch.model.CuentaAnual;
import com.bancoxyz.batch.support.LectorCampos;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.core.io.Resource;

/** Lee estados_financieros_anuales.csv (cuenta_id, fecha, transaccion, monto, descripcion). */
public class CuentasAnualesItemReader {

    private CuentasAnualesItemReader() {
    }

    /**
     * Se usa en un paso multihilo (envuelto en un lector sincronizado). Con varios
     * hilos la posición guardada no sería confiable, por eso saveState=false: si el
     * job falla, se relee el archivo completo y la escritura idempotente evita
     * duplicar filas.
     */
    public static FlatFileItemReader<CuentaAnual> reader(Resource archivo) {
        return new FlatFileItemReaderBuilder<CuentaAnual>()
                .name("cuentasAnualesItemReader")
                .resource(archivo)
                .saveState(false)
                .linesToSkip(1)
                .delimited()
                .names("cuenta_id", "fecha", "transaccion", "monto", "descripcion")
                .fieldSetMapper(fieldSet -> {
                    CuentaAnual cuenta = new CuentaAnual();
                    cuenta.setCuentaId(LectorCampos.largo(fieldSet.readString("cuenta_id")));
                    cuenta.setFecha(LectorCampos.fecha(fieldSet.readString("fecha")));
                    cuenta.setTransaccion(LectorCampos.texto(fieldSet.readString("transaccion")));
                    cuenta.setMonto(LectorCampos.decimal(fieldSet.readString("monto")));
                    cuenta.setDescripcion(LectorCampos.texto(fieldSet.readString("descripcion")));
                    return cuenta;
                })
                .build();
    }
}
