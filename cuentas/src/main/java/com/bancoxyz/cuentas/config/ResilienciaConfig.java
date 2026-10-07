package com.bancoxyz.cuentas.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/** Registra en el log cada cambio de estado del circuito de retiros. */
@Configuration
public class ResilienciaConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienciaConfig.class);

    public ResilienciaConfig(CircuitBreakerRegistry registry) {
        registry.circuitBreaker("retiro").getEventPublisher()
                .onStateTransition(evento -> log.warn("Circuit breaker '{}': {}",
                        evento.getCircuitBreakerName(), evento.getStateTransition()));
    }
}