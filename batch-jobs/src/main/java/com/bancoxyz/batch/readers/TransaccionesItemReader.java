package com.bancoxyz.batch.readers;

import com.bancoxyz.batch.model.Transaccion;
import com.bancoxyz.batch.support.LectorCampos;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.core.io.Resource;

/** Lee movimientos_financieros_diarios.csv (id, fecha, monto, tipo). */
public class TransaccionesItemReader {

    private TransaccionesItemReader() {
    }

    /**
     * Lector de una partición: procesa solo los registros [fromItem, toItem). Cada
     * partición guarda su posición, por lo que se puede reanudar si el job falla.
     */
    public static FlatFileItemReader<Transaccion> reader(Resource archivo, int fromItem, int toItem) {
        FlatFileItemReader<Transaccion> reader = new FlatFileItemReaderBuilder<Transaccion>()
                .name("transaccionesItemReader-" + fromItem + "-" + toItem)
                .resource(archivo)
                .linesToSkip(1)
                .delimited()
                .names("id", "fecha", "monto", "tipo")
                .fieldSetMapper(fieldSet -> {
                    Transaccion transaccion = new Transaccion();
                    transaccion.setId(LectorCampos.largo(fieldSet.readString("id")));
                    transaccion.setFecha(LectorCampos.fecha(fieldSet.readString("fecha")));
                    transaccion.setMonto(LectorCampos.decimal(fieldSet.readString("monto")));
                    transaccion.setTipo(LectorCampos.texto(fieldSet.readString("tipo")));
                    return transaccion;
                })
                .build();
        reader.setCurrentItemCount(fromItem);
        reader.setMaxItemCount(toItem);
        return reader;
    }
}
