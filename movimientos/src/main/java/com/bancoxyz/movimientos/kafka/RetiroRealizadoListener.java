package com.bancoxyz.movimientos.kafka;

import com.bancoxyz.movimientos.config.KafkaTopics;
import com.bancoxyz.movimientos.event.RetiroRealizadoEvent;
import com.bancoxyz.movimientos.service.MovimientoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RetiroRealizadoListener {

    private static final Logger log = LoggerFactory.getLogger(RetiroRealizadoListener.class);

    private final MovimientoService movimientoService;
    private final ObjectMapper objectMapper;

    public RetiroRealizadoListener(MovimientoService movimientoService, ObjectMapper objectMapper) {
        this.movimientoService = movimientoService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = KafkaTopics.RETIRO_REALIZADO, groupId = "movimientos")
    public void escuchar(String mensaje) {
        try {
            RetiroRealizadoEvent evento = objectMapper.readValue(mensaje, RetiroRealizadoEvent.class);
            movimientoService.procesarRetiro(evento);
        } catch (Exception e) {
            log.error("Mensaje de retiro-realizado invalido, se descarta: {}", mensaje, e);
        }
    }
}