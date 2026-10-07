package com.bancoxyz.cuentas.service;

import com.bancoxyz.cuentas.exception.ClienteNoEncontradoException;
import com.bancoxyz.cuentas.exception.ServicioNoDisponibleException;
import com.bancoxyz.cuentas.model.ClienteResumen;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Consulta al microservicio clientes, protegida con Circuit Breaker. */
@Component
public class ClientesClient {

    private static final Logger log = LoggerFactory.getLogger(ClientesClient.class);

    private final RestClient restClient;

    public ClientesClient(@Qualifier("clientesRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @CircuitBreaker(name = "clientes", fallbackMethod = "clientesNoDisponible")
    public ClienteResumen obtener(Long clienteId) {
        return restClient.get()
                .uri("/api/clientes/{id}", clienteId)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
                    throw new ClienteNoEncontradoException("No existe el cliente " + clienteId);
                })
                .body(ClienteResumen.class);
    }

    /**
     * Comportamiento alternativo: si clientes no responde (o el circuito está
     * abierto), no se abre la cuenta. Se prefiere rechazar a crear una cuenta sin
     * titular verificado.
     */
    public ClienteResumen clientesNoDisponible(Long clienteId, Throwable error) {
        if (error instanceof ClienteNoEncontradoException noEncontrado) {
            // Respuesta de negocio válida: no es una falla de clientes
            throw noEncontrado;
        }
        log.warn("No se pudo consultar el cliente {}: {}", clienteId, error.toString());
        throw new ServicioNoDisponibleException("No fue posible validar al cliente " + clienteId
                + " porque el servicio de clientes no responde. No se abrió la cuenta; intenta nuevamente.");
    }
}
