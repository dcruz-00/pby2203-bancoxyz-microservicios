package com.bancoxyz.cuentas.service;

import com.bancoxyz.cuentas.exception.OperacionNoPermitidaException;
import com.bancoxyz.cuentas.exception.SolicitudInvalidaException;
import com.bancoxyz.cuentas.model.CuentaDTO;
import com.bancoxyz.cuentas.model.OperacionRequest;
import com.bancoxyz.cuentas.model.OperacionResultado;
import com.bancoxyz.cuentas.model.TipoOperacion;
import com.bancoxyz.cuentas.repository.CuentaRepository;
import com.bancoxyz.cuentas.repository.OperacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

/**
 * Aplica los depósitos, pagos y transferencias que solicita el microservicio pagos.
 *
 * Cada operación se aplica en una sola transacción local (saldos + registro de la
 * operación) y es idempotente: si pagos reintenta con el mismo id de operación,
 * se devuelve el resultado sin volver a mover dinero.
 */
@Service
public class OperacionService {

    private static final Logger log = LoggerFactory.getLogger(OperacionService.class);

    private final CuentaRepository cuentaRepository;
    private final OperacionRepository operacionRepository;
    private final TransactionTemplate transactionTemplate;
    private final AlertaSeguridadPublisher alertaPublisher;

    public OperacionService(CuentaRepository cuentaRepository, OperacionRepository operacionRepository,
                            PlatformTransactionManager transactionManager,
                            AlertaSeguridadPublisher alertaPublisher) {
        this.cuentaRepository = cuentaRepository;
        this.operacionRepository = operacionRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.alertaPublisher = alertaPublisher;
    }

    public OperacionResultado aplicar(OperacionRequest request) {
        validar(request);

        // Reintento de una operación ya aplicada: se responde sin repetirla
        var existente = operacionRepository.buscar(request.idOperacion());
        if (existente.isPresent()) {
            return repetida(request, existente.get());
        }

        OperacionResultado resultado;
        try {
            resultado = transactionTemplate.execute(status -> ejecutar(request));
        } catch (DuplicateKeyException ex) {
            // Dos solicitudes iguales llegaron al mismo tiempo: la otra ya la aplicó y
            // esta transacción se deshizo completa (incluidos los saldos)
            return repetida(request, operacionRepository.buscar(request.idOperacion()).orElseThrow());
        }

        log.info("Operación {} {} APLICADA: cuenta {}, monto {}", request.idOperacion(), request.tipo(),
                request.cuentaId(), request.monto());
        if (request.tipo() != TipoOperacion.DEPOSITO) {
            alertaPublisher.revisarMonto(request.tipo().name(), request.cuentaId(), resultado.clienteId(),
                    request.monto());
        }
        return resultado;
    }

    private OperacionResultado ejecutar(OperacionRequest request) {
        Long origen = request.cuentaId();
        Double monto = request.monto();

        CuentaDTO cuenta;
        CuentaDTO destino = null;
        switch (request.tipo()) {
            case DEPOSITO -> cuenta = cuentaRepository.acreditar(origen, monto);
            case PAGO -> cuenta = cuentaRepository.debitar(origen, monto);
            case TRANSFERENCIA -> {
                Long idDestino = request.cuentaDestinoId();
                // Se actualizan siempre en el mismo orden (menor número primero) para que dos
                // transferencias cruzadas simultáneas no se bloqueen mutuamente
                if (origen < idDestino) {
                    cuenta = cuentaRepository.debitar(origen, monto);
                    destino = cuentaRepository.acreditar(idDestino, monto);
                } else {
                    destino = cuentaRepository.acreditar(idDestino, monto);
                    cuenta = cuentaRepository.debitar(origen, monto);
                }
            }
            default -> throw new SolicitudInvalidaException("Tipo de operación no soportado: " + request.tipo());
        }

        operacionRepository.crearAplicada(request.idOperacion(), request.tipo().name(), origen,
                request.cuentaDestinoId(), monto);

        return new OperacionResultado(request.idOperacion(), request.tipo(), origen, cuenta.clienteId(),
                cuenta.saldo(), destino == null ? null : destino.cuentaId(),
                destino == null ? null : destino.clienteId(), false);
    }

    private void validar(OperacionRequest request) {
        if (request.tipo() == TipoOperacion.TRANSFERENCIA) {
            if (request.cuentaDestinoId() == null) {
                throw new SolicitudInvalidaException("La transferencia requiere cuentaDestinoId");
            }
            if (request.cuentaDestinoId().equals(request.cuentaId())) {
                throw new SolicitudInvalidaException("La cuenta de destino debe ser distinta de la de origen");
            }
        } else if (request.cuentaDestinoId() != null) {
            throw new SolicitudInvalidaException("cuentaDestinoId solo aplica a transferencias");
        }
    }

    private OperacionResultado repetida(OperacionRequest request, OperacionRepository.Operacion existente) {
        boolean mismaOperacion = existente.tipo().equals(request.tipo().name())
                && existente.cuentaId().equals(request.cuentaId())
                && Objects.equals(existente.cuentaDestinoId(), request.cuentaDestinoId())
                && Math.abs(existente.monto() - request.monto()) < 0.005;
        if (!mismaOperacion) {
            throw new OperacionNoPermitidaException("El id de operación " + request.idOperacion()
                    + " ya se usó para una operación distinta");
        }
        log.warn("Operación {} repetida: ya estaba aplicada, no se vuelve a mover dinero", request.idOperacion());

        CuentaDTO cuenta = cuentaRepository.obtener(existente.cuentaId());
        CuentaDTO destino = existente.cuentaDestinoId() == null
                ? null
                : cuentaRepository.obtener(existente.cuentaDestinoId());
        return new OperacionResultado(request.idOperacion(), request.tipo(), cuenta.cuentaId(), cuenta.clienteId(),
                cuenta.saldo(), destino == null ? null : destino.cuentaId(),
                destino == null ? null : destino.clienteId(), true);
    }
}
