package com.bancoxyz.movimientos.kafka;

import com.bancoxyz.movimientos.config.KafkaTopics;
import com.bancoxyz.movimientos.event.MovimientoFallidoEvent;
import com.bancoxyz.movimientos.event.MovimientoRegistradoEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class EventoPublisher {

    private static final Logger log = LoggerFactory.getLogger(EventoPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public EventoPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publicarRegistrado(MovimientoRegistradoEvent evento) {
        enviar(KafkaTopics.MOVIMIENTO_REGISTRADO, evento.cuentaId().toString(), evento);
    }

    public void publicarFallido(MovimientoFallidoEvent evento) {
        enviar(KafkaTopics.MOVIMIENTO_FALLIDO, evento.cuentaId().toString(), evento);
    }

    private void enviar(String topic, String key, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, json);
            log.info("Evento publicado en {} (key={}): {}", topic, key, json);
        } catch (Exception e) {
            log.error("No se pudo publicar el evento en {}", topic, e);
        }
    }
}