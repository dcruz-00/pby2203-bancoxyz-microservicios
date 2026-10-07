package com.bancoxyz.bffweb.controller;

import com.bancoxyz.bffweb.model.AperturaWebRequest;
import com.bancoxyz.bffweb.model.CuentaWebDTO;
import com.bancoxyz.bffweb.model.PagoWebDTO;
import com.bancoxyz.bffweb.model.TransaccionWebDTO;
import com.bancoxyz.bffweb.model.TransferenciaWebRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;

/** Banca web: datos completos y operaciones de la banca en línea. */
@RestController
@RequestMapping("/web")
public class CuentaWebController {

    private final RestClient cuentasClient;
    private final RestClient pagosClient;

    public CuentaWebController(@Qualifier("cuentasClient") RestClient cuentasClient,
                               @Qualifier("pagosClient") RestClient pagosClient) {
        this.cuentasClient = cuentasClient;
        this.pagosClient = pagosClient;
    }

    @GetMapping("/cuentas")
    public List<CuentaWebDTO> listarCuentas() {
        return cuentasClient.get()
                .uri("/api/cuentas")
                .retrieve()
                .body(new ParameterizedTypeReference<List<CuentaWebDTO>>() {});
    }

    @GetMapping("/cuentas/{cuentaId}")
    public CuentaWebDTO obtenerCuenta(@PathVariable Long cuentaId) {
        return cuentasClient.get()
                .uri("/api/cuentas/{id}", cuentaId)
                .retrieve()
                .body(CuentaWebDTO.class);
    }

    /** Apertura de cuenta desde la banca web. */
    @PostMapping("/cuentas")
    public ResponseEntity<CuentaWebDTO> abrirCuenta(@Valid @RequestBody AperturaWebRequest request) {
        CuentaWebDTO cuenta = cuentasClient.post()
                .uri("/api/cuentas")
                .body(request)
                .retrieve()
                .body(CuentaWebDTO.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(cuenta);
    }

    /** Historial de pagos de la cuenta (como origen o destino). */
    @GetMapping("/cuentas/{cuentaId}/pagos")
    public List<PagoWebDTO> listarPagos(@PathVariable Long cuentaId,
                                        @RequestParam(defaultValue = "20") int limite) {
        return pagosClient.get()
                .uri(uri -> uri.path("/api/pagos").queryParam("cuentaId", cuentaId)
                        .queryParam("limite", limite).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<PagoWebDTO>>() {});
    }

    @PostMapping("/transferencias")
    public ResponseEntity<PagoWebDTO> transferir(@Valid @RequestBody TransferenciaWebRequest request) {
        PagoWebDTO pago = pagosClient.post()
                .uri("/api/pagos/transferencias")
                .body(request)
                .retrieve()
                .body(PagoWebDTO.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(pago);
    }

    /** Transacciones diarias procesadas por el Batch (datos heredados). */
    @GetMapping("/transacciones")
    public List<TransaccionWebDTO> listarTransacciones() {
        return cuentasClient.get()
                .uri("/api/transacciones")
                .retrieve()
                .body(new ParameterizedTypeReference<List<TransaccionWebDTO>>() {});
    }
}
