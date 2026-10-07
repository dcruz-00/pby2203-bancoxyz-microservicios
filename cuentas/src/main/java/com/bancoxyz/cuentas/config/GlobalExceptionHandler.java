package com.bancoxyz.cuentas.config;

import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.bancoxyz.cuentas.exception.ClienteNoEncontradoException;
import com.bancoxyz.cuentas.exception.CuentaInactivaException;
import com.bancoxyz.cuentas.exception.CuentaNoEncontradaException;
import com.bancoxyz.cuentas.exception.OperacionNoPermitidaException;
import com.bancoxyz.cuentas.exception.PublicacionEventoException;
import com.bancoxyz.cuentas.exception.SaldoInsuficienteException;
import com.bancoxyz.cuentas.exception.ServicioNoDisponibleException;
import com.bancoxyz.cuentas.exception.SolicitudInvalidaException;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ProblemDetail manejarCuentaNoEncontrada(CuentaNoEncontradaException ex) {
        return crear(HttpStatus.NOT_FOUND, "Cuenta no encontrada", ex.getMessage());
    }

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ProblemDetail manejarSaldoInsuficiente(SaldoInsuficienteException ex) {
        return crear(HttpStatus.CONFLICT, "Saldo insuficiente", ex.getMessage());
    }

    @ExceptionHandler(CuentaInactivaException.class)
    public ProblemDetail manejarCuentaInactiva(CuentaInactivaException ex) {
        return crear(HttpStatus.CONFLICT, "Cuenta cerrada", ex.getMessage());
    }

    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ProblemDetail manejarOperacionNoPermitida(OperacionNoPermitidaException ex) {
        return crear(HttpStatus.CONFLICT, "Operación no permitida", ex.getMessage());
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ProblemDetail manejarSolicitudInvalida(SolicitudInvalidaException ex) {
        return crear(HttpStatus.BAD_REQUEST, "Solicitud inválida", ex.getMessage());
    }

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ProblemDetail manejarClienteNoEncontrado(ClienteNoEncontradoException ex) {
        return crear(HttpStatus.UNPROCESSABLE_CONTENT, "Cliente no encontrado", ex.getMessage());
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ProblemDetail manejarServicioNoDisponible(ServicioNoDisponibleException ex) {
        return crear(HttpStatus.SERVICE_UNAVAILABLE, "Servicio dependiente no disponible", ex.getMessage());
    }

    @ExceptionHandler(PublicacionEventoException.class)
    public ProblemDetail manejarPublicacionFallida(PublicacionEventoException ex) {
        return crear(HttpStatus.SERVICE_UNAVAILABLE, "Servicio temporalmente no disponible",
                "No se pudo completar el retiro y no se realizó ningún cargo. Intenta nuevamente en unos minutos.");
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ProblemDetail manejarCircuitoAbierto(CallNotPermittedException ex) {
        return crear(HttpStatus.SERVICE_UNAVAILABLE, "Retiros suspendidos temporalmente",
                "El servicio de retiros está suspendido por fallas recientes. No se realizó ningún cargo.");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail manejarErrorInesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return crear(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado. Revisa los logs del servicio.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest()
                .body(crear(HttpStatus.BAD_REQUEST, "Solicitud inválida", detalle));
    }

    private ProblemDetail crear(HttpStatus status, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(titulo);
        return problema;
    }
}