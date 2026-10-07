package com.bancoxyz.cuentas.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import java.time.Duration;

/**
 * Cliente HTTP para llamar a otros microservicios.
 *
 * Cada solicitud pasa por tres pasos:
 * 1. OAuth 2.0: obtiene (y reutiliza mientras no venza) un token client_credentials
 *    de auth-server y lo agrega como "Authorization: Bearer".
 * 2. Balanceo de carga: reemplaza el nombre del servicio (https://clientes) por la
 *    dirección de una de sus instancias registradas en Eureka.
 * 3. HTTPS: confía solo en el certificado del proyecto.
 */
@Configuration
public class HttpInternoConfig {

    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository registros,
                                                          OAuth2AuthorizedClientService clientesAutorizados) {
        // Sin usuario final: el gestor por defecto usa el flujo client_credentials
        return new AuthorizedClientServiceOAuth2AuthorizedClientManager(registros, clientesAutorizados);
    }

    @Bean
    RestClient clientesRestClient(LoadBalancerClient loadBalancerClient,
                                  OAuth2AuthorizedClientManager authorizedClientManager,
                                  @Value("${server.ssl.key-store}") Resource keyStore,
                                  @Value("${server.ssl.key-store-password}") String keyStorePassword) throws Exception {
        return crear("https://clientes", "clientes", loadBalancerClient, authorizedClientManager,
                keyStore, keyStorePassword);
    }

    private RestClient crear(String urlBase, String registroOAuth, LoadBalancerClient loadBalancerClient,
                             OAuth2AuthorizedClientManager authorizedClientManager,
                             Resource keyStore, String keyStorePassword) throws Exception {
        OAuth2ClientHttpRequestInterceptor oauth = new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);
        oauth.setClientRegistrationIdResolver(request -> registroOAuth);
        // Token del servicio, no del usuario: todas las solicitudes comparten el mismo token
        oauth.setPrincipalResolver(request -> null);

        return RestClient.builder()
                .baseUrl(urlBase)
                .requestFactory(requestFactory(keyStore, keyStorePassword))
                .requestInterceptor(oauth)
                .requestInterceptor(new LoadBalancerInterceptor(loadBalancerClient))
                .build();
    }

    private HttpComponentsClientHttpRequestFactory requestFactory(Resource keyStore, String keyStorePassword)
            throws Exception {
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
        // Una dependencia lenta no retiene la solicitud más de 5 s
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }
}
