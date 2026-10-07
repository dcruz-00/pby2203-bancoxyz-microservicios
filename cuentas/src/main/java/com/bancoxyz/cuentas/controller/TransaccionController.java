package com.bancoxyz.cuentas.controller;

import com.bancoxyz.cuentas.model.TransaccionDTO;
import com.bancoxyz.cuentas.repository.TransaccionRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transacciones")
public class TransaccionController {

    private final TransaccionRepository repository;

    public TransaccionController(TransaccionRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ResponseEntity<List<TransaccionDTO>> listar() {
        return ResponseEntity.ok(repository.findAll());
    }
}