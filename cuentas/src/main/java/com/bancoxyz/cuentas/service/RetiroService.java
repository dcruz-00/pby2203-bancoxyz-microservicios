package com.bancoxyz.cuentas.service;

import com.bancoxyz.cuentas.exception.PublicacionEventoException;
import com.bancoxyz.cuentas.model.CuentaDTO;
import com.bancoxyz.cuentas.model.RetiroRealizadoEvento;
import com.bancoxyz.cuentas.repository.CuentaRepository;
import com.bancoxyz.cuentas.repository.OperacionRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.UUID;

@Service
public class RetiroService {

    private static final Logger log = LoggerFactory.getLogger(RetiroService.class);

    private final CuentaRepository cuentaRepository;
    private final OperacionRepository operacionRepository;
    private final TransactionTemplate transactionTemplate;
    private final RetiroEventoPublisher publisher;
    private final AlertaSeguridadPublisher alertaPublisher;

    public RetiroService(CuentaRepository cuentaRepository,
                         OperacionRepository operacionRepository,
                         PlatformTransactionManager transactionManager,
                         RetiroEventoPublisher publisher,
                         AlertaSeguridadPublisher alertaPublisher) {
        this.cuentaRepository = cuentaRepository;
        this.operacionRepository = operacionRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.publisher = publisher;
        this.alertaPublisher = alertaPublisher;
    }

    public record Resultado(UUID idOperacion, CuentaDTO cuenta) {
    }

    /**
     * Con el circuito abierto (Kafka caído de forma prolongada), el retiro se
     * rechaza antes de tocar la base de datos: no se descuenta dinero que habría
     * que devolver.
     */
    @CircuitBreaker(name = "retiro")
    public Resultado retirar(Long cuentaId, Double monto) {
        UUID idOperacion = UUID.randomUUID();

        // Débito y operación PENDIENTE en una sola transacción: se guardan ambos o ninguno
        CuentaDTO cuenta = transactionTemplate.execute(status -> {
            // Débito condicionado: falla si la cuenta está cerrada o no tiene saldo suficiente
            CuentaDTO actualizada = cuentaRepository.debitar(cuentaId, monto);
            operacionRepository.crearPendiente(idOperacion, cuentaId, monto);
            return actualizada;
        });

        try {
            publisher.publicar(new RetiroRealizadoEvento(
                    idOperacion.toString(), cuentaId, monto, Instant.now().toString()));
        } catch (PublicacionEventoException ex) {
            // Agotados los reintentos: sin evento, pagos nunca respondería,
            // así que se compensa aquí mismo y el cliente recibe 503
            compensar(idOperacion);
            throw ex;
        }
        alertaPublisher.revisarMonto("RETIRO", cuentaId, cuenta.clienteId(), monto);
        return new Resultado(idOperacion, cuenta);
    }

    /** Misma compensación que aplica la saga ante movimiento-fallido. */
    private void compensar(UUID idOperacion) {
        transactionTemplate.executeWithoutResult(status -> operacionRepository.revertir(idOperacion)
                .ifPresent(op -> cuentaRepository.devolver(op.cuentaId(), op.monto())));
        log.warn("Operación {} REVERTIDA localmente: no se pudo publicar retiro-realizado", idOperacion);
    }
}