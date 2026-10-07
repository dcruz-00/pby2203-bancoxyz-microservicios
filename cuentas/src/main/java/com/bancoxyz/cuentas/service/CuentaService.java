package com.bancoxyz.cuentas.service;

import com.bancoxyz.cuentas.exception.OperacionNoPermitidaException;
import com.bancoxyz.cuentas.model.AperturaCuentaRequest;
import com.bancoxyz.cuentas.model.ClienteResumen;
import com.bancoxyz.cuentas.model.CuentaDTO;
import com.bancoxyz.cuentas.repository.CuentaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;

/** Apertura y cierre de cuentas. */
@Service
public class CuentaService {

    private static final Logger log = LoggerFactory.getLogger(CuentaService.class);

    private final CuentaRepository cuentaRepository;
    private final ClientesClient clientesClient;
    private final AlertaSeguridadPublisher alertaPublisher;

    public CuentaService(CuentaRepository cuentaRepository, ClientesClient clientesClient,
                         AlertaSeguridadPublisher alertaPublisher) {
        this.cuentaRepository = cuentaRepository;
        this.clientesClient = clientesClient;
        this.alertaPublisher = alertaPublisher;
    }

    /**
     * Abre una cuenta para un cliente existente. El titular se valida en el
     * microservicio clientes; si no responde, la apertura se rechaza (503).
     */
    public CuentaDTO abrir(AperturaCuentaRequest request) {
        ClienteResumen cliente = clientesClient.obtener(request.clienteId());
        if (!"ACTIVO".equals(cliente.estado())) {
            throw new OperacionNoPermitidaException("El cliente " + cliente.id() + " no está activo");
        }
        Integer edad = cliente.fechaNacimiento() == null
                ? null
                : Period.between(cliente.fechaNacimiento(), LocalDate.now()).getYears();

        Long cuentaId = cuentaRepository.crear(cliente.id(), cliente.nombre(), edad, request.tipo(),
                request.saldoInicial());
        log.info("Cuenta {} abierta para el cliente {} ({})", cuentaId, cliente.id(), request.tipo());
        return cuentaRepository.obtener(cuentaId);
    }

    /** Cierra la cuenta. Exige saldo cero para no dejar dinero sin titular operativo. */
    public CuentaDTO cerrar(Long cuentaId) {
        CuentaDTO cuenta = cuentaRepository.obtener(cuentaId);
        if (!"ACTIVA".equals(cuenta.estado())) {
            throw new OperacionNoPermitidaException("La cuenta " + cuentaId + " ya está cerrada");
        }
        if (!cuentaRepository.cerrar(cuentaId)) {
            throw new OperacionNoPermitidaException("La cuenta " + cuentaId
                    + " debe tener saldo cero para cerrarse (saldo actual: " + cuenta.saldo() + ")");
        }
        log.info("Cuenta {} cerrada", cuentaId);
        alertaPublisher.publicar("CUENTA_CERRADA", cuentaId, cuenta.clienteId(), null,
                "Se cerró la cuenta " + cuentaId);
        return cuentaRepository.obtener(cuentaId);
    }
}
