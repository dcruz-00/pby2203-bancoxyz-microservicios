package com.bancoxyz.bffmovil.config;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;

/**
 * Clientes HTTP hacia los microservicios. Cada solicitud pasa, en orden, por:
 * 1. Circuit Breaker (uno por microservicio): si el servicio falla repetidamente,
 *    se responde de inmediato sin llamarlo.
 * 2. OAuth 2.0: agrega el token client_credentials del BFF ("bff-movil"), que se
 *    reutiliza hasta que vence.
 * 3. Balanceo de carga: elige una instancia del servicio registrada en Eureka.
 * 4. HTTPS: confía solo en el certificado del proyecto.
 */
@Configuration
public class RestClientConfig {

    /** Registro OAuth 2.0 del BFF en el config-repo (spring.security.oauth2.client.registration.bff). */
    private static final String REGISTRO_OAUTH = "bff";

    private final LoadBalancerClient loadBalancerClient;
    private final CircuitBreakerFactory<?, ?> circuitBreakerFactory;
    private final Resource keyStore;
    private final String keyStorePassword;

    public RestClientConfig(LoadBalancerClient loadBalancerClient,
                            CircuitBreakerFactory<?, ?> circuitBreakerFactory,
                            @Value("${server.ssl.key-store}") Resource keyStore,
                            @Value("${server.ssl.key-store-password}") String keyStorePassword) {
        this.loadBalancerClient = loadBalancerClient;
        this.circuitBreakerFactory = circuitBreakerFactory;
        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
    }

    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository registros,
                                                          OAuth2AuthorizedClientService clientesAutorizados) {
        return new AuthorizedClientServiceOAuth2AuthorizedClientManager(registros, clientesAutorizados);
    }

    @Bean
    RestClient cuentasClient(OAuth2AuthorizedClientManager manager) throws Exception {
        return crear("cuentas", manager);
    }

    @Bean
    RestClient pagosClient(OAuth2AuthorizedClientManager manager) throws Exception {
        return crear("pagos", manager);
    }

    private RestClient crear(String servicio, OAuth2AuthorizedClientManager manager) throws Exception {
        OAuth2ClientHttpRequestInterceptor oauth = new OAuth2ClientHttpRequestInterceptor(manager);
        oauth.setClientRegistrationIdResolver(request -> REGISTRO_OAUTH);
        // Token del BFF, no del usuario del canal: todas las solicitudes comparten el mismo token
        oauth.setPrincipalResolver(request -> null);

        return RestClient.builder()
                .baseUrl("https://" + servicio)
                .requestFactory(requestFactory())
                .requestInterceptor(circuitBreakerInterceptor(servicio, circuitBreakerFactory.create(servicio)))
                .requestInterceptor(oauth)
                .requestInterceptor(new LoadBalancerInterceptor(loadBalancerClient))
                .build();
    }

    private HttpComponentsClientHttpRequestFactory requestFactory() throws Exception {
        SSLContext sslContext = SSLContexts.custom()
                .loadTrustMaterial(keyStore.getURL(), keyStorePassword.toCharArray())
                .build();

        // Se confía solo en el certificado del proyecto, pero no se verifica el nombre de host:
        // con varias réplicas, Eureka entrega la IP de cada instancia y un certificado
        // autofirmado no puede incluir IPs que Docker asigna dinámicamente.
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext,
                NoopHostnameVerifier.INSTANCE);

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(socketFactory)
                        .setDefaultConnectionConfig(ConnectionConfig.custom()
                                .setConnectTimeout(Timeout.ofSeconds(2))
                                .build())
                        .build())
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    /**
     * Ejecuta la solicitud dentro del Circuit Breaker. Si el servicio no responde o
     * el circuito está abierto, se lanza ResourceAccessException, que
     * RestClientExceptionHandler convierte en 503.
     */
    private ClientHttpRequestInterceptor circuitBreakerInterceptor(String servicio, CircuitBreaker circuitBreaker) {
        return (request, body, execution) -> circuitBreaker.run(
                () -> {
                    try {
                        return execution.execute(request, body);
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                },
                throwable -> {
                    if (throwable instanceof CallNotPermittedException) {
                        throw new ResourceAccessException(servicio
                                + ": circuito abierto, llamada rechazada sin contactar al servicio");
                    }
                    Throwable causa = (throwable instanceof UncheckedIOException && throwable.getCause() != null)
                            ? throwable.getCause()
                            : throwable;
                    throw new ResourceAccessException(servicio + ": sin respuesta ("
                            + causa.getClass().getSimpleName() + ")");
                });
    }
}
