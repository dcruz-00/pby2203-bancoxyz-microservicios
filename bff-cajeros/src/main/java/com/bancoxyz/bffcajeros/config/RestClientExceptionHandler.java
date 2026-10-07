package com.bancoxyz.bffcajeros.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * Traduce las respuestas de los microservicios para el canal: los errores de
 * negocio (4xx) y de servidor (5xx) se devuelven con el mismo código y cuerpo, y
 * la falta de respuesta (o circuito abierto) como 503.
 */
@RestControllerAdvice
public class RestClientExceptionHandler {

    @ExceptionHandler(HttpClientErrorException.class)
    public ResponseEntity<String> manejarErrorCliente(HttpClientErrorException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ex.getResponseBodyAsString());
    }

    @ExceptionHandler(HttpServerErrorException.class)
    public ResponseEntity<String> manejarErrorServidor(HttpServerErrorException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(ex.getResponseBodyAsString());
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ProblemDetail manejarErrorConexion(ResourceAccessException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "No fue posible comunicarse con " + ex.getMessage());
        problema.setTitle("Servicio no disponible");
        return problema;
    }

    @ExceptionHandler(OperacionRechazadaException.class)
    public ProblemDetail manejarRechazoCanal(OperacionRechazadaException ex) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problema.setTitle("Operación no permitida en este canal");
        return problema;
    }
}
