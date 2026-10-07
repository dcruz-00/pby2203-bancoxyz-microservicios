package com.bancoxyz.cuentas.controller;

import com.bancoxyz.cuentas.exception.CuentaNoEncontradaException;
import com.bancoxyz.cuentas.model.CuentaInteresDTO;
import com.bancoxyz.cuentas.model.RetiroRequest;
import com.bancoxyz.cuentas.repository.CuentaInteresRepository;
import com.bancoxyz.cuentas.service.RetiroService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {

    private final CuentaInteresRepository repository;
    private final RetiroService retiroService;

    public CuentaController(CuentaInteresRepository repository, RetiroService retiroService) {
        this.repository = repository;
        this.retiroService = retiroService;
    }

    @GetMapping
    public ResponseEntity<List<CuentaInteresDTO>> listar() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/{cuentaId}")
    public ResponseEntity<CuentaInteresDTO> obtener(@PathVariable Long cuentaId) {
        CuentaInteresDTO cuenta = repository.findByCuentaId(cuentaId);
        if (cuenta == null) {
            throw new CuentaNoEncontradaException("No existe la cuenta " + cuentaId);
        }
        return ResponseEntity.ok(cuenta);
    }

    @PatchMapping("/{cuentaId}/retiro")
    public ResponseEntity<CuentaInteresDTO> retirar(@PathVariable Long cuentaId,
            @Valid @RequestBody RetiroRequest request) {
        RetiroService.Resultado resultado = retiroService.retirar(cuentaId, request.monto());
        return ResponseEntity.ok()
                .header("X-Id-Operacion", resultado.idOperacion().toString())
                .body(resultado.cuenta());
    }
}