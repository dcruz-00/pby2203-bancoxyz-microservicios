package com.bancoxyz.clientes.controller;

import com.bancoxyz.clientes.exception.ClienteNoEncontradoException;
import com.bancoxyz.clientes.model.ClienteDTO;
import com.bancoxyz.clientes.model.ClienteRequest;
import com.bancoxyz.clientes.model.NotificacionDTO;
import com.bancoxyz.clientes.repository.ClienteRepository;
import com.bancoxyz.clientes.repository.NotificacionRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private static final Logger log = LoggerFactory.getLogger(ClienteController.class);

    private final ClienteRepository clienteRepository;
    private final NotificacionRepository notificacionRepository;

    public ClienteController(ClienteRepository clienteRepository, NotificacionRepository notificacionRepository) {
        this.clienteRepository = clienteRepository;
        this.notificacionRepository = notificacionRepository;
    }

    @GetMapping
    public ResponseEntity<List<ClienteDTO>> listar() {
        return ResponseEntity.ok(clienteRepository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(buscar(id));
    }

    @PostMapping
    public ResponseEntity<ClienteDTO> crear(@Valid @RequestBody ClienteRequest request) {
        Long id = clienteRepository.crear(request);
        log.info("Cliente {} registrado (RUT {})", id, request.rut());
        return ResponseEntity.created(URI.create("/api/clientes/" + id)).body(buscar(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        if (!clienteRepository.actualizar(id, request)) {
            throw new ClienteNoEncontradoException("No existe el cliente " + id);
        }
        log.info("Perfil del cliente {} actualizado", id);
        return ResponseEntity.ok(buscar(id));
    }

    /** Notificaciones generadas por las transacciones y alertas de seguridad del cliente. */
    @GetMapping("/{id}/notificaciones")
    public ResponseEntity<List<NotificacionDTO>> notificaciones(@PathVariable Long id,
            @RequestParam(defaultValue = "20") int limite) {
        buscar(id);
        return ResponseEntity.ok(notificacionRepository.listar(id, Math.max(1, Math.min(limite, 100))));
    }

    private ClienteDTO buscar(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("No existe el cliente " + id));
    }
}
