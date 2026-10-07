package com.bancoxyz.clientes.kafka;

import com.bancoxyz.clientes.config.KafkaTopics;
import com.bancoxyz.clientes.model.AlertaSeguridadEvento;
import com.bancoxyz.clientes.model.TransaccionCompletadaEvento;
import com.bancoxyz.clientes.repository.ClienteRepository;
import com.bancoxyz.clientes.repository.NotificacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Consume los eventos de transacciones (de pagos) y de alertas de seguridad (de
 * cuentas) y los convierte en notificaciones para el cliente.
 *
 * Un mensaje con formato inválido se descarta (reintentarlo no lo arregla). Un
 * error de base de datos se deja propagar para que el contenedor de Kafka
 * reintente el mensaje (ver KafkaConfig).
 */
@Component
public class EventosListener {

    private static final Logger log = LoggerFactory.getLogger(EventosListener.class);

    private final ClienteRepository clienteRepository;
    private final NotificacionRepository notificacionRepository;
    private final JsonMapper jsonMapper;

    public EventosListener(ClienteRepository clienteRepository, NotificacionRepository notificacionRepository,
                           JsonMapper jsonMapper) {
        this.clienteRepository = clienteRepository;
        this.notificacionRepository = notificacionRepository;
        this.jsonMapper = jsonMapper;
    }

    @KafkaListener(topics = KafkaTopics.TRANSACCION_COMPLETADA, groupId = "clientes")
    public void alCompletarTransaccion(String mensaje) {
        TransaccionCompletadaEvento evento = leer(mensaje, TransaccionCompletadaEvento.class);
        if (evento == null || evento.tipo() == null) {
            return;
        }
        String monto = formatear(evento.monto());
        String saldo = formatear(evento.saldo());
        switch (evento.tipo()) {
            case "DEPOSITO" -> notificar(evento.clienteId(), "DEPOSITO",
                    "Depósito de " + monto + " en la cuenta " + evento.cuentaId() + ". Saldo: " + saldo,
                    evento.idOperacion());
            case "PAGO" -> notificar(evento.clienteId(), "PAGO",
                    "Pago de " + monto + " con cargo a la cuenta " + evento.cuentaId() + ". Saldo: " + saldo,
                    evento.idOperacion());
            case "TRANSFERENCIA" -> {
                notificar(evento.clienteId(), "TRANSFERENCIA_ENVIADA",
                        "Transferencia de " + monto + " desde la cuenta " + evento.cuentaId() + " a la cuenta "
                                + evento.cuentaDestinoId() + ". Saldo: " + saldo,
                        evento.idOperacion() + ":origen");
                notificar(evento.clienteDestinoId(), "TRANSFERENCIA_RECIBIDA",
                        "Recibiste una transferencia de " + monto + " en la cuenta " + evento.cuentaDestinoId(),
                        evento.idOperacion() + ":destino");
            }
            default -> log.warn("Tipo de transacción desconocido, se ignora: {}", mensaje);
        }
    }

    @KafkaListener(topics = KafkaTopics.ALERTA_SEGURIDAD, groupId = "clientes")
    public void alRecibirAlerta(String mensaje) {
        AlertaSeguridadEvento evento = leer(mensaje, AlertaSeguridadEvento.class);
        if (evento == null) {
            return;
        }
        notificar(evento.clienteId(), "ALERTA_" + evento.tipo(),
                "Alerta de seguridad en la cuenta " + evento.cuentaId() + ": " + evento.detalle(),
                evento.idEvento());
    }

    private void notificar(Long clienteId, String tipo, String mensaje, String idEvento) {
        if (clienteId == null || !clienteRepository.existe(clienteId)) {
            log.warn("Evento {} sin cliente registrado ({}), no se genera notificación", idEvento, clienteId);
            return;
        }
        if (notificacionRepository.guardar(clienteId, tipo, mensaje, idEvento)) {
            log.info("Notificación {} para el cliente {}: {}", tipo, clienteId, mensaje);
        } else {
            log.warn("Evento {} repetido para el cliente {}: ya estaba notificado", idEvento, clienteId);
        }
    }

    private <T> T leer(String mensaje, Class<T> tipo) {
        try {
            return jsonMapper.readValue(mensaje, tipo);
        } catch (JacksonException ex) {
            log.error("Mensaje con formato inválido, se descarta: {}", mensaje, ex);
            return null;
        }
    }

    private static String formatear(Double monto) {
        return monto == null ? "-" : String.format("$%,.2f", monto);
    }
}
