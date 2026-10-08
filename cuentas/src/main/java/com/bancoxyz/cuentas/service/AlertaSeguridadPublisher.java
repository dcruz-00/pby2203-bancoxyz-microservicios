package com.bancoxyz.cuentas.service;

import com.bancoxyz.cuentas.model.AlertaSeguridadEvento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Publica alertas en el tópico alerta-seguridad (las consume clientes).
 *
 * La alerta es informativa: si Kafka no está disponible se registra en el log,
 * pero la operación que la originó no se revierte.
 */
@Component
public class AlertaSeguridadPublisher {

    public static final String TOPICO_ALERTA_SEGURIDAD = "alerta-seguridad";

    private static final Logger log = LoggerFactory.getLogger(AlertaSeguridadPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;
    private final double umbralMonto;

    public AlertaSeguridadPublisher(KafkaTemplate<String, String> kafkaTemplate, JsonMapper jsonMapper,
                                    @Value("${bancoxyz.alertas.umbral-monto:1000}") double umbralMonto) {
        this.kafkaTemplate = kafkaTemplate;
        this.jsonMapper = jsonMapper;
        this.umbralMonto = umbralMonto;
    }

    /** Publica MONTO_ELEVADO si el monto de un cargo alcanza el umbral configurado. */
    public void revisarMonto(String operacion, Long cuentaId, Long clienteId, Double monto) {
        if (monto != null && monto >= umbralMonto) {
            publicar("MONTO_ELEVADO", cuentaId, clienteId, monto,
                    operacion + " por " + formatear(monto) + " (umbral " + formatear(umbralMonto) + ")");
        }
    }

    /** Formato chileno: separador de miles "." y decimal "," (por ejemplo, $1.200,00). */
    private static String formatear(double monto) {
        return String.format(Locale.of("es", "CL"), "$%,.2f", monto);
    }

    public void publicar(String tipo, Long cuentaId, Long clienteId, Double monto, String detalle) {
        AlertaSeguridadEvento evento = new AlertaSeguridadEvento(UUID.randomUUID().toString(), tipo, cuentaId,
                clienteId, monto, detalle, Instant.now().toString());
        try {
            String json = jsonMapper.writeValueAsString(evento);
            kafkaTemplate.send(TOPICO_ALERTA_SEGURIDAD, String.valueOf(cuentaId), json)
                    .whenComplete((resultado, error) -> {
                        if (error != null) {
                            log.error("No se pudo publicar la alerta {} de la cuenta {}: {}", tipo, cuentaId,
                                    error.getMessage());
                        } else {
                            log.warn("Alerta de seguridad publicada: {} en cuenta {} ({})", tipo, cuentaId,
                                    detalle);
                        }
                    });
        } catch (RuntimeException ex) {
            // El productor también puede fallar al enviar, no solo al confirmar
            log.error("No se pudo publicar la alerta {} de la cuenta {}: {}", tipo, cuentaId, ex.getMessage());
        }
    }
}
