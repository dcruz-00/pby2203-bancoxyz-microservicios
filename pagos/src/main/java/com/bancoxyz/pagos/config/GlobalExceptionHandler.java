package com.bancoxyz.pagos.config;

import com.bancoxyz.pagos.exception.CuentasNoDisponibleException;
import com.bancoxyz.pagos.exception.PagoNoEncontradoException;
import com.bancoxyz.pagos.exception.RechazoCuentasException;
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

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Se devuelve al cliente el mismo código que dio cuentas (404, 409, 400...). */
    @ExceptionHandler(RechazoCuentasException.class)
    public ProblemDetail manejarRechazo(RechazoCuentasException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problema.setTitle("Operación rechazada por cuentas");
        return problema;
    }

    @ExceptionHandler(CuentasNoDisponibleException.class)
    public ProblemDetail manejarCuentasNoDisponible(CuentasNoDisponibleException ex) {
        return crear(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de cuentas no disponible",
                ex.getMessage() + ". No se realizó ningún movimiento de dinero; intenta nuevamente.");
    }

    @ExceptionHandler(PagoNoEncontradoException.class)
    public ProblemDetail manejarPagoNoEncontrado(PagoNoEncontradoException ex) {
        return crear(HttpStatus.NOT_FOUND, "Pago no encontrado", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail manejarErrorInesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return crear(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno",
                "Ocurrió un error inesperado. Revisa los logs del servicio.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String detalle = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(crear(HttpStatus.BAD_REQUEST, "Solicitud inválida", detalle));
    }

    private ProblemDetail crear(HttpStatus status, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalle);
        problema.setTitle(titulo);
        return problema;
    }
}
