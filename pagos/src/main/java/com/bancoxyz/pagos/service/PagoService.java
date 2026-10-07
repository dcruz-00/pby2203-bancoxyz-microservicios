package com.bancoxyz.pagos.service;

import com.bancoxyz.pagos.event.TransaccionCompletadaEvent;
import com.bancoxyz.pagos.exception.CuentasNoDisponibleException;
import com.bancoxyz.pagos.exception.PagoNoEncontradoException;
import com.bancoxyz.pagos.exception.RechazoCuentasException;
import com.bancoxyz.pagos.kafka.EventoPublisher;
import com.bancoxyz.pagos.model.OperacionCuentasRequest;
import com.bancoxyz.pagos.model.OperacionCuentasResultado;
import com.bancoxyz.pagos.model.PagoDTO;
import com.bancoxyz.pagos.model.TipoPago;
import com.bancoxyz.pagos.repository.PagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Procesa depósitos, pagos de servicios y transferencias.
 *
 * 1. Registra la solicitud como PENDIENTE (queda constancia aunque algo falle después).
 * 2. Pide a cuentas que la aplique (síncrono: el cliente necesita saber si se hizo).
 * 3. Según la respuesta, la marca COMPLETADO, RECHAZADO o FALLIDO.
 * 4. Si se completó, publica transaccion-completada (clientes notifica al titular).
 */
@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository pagoRepository;
    private final CuentasClient cuentasClient;
    private final EventoPublisher eventoPublisher;

    public PagoService(PagoRepository pagoRepository, CuentasClient cuentasClient, EventoPublisher eventoPublisher) {
        this.pagoRepository = pagoRepository;
        this.cuentasClient = cuentasClient;
        this.eventoPublisher = eventoPublisher;
    }

    public PagoDTO procesar(TipoPago tipo, Long cuentaId, Long cuentaDestinoId, Double monto, String comercio) {
        UUID idOperacion = UUID.randomUUID();
        pagoRepository.crearPendiente(idOperacion, tipo, cuentaId, cuentaDestinoId, monto, comercio);
        log.info("[idOperacion={}] {} PENDIENTE: cuenta {}, monto {}", idOperacion, tipo, cuentaId, monto);

        OperacionCuentasResultado resultado;
        try {
            resultado = cuentasClient.aplicar(
                    new OperacionCuentasRequest(idOperacion, tipo, cuentaId, cuentaDestinoId, monto));
        } catch (RechazoCuentasException ex) {
            pagoRepository.finalizarSinAplicar(idOperacion, "RECHAZADO", ex.getMessage());
            log.info("[idOperacion={}] {} RECHAZADO por cuentas: {}", idOperacion, tipo, ex.getMessage());
            throw ex;
        } catch (CuentasNoDisponibleException ex) {
            pagoRepository.finalizarSinAplicar(idOperacion, "FALLIDO", ex.getMessage());
            log.warn("[idOperacion={}] {} FALLIDO: {}", idOperacion, tipo, ex.getMessage());
            throw ex;
        }

        pagoRepository.completar(idOperacion, resultado.saldo());
        log.info("[idOperacion={}] {} COMPLETADO: saldo resultante {}", idOperacion, tipo, resultado.saldo());

        eventoPublisher.publicarTransaccion(new TransaccionCompletadaEvent(
                idOperacion.toString(), tipo.name(), cuentaId, resultado.clienteId(), monto, resultado.saldo(),
                resultado.cuentaDestinoId(), resultado.clienteDestinoId(), Instant.now().toString()));

        return obtener(idOperacion);
    }

    public PagoDTO obtener(UUID idOperacion) {
        return pagoRepository.buscar(idOperacion)
                .orElseThrow(() -> new PagoNoEncontradoException("No existe el pago " + idOperacion));
    }
}
