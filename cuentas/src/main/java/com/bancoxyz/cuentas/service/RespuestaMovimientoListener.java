package com.bancoxyz.cuentas.service;

import com.bancoxyz.cuentas.model.RespuestaMovimientoEvento;
import com.bancoxyz.cuentas.repository.CuentaRepository;
import com.bancoxyz.cuentas.repository.OperacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Service
public class RespuestaMovimientoListener {

    private static final Logger log = LoggerFactory.getLogger(RespuestaMovimientoListener.class);

    private final OperacionRepository operacionRepository;
    private final CuentaRepository cuentaRepository;
    private final TransactionTemplate transactionTemplate;
    private final JsonMapper jsonMapper;

    public RespuestaMovimientoListener(OperacionRepository operacionRepository,
            CuentaRepository cuentaRepository,
            PlatformTransactionManager transactionManager,
            JsonMapper jsonMapper) {
        this.operacionRepository = operacionRepository;
        this.cuentaRepository = cuentaRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = "movimiento-registrado")
    public void alRegistrarMovimiento(String json) {
        RespuestaMovimientoEvento evento = leer(json);
        if (evento == null) {
            return;
        }
        if (operacionRepository.confirmar(UUID.fromString(evento.idOperacion()))) {
            log.info("Operación {} CONFIRMADA", evento.idOperacion());
        } else {
            log.warn("movimiento-registrado ignorado: la operación {} no existe o ya no está PENDIENTE",
                    evento.idOperacion());
        }
    }

    @KafkaListener(topics = "movimiento-fallido")
    public void alFallarMovimiento(String json) {
        RespuestaMovimientoEvento evento = leer(json);
        if (evento == null) {
            return;
        }
        UUID idOperacion = UUID.fromString(evento.idOperacion());

        // Cambio de estado y devolución del monto en una sola transacción
        Boolean compensada = transactionTemplate.execute(status -> operacionRepository.revertir(idOperacion)
                .map(op -> {
                    cuentaRepository.devolver(op.cuentaId(), op.monto());
                    return true;
                })
                .orElse(false));

        if (Boolean.TRUE.equals(compensada)) {
            log.info("Operación {} REVERTIDA: monto devuelto. Motivo: {}", evento.idOperacion(), evento.motivo());
        } else {
            log.warn("movimiento-fallido ignorado: la operación {} no existe o ya no está PENDIENTE",
                    evento.idOperacion());
        }
    }

    /**
     * Convierte el JSON; si es inválido, lo registra y devuelve null para
     * descartarlo sin reintentar.
     */
    private RespuestaMovimientoEvento leer(String json) {
        try {
            RespuestaMovimientoEvento evento = jsonMapper.readValue(json, RespuestaMovimientoEvento.class);
            UUID.fromString(evento.idOperacion());
            return evento;
        } catch (RuntimeException ex) {
            log.error("Mensaje descartado por formato inválido: {}", json, ex);
            return null;
        }
    }
}