package com.bancoxyz.movimientos.config;

public class KafkaTopics {
    public static final String RETIRO_REALIZADO = "retiro-realizado";
    public static final String MOVIMIENTO_REGISTRADO = "movimiento-registrado";
    public static final String MOVIMIENTO_FALLIDO = "movimiento-fallido";

    private KafkaTopics() {
    }
}