package com.bancoxyz.pagos.service;

import com.bancoxyz.pagos.event.MovimientoFallidoEvent;
import com.bancoxyz.pagos.event.MovimientoRegistradoEvent;
import com.bancoxyz.pagos.event.RetiroRealizadoEvent;
import com.bancoxyz.pagos.kafka.EventoPublisher;
import com.bancoxyz.pagos.repository.MovimientoRepository;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MovimientoService {

    private static final Logger log = LoggerFactory.getLogger(MovimientoService.class);

    private final MovimientoRepository repository;
    private final EventoPublisher publisher;

    public MovimientoService(MovimientoRepository repository, EventoPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @Retry(name = "guardarMovimiento", fallbackMethod = "registrarFallo")
    public void procesarRetiro(RetiroRealizadoEvent evento) {
        String idOperacion = evento.idOperacion();
        log.info("[idOperacion={}] procesando retiro-realizado para cuenta {}", idOperacion, evento.cuentaId());

        if (repository.existePorIdOperacion(idOperacion)) {
            log.warn("[idOperacion={}] duplicado detectado, se reenvia confirmacion sin reinsertar", idOperacion);
        } else {
            repository.guardar(evento);
            log.info("[idOperacion={}] movimiento guardado correctamente", idOperacion);
        }

        publisher.publicarRegistrado(new MovimientoRegistradoEvent(
                idOperacion, evento.cuentaId(), evento.monto(), evento.fechaHora()));
    }

    // Firma exigida por Resilience4j: mismos parametros del metodo original + Throwable
    public void registrarFallo(RetiroRealizadoEvent evento, Throwable error) {
        log.error("[idOperacion={}] se agotaron los reintentos, se publica movimiento-fallido: {}",
                evento.idOperacion(), error.getMessage());

        publisher.publicarFallido(new MovimientoFallidoEvent(
                evento.idOperacion(),
                evento.cuentaId(),
                evento.monto(),
                evento.fechaHora(),
                "No se pudo registrar el movimiento: " + error.getMessage()
        ));
    }
}