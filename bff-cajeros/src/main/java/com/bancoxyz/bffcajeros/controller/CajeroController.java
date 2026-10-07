package com.bancoxyz.bffcajeros.controller;

import com.bancoxyz.bffcajeros.config.OperacionRechazadaException;
import com.bancoxyz.bffcajeros.model.CuentaCajeroDTO;
import com.bancoxyz.bffcajeros.model.RetiroCajeroRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

/**
 * Cajeros automáticos: solo consulta de saldo y retiro, con la respuesta mínima
 * (cuenta y saldo) y un monto máximo por retiro.
 */
@RestController
@RequestMapping("/cajero")
public class CajeroController {

    private final RestClient cuentasClient;
    private final double limitePorRetiro;

    public CajeroController(@Qualifier("cuentasClient") RestClient cuentasClient,
                            @Value("${bff.limite-por-operacion}") double limitePorRetiro) {
        this.cuentasClient = cuentasClient;
        this.limitePorRetiro = limitePorRetiro;
    }

    @GetMapping("/cuentas/{cuentaId}/saldo")
    public CuentaCajeroDTO consultarSaldo(@PathVariable Long cuentaId) {
        return cuentasClient.get()
                .uri("/api/cuentas/{id}", cuentaId)
                .retrieve()
                .body(CuentaCajeroDTO.class);
    }

    /** El retiro inicia la saga en cuentas; el cajero recibe solo el saldo resultante. */
    @PatchMapping("/cuentas/{cuentaId}/retiro")
    public ResponseEntity<CuentaCajeroDTO> retirar(@PathVariable Long cuentaId,
                                                   @Valid @RequestBody RetiroCajeroRequest request) {
        if (request.monto() > limitePorRetiro) {
            throw new OperacionRechazadaException("El monto supera el máximo por retiro en cajero ("
                    + limitePorRetiro + ")");
        }
        ResponseEntity<CuentaCajeroDTO> respuesta = cuentasClient.patch()
                .uri("/api/cuentas/{id}/retiro", cuentaId)
                .body(request)
                .retrieve()
                .toEntity(CuentaCajeroDTO.class);
        // Respuesta nueva (no se reenvían las cabeceras de cuentas, como Content-Length,
        // que corresponden al cuerpo completo); solo se conserva el id de operación
        return ResponseEntity.ok()
                .header("X-Id-Operacion", respuesta.getHeaders().getFirst("X-Id-Operacion"))
                .body(respuesta.getBody());
    }
}
