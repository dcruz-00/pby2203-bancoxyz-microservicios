package com.bancoxyz.coreapi.service;

import com.bancoxyz.coreapi.model.CuentaInteresDTO;
import com.bancoxyz.coreapi.model.RetiroRealizadoEvento;
import com.bancoxyz.coreapi.repository.CuentaInteresRepository;
import com.bancoxyz.coreapi.repository.OperacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

@Service
public class RetiroService {

    public static final String TOPICO_RETIRO_REALIZADO = "retiro-realizado";

    private static final Logger log = LoggerFactory.getLogger(RetiroService.class);

    private final CuentaInteresRepository cuentaRepository;
    private final OperacionRepository operacionRepository;
    private final TransactionTemplate transactionTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public RetiroService(CuentaInteresRepository cuentaRepository,
                         OperacionRepository operacionRepository,
                         PlatformTransactionManager transactionManager,
                         KafkaTemplate<String, String> kafkaTemplate,
                         JsonMapper jsonMapper) {
        this.cuentaRepository = cuentaRepository;
        this.operacionRepository = operacionRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
    }

    public record Resultado(UUID idOperacion, CuentaInteresDTO cuenta) {
    }

    public Resultado retirar(Long cuentaId, Double monto) {
        UUID idOperacion = UUID.randomUUID();

        // Débito y operación PENDIENTE en una sola transacción: se guardan ambos o ninguno
        CuentaInteresDTO cuenta = transactionTemplate.execute(status -> {
            CuentaInteresDTO actualizada = cuentaRepository.retirar(cuentaId, monto);
            operacionRepository.crearPendiente(idOperacion, cuentaId, monto);
            return actualizada;
        });

        publicar(new RetiroRealizadoEvento(idOperacion.toString(), cuentaId, monto, Instant.now().toString()));
        return new Resultado(idOperacion, cuenta);
    }

    private void publicar(RetiroRealizadoEvento evento) {
        String json = jsonMapper.writeValueAsString(evento);
        kafkaTemplate.send(TOPICO_RETIRO_REALIZADO, String.valueOf(evento.cuentaId()), json)
                .whenComplete((resultado, error) -> {
                    if (error != null) {
                        log.error("No se pudo publicar retiro-realizado de la operación {}", evento.idOperacion(), error);
                    } else {
                        log.info("Publicado retiro-realizado: operación {}, partición {}",
                                evento.idOperacion(), resultado.getRecordMetadata().partition());
                    }
                });
    }
}