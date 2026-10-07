package com.bancoxyz.pagos.controller;

import com.bancoxyz.pagos.model.MovimientoDTO;
import com.bancoxyz.pagos.repository.MovimientoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movimientos")
public class MovimientoController {

    private final MovimientoRepository repository;

    public MovimientoController(MovimientoRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<MovimientoDTO>> listar(@RequestParam(required = false) Long cuentaId) {
        return ResponseEntity.ok(repository.listar(cuentaId));
    }

    @GetMapping("/{idOperacion}")
    public ResponseEntity<MovimientoDTO> obtener(@PathVariable String idOperacion) {
        return repository.buscarPorIdOperacion(idOperacion)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}