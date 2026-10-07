package com.bancoxyz.pagos.config;

public class KafkaTopics {
    public static final String RETIRO_REALIZADO = "retiro-realizado";
    public static final String MOVIMIENTO_REGISTRADO = "movimiento-registrado";
    public static final String MOVIMIENTO_FALLIDO = "movimiento-fallido";
    public static final String TRANSACCION_COMPLETADA = "transaccion-completada";

    private KafkaTopics() {
    }
}