package com.bancoxyz.bffweb.controller;

import com.bancoxyz.bffweb.model.ClienteWebDTO;
import com.bancoxyz.bffweb.model.CuentaWebDTO;
import com.bancoxyz.bffweb.model.NotificacionWebDTO;
import com.bancoxyz.bffweb.model.PagoWebDTO;
import com.bancoxyz.bffweb.model.ResumenClienteWebDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Resumen del cliente para la página principal de la banca web: combina datos de
 * los tres microservicios en una sola respuesta, para que el navegador no tenga
 * que hacer varias llamadas.
 *
 * Degradación controlada: si un microservicio no responde, su sección queda
 * vacía y se informa en "advertencias", pero el resto del resumen se entrega.
 */
@RestController
@RequestMapping("/web/clientes")
public class ResumenClienteController {

    private static final Logger log = LoggerFactory.getLogger(ResumenClienteController.class);
    private static final int PAGOS_POR_CUENTA = 5;
    private static final int ULTIMOS_PAGOS = 10;

    private final RestClient clientesClient;
    private final RestClient cuentasClient;
    private final RestClient pagosClient;

    public ResumenClienteController(@Qualifier("clientesClient") RestClient clientesClient,
                                    @Qualifier("cuentasClient") RestClient cuentasClient,
                                    @Qualifier("pagosClient") RestClient pagosClient) {
        this.clientesClient = clientesClient;
        this.cuentasClient = cuentasClient;
        this.pagosClient = pagosClient;
    }

    @GetMapping("/{clienteId}/resumen")
    public ResumenClienteWebDTO resumen(@PathVariable Long clienteId) {
        List<String> advertencias = new ArrayList<>();

        ClienteWebDTO cliente = null;
        List<NotificacionWebDTO> notificaciones = List.of();
        try {
            cliente = clientesClient.get().uri("/api/clientes/{id}", clienteId)
                    .retrieve().body(ClienteWebDTO.class);
            notificaciones = clientesClient.get()
                    .uri(uri -> uri.path("/api/clientes/{id}/notificaciones").queryParam("limite", 10)
                            .build(clienteId))
                    .retrieve().body(new ParameterizedTypeReference<List<NotificacionWebDTO>>() {});
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
                // El cliente no existe: no hay resumen que armar
                throw ex;
            }
            advertencias.add(degradar("clientes", ex));
        } catch (RestClientException ex) {
            advertencias.add(degradar("clientes", ex));
        }

        List<CuentaWebDTO> cuentas = List.of();
        try {
            cuentas = cuentasClient.get()
                    .uri(uri -> uri.path("/api/cuentas").queryParam("clienteId", clienteId).build())
                    .retrieve().body(new ParameterizedTypeReference<List<CuentaWebDTO>>() {});
        } catch (RestClientException ex) {
            advertencias.add(degradar("cuentas", ex));
        }

        List<PagoWebDTO> pagos = new ArrayList<>();
        try {
            for (CuentaWebDTO cuenta : cuentas) {
                pagos.addAll(pagosClient.get()
                        .uri(uri -> uri.path("/api/pagos").queryParam("cuentaId", cuenta.cuentaId())
                                .queryParam("limite", PAGOS_POR_CUENTA).build())
                        .retrieve().body(new ParameterizedTypeReference<List<PagoWebDTO>>() {}));
            }
        } catch (RestClientException ex) {
            advertencias.add(degradar("pagos", ex));
        }

        // Una transferencia entre cuentas del mismo cliente aparece en ambas: se deja una vez
        List<PagoWebDTO> ultimosPagos = pagos.stream()
                .filter(distintoPorId())
                .sorted(Comparator.comparing(PagoWebDTO::fechaCreacion).reversed())
                .limit(ULTIMOS_PAGOS)
                .toList();

        double saldoTotal = cuentas.stream()
                .filter(c -> "ACTIVA".equals(c.estado()))
                .mapToDouble(CuentaWebDTO::saldo)
                .sum();

        return new ResumenClienteWebDTO(cliente, cuentas, saldoTotal, ultimosPagos, notificaciones, advertencias);
    }

    private String degradar(String servicio, RestClientException ex) {
        log.warn("Resumen sin datos de {}: {}", servicio, ex.getMessage());
        return "No se pudieron obtener los datos de " + servicio + "; la sección se muestra vacía";
    }

    private static Predicate<PagoWebDTO> distintoPorId() {
        Set<Object> vistos = new HashSet<>();
        return pago -> vistos.add(pago.idOperacion());
    }
}
