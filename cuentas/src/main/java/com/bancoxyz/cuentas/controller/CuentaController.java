package com.bancoxyz.cuentas.controller;

import com.bancoxyz.cuentas.model.AperturaCuentaRequest;
import com.bancoxyz.cuentas.model.CuentaDTO;
import com.bancoxyz.cuentas.model.OperacionRequest;
import com.bancoxyz.cuentas.model.OperacionResultado;
import com.bancoxyz.cuentas.model.RetiroRequest;
import com.bancoxyz.cuentas.repository.CuentaRepository;
import com.bancoxyz.cuentas.service.CuentaService;
import com.bancoxyz.cuentas.service.OperacionService;
import com.bancoxyz.cuentas.service.RetiroService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {

    private final CuentaRepository repository;
    private final CuentaService cuentaService;
    private final RetiroService retiroService;
    private final OperacionService operacionService;

    public CuentaController(CuentaRepository repository, CuentaService cuentaService, RetiroService retiroService,
                            OperacionService operacionService) {
        this.repository = repository;
        this.cuentaService = cuentaService;
        this.retiroService = retiroService;
        this.operacionService = operacionService;
    }

    /** Lista todas las cuentas, o solo las de un cliente si se indica clienteId. */
    @GetMapping
    public ResponseEntity<List<CuentaDTO>> listar(@RequestParam(required = false) Long clienteId) {
        return ResponseEntity.ok(clienteId == null ? repository.findAll() : repository.findByClienteId(clienteId));
    }

    @GetMapping("/{cuentaId}")
    public ResponseEntity<CuentaDTO> obtener(@PathVariable Long cuentaId) {
        return ResponseEntity.ok(repository.obtener(cuentaId));
    }

    /** Apertura de cuenta para un cliente existente (validado en el microservicio clientes). */
    @PostMapping
    public ResponseEntity<CuentaDTO> abrir(@Valid @RequestBody AperturaCuentaRequest request) {
        CuentaDTO cuenta = cuentaService.abrir(request);
        return ResponseEntity.created(URI.create("/api/cuentas/" + cuenta.cuentaId())).body(cuenta);
    }

    @PatchMapping("/{cuentaId}/cierre")
    public ResponseEntity<CuentaDTO> cerrar(@PathVariable Long cuentaId) {
        return ResponseEntity.ok(cuentaService.cerrar(cuentaId));
    }

    /** Retiro: inicia la saga con el microservicio pagos (evento retiro-realizado). */
    @PatchMapping("/{cuentaId}/retiro")
    public ResponseEntity<CuentaDTO> retirar(@PathVariable Long cuentaId,
            @Valid @RequestBody RetiroRequest request) {
        RetiroService.Resultado resultado = retiroService.retirar(cuentaId, request.monto());
        return ResponseEntity.ok()
                .header("X-Id-Operacion", resultado.idOperacion().toString())
                .body(resultado.cuenta());
    }

    /** Depósitos, pagos y transferencias solicitados por el microservicio pagos. */
    @PostMapping("/operaciones")
    public ResponseEntity<OperacionResultado> aplicarOperacion(@Valid @RequestBody OperacionRequest request) {
        return ResponseEntity.ok(operacionService.aplicar(request));
    }
}
