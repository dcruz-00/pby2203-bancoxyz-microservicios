package com.bancoxyz.movimientos.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * movimientos como resource server OAuth 2.0: la consulta de movimientos exige
 * un JWT válido emitido por auth-server con el scope "movimientos.leer".
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Redirección interna a /error: sin esto, un error real (ej. 500)
                // llegaría al cliente como 401/403 por el denyAll del final
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                // Abierto para los healthchecks de Docker (sin detalles para anónimos)
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/actuator/**").hasAuthority("SCOPE_monitoreo")
                .requestMatchers(HttpMethod.GET, "/api/movimientos", "/api/movimientos/*")
                    .hasAuthority("SCOPE_movimientos.leer")
                // Todo lo que no esté listado arriba se deniega
                .anyRequest().denyAll())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            // API sin estado: sin sesión HTTP ni cookies, por lo que CSRF no aplica
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
}