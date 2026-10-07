package com.bancoxyz.clientes.config;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * clientes como resource server OAuth 2.0: lectura con "clientes.leer" y
 * registro o actualización de perfiles con "clientes.escribir".
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers("/actuator/health/**").permitAll()
                .requestMatchers("/actuator/**").hasAuthority("SCOPE_monitoreo")
                .requestMatchers(HttpMethod.GET, "/api/clientes", "/api/clientes/*", "/api/clientes/*/notificaciones")
                    .hasAuthority("SCOPE_clientes.leer")
                .requestMatchers(HttpMethod.POST, "/api/clientes").hasAuthority("SCOPE_clientes.escribir")
                .requestMatchers(HttpMethod.PUT, "/api/clientes/*").hasAuthority("SCOPE_clientes.escribir")
                .anyRequest().denyAll())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
