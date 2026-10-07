package com.bancoxyz.cuentas.service;

import com.bancoxyz.cuentas.exception.PublicacionEventoException;
import com.bancoxyz.cuentas.model.RetiroRealizadoEvento;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publica retiro-realizado esperando la confirmación de Kafka.
 *
 * Está en una clase aparte de RetiroService porque las anotaciones de
 * Resilience4j actúan mediante un proxy, que no intercepta llamadas entre
 * métodos de la misma clase.
 */
@Component
public class RetiroEventoPublisher {

    public static final String TOPICO_RETIRO_REALIZADO = "retiro-realizado";

    // Respaldo: los tiempos del productor (config-repo) deberían cortar antes
    private static final long TIEMPO_MAXIMO_SEGUNDOS = 5;

    private static final Logger log = LoggerFactory.getLogger(RetiroEventoPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public RetiroEventoPublisher(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Retry(name = "publicarRetiro")
    public void publicar(RetiroRealizadoEvento evento) {
        // Fuera del try: un error de serialización no es transitorio y no debe reintentarse
        String json = jsonMapper.writeValueAsString(evento);
        try {
            SendResult<String, String> resultado = kafkaTemplate
                    .send(TOPICO_RETIRO_REALIZADO, String.valueOf(evento.cuentaId()), json)
                    .get(TIEMPO_MAXIMO_SEGUNDOS, TimeUnit.SECONDS);
            log.info("Publicado retiro-realizado: operación {}, partición {}",
                    evento.idOperacion(), resultado.getRecordMetadata().partition());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new PublicacionEventoException("Publicación interrumpida", ex);
        } catch (ExecutionException | TimeoutException | RuntimeException ex) {
            // RuntimeException: el productor también puede fallar al enviar, no solo al confirmar
            log.warn("Intento fallido de publicar retiro-realizado de la operación {}: {}",
                    evento.idOperacion(), ex.getMessage());
            throw new PublicacionEventoException("No se pudo publicar retiro-realizado", ex);
        }
    }
}