package com.bancoxyz.cuentas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * cuentas como resource server OAuth 2.0: cada solicitud debe traer un JWT
 * válido emitido por auth-server, con el scope que exige el endpoint.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Abierto para los healthchecks de Docker (sin detalles para anónimos)
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/actuator/**").hasAuthority("SCOPE_monitoreo")
                .requestMatchers(HttpMethod.GET, "/api/cuentas", "/api/cuentas/*", "/api/transacciones")
                    .hasAuthority("SCOPE_cuentas.leer")
                .requestMatchers(HttpMethod.PATCH, "/api/cuentas/*/retiro")
                    .hasAuthority("SCOPE_cuentas.retirar")
                // Todo lo que no esté listado arriba se deniega
                .anyRequest().denyAll())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            // API sin estado: sin sesión HTTP ni cookies, por lo que CSRF no aplica
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
}