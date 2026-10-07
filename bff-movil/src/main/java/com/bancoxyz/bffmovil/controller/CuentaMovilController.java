package com.bancoxyz.bffmovil.controller;

import com.bancoxyz.bffmovil.config.OperacionRechazadaException;
import com.bancoxyz.bffmovil.model.CuentaMovilDTO;
import com.bancoxyz.bffmovil.model.MovimientoMovilDTO;
import com.bancoxyz.bffmovil.model.OperacionMovilDTO;
import com.bancoxyz.bffmovil.model.PagoMovilRequest;
import com.bancoxyz.bffmovil.model.PagoServicio;
import com.bancoxyz.bffmovil.model.TransferenciaMovilRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * App móvil: respuestas livianas (solo los campos que muestra la app) y un límite
 * por operación propio del canal.
 */
@RestController
@RequestMapping("/movil")
public class CuentaMovilController {

    private static final int MAXIMO_MOVIMIENTOS = 10;

    private final RestClient cuentasClient;
    private final RestClient pagosClient;
    private final double limitePorOperacion;

    public CuentaMovilController(@Qualifier("cuentasClient") RestClient cuentasClient,
                                 @Qualifier("pagosClient") RestClient pagosClient,
                                 @Value("${bff.limite-por-operacion}") double limitePorOperacion) {
        this.cuentasClient = cuentasClient;
        this.pagosClient = pagosClient;
        this.limitePorOperacion = limitePorOperacion;
    }

    @GetMapping("/cuentas/{cuentaId}")
    public CuentaMovilDTO obtenerCuenta(@PathVariable Long cuentaId) {
        return cuentasClient.get()
                .uri("/api/cuentas/{id}", cuentaId)
                .retrieve()
                .body(CuentaMovilDTO.class);
    }

    /** Últimos movimientos de la cuenta (por defecto 5, máximo 10). */
    @GetMapping("/cuentas/{cuentaId}/movimientos")
    public List<MovimientoMovilDTO> ultimosMovimientos(@PathVariable Long cuentaId,
                                                       @RequestParam(defaultValue = "5") int limite) {
        int cantidad = Math.max(1, Math.min(limite, MAXIMO_MOVIMIENTOS));
        List<PagoServicio> pagos = pagosClient.get()
                .uri(uri -> uri.path("/api/pagos").queryParam("cuentaId", cuentaId)
                        .queryParam("limite", cantidad).build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<PagoServicio>>() {});
        return pagos.stream()
                .map(p -> new MovimientoMovilDTO(p.tipo(), p.monto(), p.estado(), p.fechaCreacion()))
                .toList();
    }

    @PostMapping("/transferencias")
    public ResponseEntity<OperacionMovilDTO> transferir(@Valid @RequestBody TransferenciaMovilRequest request) {
        verificarLimite(request.monto());
        PagoServicio pago = pagosClient.post()
                .uri("/api/pagos/transferencias")
                .body(request)
                .retrieve()
                .body(PagoServicio.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(resumir(pago));
    }

    @PostMapping("/pagos")
    public ResponseEntity<OperacionMovilDTO> pagar(@Valid @RequestBody PagoMovilRequest request) {
        verificarLimite(request.monto());
        PagoServicio pago = pagosClient.post()
                .uri("/api/pagos/servicios")
                .body(request)
                .retrieve()
                .body(PagoServicio.class);
        return ResponseEntity.status(HttpStatus.CREATED).body(resumir(pago));
    }

    private void verificarLimite(Double monto) {
        if (monto > limitePorOperacion) {
            throw new OperacionRechazadaException("El monto supera el límite por operación del canal móvil ("
                    + limitePorOperacion + "). Usa la banca web para montos mayores.");
        }
    }

    private OperacionMovilDTO resumir(PagoServicio pago) {
        return new OperacionMovilDTO(pago.idOperacion(), pago.estado(), pago.saldoResultante());
    }
}
