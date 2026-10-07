package com.bancoxyz.pagos.kafka;

import com.bancoxyz.pagos.config.KafkaTopics;
import com.bancoxyz.pagos.event.RetiroRealizadoEvent;
import com.bancoxyz.pagos.service.MovimientoService;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RetiroRealizadoListener {

    private static final Logger log = LoggerFactory.getLogger(RetiroRealizadoListener.class);

    private final MovimientoService movimientoService;
    private final JsonMapper objectMapper;

    public RetiroRealizadoListener(MovimientoService movimientoService, JsonMapper objectMapper) {
        this.movimientoService = movimientoService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.RETIRO_REALIZADO, groupId = "pagos")
    public void escuchar(String mensaje) {
        try {
            RetiroRealizadoEvent evento = objectMapper.readValue(mensaje, RetiroRealizadoEvent.class);
            movimientoService.procesarRetiro(evento);
        } catch (Exception e) {
            log.error("Mensaje de retiro-realizado invalido, se descarta: {}", mensaje, e);
        }
    }
}