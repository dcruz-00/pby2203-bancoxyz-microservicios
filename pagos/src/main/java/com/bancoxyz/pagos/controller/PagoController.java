package com.bancoxyz.pagos.controller;

import com.bancoxyz.pagos.model.DepositoRequest;
import com.bancoxyz.pagos.model.PagoDTO;
import com.bancoxyz.pagos.model.PagoServicioRequest;
import com.bancoxyz.pagos.model.TipoPago;
import com.bancoxyz.pagos.model.TransferenciaRequest;
import com.bancoxyz.pagos.repository.PagoRepository;
import com.bancoxyz.pagos.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;
    private final PagoRepository pagoRepository;

    public PagoController(PagoService pagoService, PagoRepository pagoRepository) {
        this.pagoService = pagoService;
        this.pagoRepository = pagoRepository;
    }

    @PostMapping("/depositos")
    public ResponseEntity<PagoDTO> depositar(@Valid @RequestBody DepositoRequest request) {
        return creado(pagoService.procesar(TipoPago.DEPOSITO, request.cuentaId(), null, request.monto(), null));
    }

    @PostMapping("/servicios")
    public ResponseEntity<PagoDTO> pagarServicio(@Valid @RequestBody PagoServicioRequest request) {
        return creado(pagoService.procesar(TipoPago.PAGO, request.cuentaId(), null, request.monto(),
                request.comercio()));
    }

    @PostMapping("/transferencias")
    public ResponseEntity<PagoDTO> transferir(@Valid @RequestBody TransferenciaRequest request) {
        return creado(pagoService.procesar(TipoPago.TRANSFERENCIA, request.cuentaOrigenId(),
                request.cuentaDestinoId(), request.monto(), null));
    }

    /** Pagos de una cuenta (como origen o destino), más recientes primero. */
    @GetMapping
    public ResponseEntity<List<PagoDTO>> listar(@RequestParam(required = false) Long cuentaId,
            @RequestParam(defaultValue = "20") int limite) {
        return ResponseEntity.ok(pagoRepository.listar(cuentaId, Math.max(1, Math.min(limite, 100))));
    }

    @GetMapping("/{idOperacion}")
    public ResponseEntity<PagoDTO> obtener(@PathVariable UUID idOperacion) {
        return ResponseEntity.ok(pagoService.obtener(idOperacion));
    }

    private ResponseEntity<PagoDTO> creado(PagoDTO pago) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .header("X-Id-Operacion", pago.idOperacion().toString())
                .body(pago);
    }
}
