package com.bancoxyz.bffweb.config;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Autenticación del canal web: usuario y contraseña (HTTP Basic, siempre sobre
 * HTTPS) con rol WEB. Las credenciales vienen del config-repo.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder,
                                                 @Value("${bff.usuario}") String usuario,
                                                 @Value("${bff.clave}") String clave) {
        return new InMemoryUserDetailsManager(
                User.withUsername(usuario)
                        .password(encoder.encode(clave))
                        .roles("WEB")
                        .build()
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                    // Redirección interna a /error: sin esto, un error real llegaría como 401/403
                    .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                    // Healthcheck de Docker
                    .requestMatchers("/actuator/health/**").permitAll()
                    .requestMatchers("/web/**").hasRole("WEB")
                    .anyRequest().denyAll()
            )
            .httpBasic(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
