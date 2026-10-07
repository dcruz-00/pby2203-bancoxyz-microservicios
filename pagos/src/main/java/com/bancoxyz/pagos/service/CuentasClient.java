package com.bancoxyz.pagos.service;

import com.bancoxyz.pagos.exception.CuentasNoDisponibleException;
import com.bancoxyz.pagos.exception.RechazoCuentasException;
import com.bancoxyz.pagos.model.ErrorCuentas;
import com.bancoxyz.pagos.model.OperacionCuentasRequest;
import com.bancoxyz.pagos.model.OperacionCuentasResultado;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/**
 * Solicita a cuentas que aplique un depósito, pago o transferencia.
 *
 * Resilience4j envuelve la llamada así: Retry( CircuitBreaker( llamada ) ).
 * - Retry: reintenta fallas técnicas (cuentas caído, timeout, error 5xx). Es
 *   seguro porque cuentas es idempotente por id de operación.
 * - Circuit Breaker: tras fallas repetidas deja de llamar a cuentas por un
 *   tiempo y falla de inmediato.
 * - Un rechazo de negocio (4xx: saldo insuficiente, cuenta cerrada...) no se
 *   reintenta ni cuenta como falla.
 */
@Component
public class CuentasClient {

    private static final Logger log = LoggerFactory.getLogger(CuentasClient.class);

    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    public CuentasClient(@Qualifier("cuentasRestClient") RestClient restClient, JsonMapper jsonMapper) {
        this.restClient = restClient;
        this.jsonMapper = jsonMapper;
    }

    @Retry(name = "cuentas", fallbackMethod = "cuentasNoDisponible")
    @CircuitBreaker(name = "cuentas")
    public OperacionCuentasResultado aplicar(OperacionCuentasRequest solicitud) {
        log.info("[idOperacion={}] solicitando {} a cuentas", solicitud.idOperacion(), solicitud.tipo());
        return restClient.post()
                .uri("/api/cuentas/operaciones")
                .body(solicitud)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    String cuerpo = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    throw new RechazoCuentasException(response.getStatusCode(), detalle(cuerpo));
                })
                .body(OperacionCuentasResultado.class);
    }

    /**
     * Comportamiento alternativo, tras agotar los reintentos o con el circuito
     * abierto: se informa que el servicio no está disponible. Los rechazos de
     * negocio se propagan sin cambios.
     */
    public OperacionCuentasResultado cuentasNoDisponible(OperacionCuentasRequest solicitud, Throwable error) {
        if (error instanceof RechazoCuentasException rechazo) {
            throw rechazo;
        }
        String causa = error instanceof CallNotPermittedException
                ? "circuito abierto: cuentas falló repetidamente y no se contactó"
                : error.getClass().getSimpleName() + ": " + error.getMessage();
        log.warn("[idOperacion={}] cuentas no disponible ({})", solicitud.idOperacion(), causa);
        throw new CuentasNoDisponibleException("El servicio de cuentas no está disponible (" + causa + ")");
    }

    private String detalle(String cuerpo) {
        try {
            ErrorCuentas error = jsonMapper.readValue(cuerpo, ErrorCuentas.class);
            return error.detail() != null ? error.detail() : cuerpo;
        } catch (RuntimeException ex) {
            return cuerpo;
        }
    }
}
