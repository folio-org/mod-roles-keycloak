package org.folio.roles.integration.keyclock.configuration;

import static org.folio.common.utils.tls.HttpClientTlsUtils.buildHttpServiceClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.integration.keyclock.KeycloakAdminTokenProvider;
import org.folio.roles.integration.keyclock.RealmConfigurationProvider;
import org.folio.roles.integration.keyclock.client.KeycloakAdminClient;
import org.folio.roles.integration.keyclock.client.KeycloakTokenClient;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Wires the native-image-friendly Keycloak integration: two Spring HTTP-interface clients
 * ({@link KeycloakTokenClient} and {@link KeycloakAdminClient}) plus the admin token provider that injects the
 * bearer token, replacing the RESTEasy {@code keycloak-admin-client}.
 */
@Log4j2
@Configuration
@RequiredArgsConstructor
public class KeycloakConfiguration {

  private final KeycloakConfigurationProperties configuration;
  private final RealmConfigurationProvider realmConfigurationProvider;

  /**
   * Executor backing {@link org.folio.roles.integration.keyclock.KeycloakPermissionsExecutor} for parallel
   * Keycloak permission create/delete calls.
   *
   * <p>Created unconditionally (no {@code @ConditionalOnExpression}, which would fork the AOT context and
   * complicate native metadata). The pool is sized from {@code application.keycloak.permissions.parallelism};
   * with {@code allowCoreThreadTimeOut} and no submitted work it starts zero threads, so a parallelism of 1 —
   * where the executor runs sequentially — allocates nothing. Parallel vs. sequential is decided at runtime by
   * the executor based on the configured parallelism.</p>
   */
  @Bean(destroyMethod = "shutdown")
  public ExecutorService keycloakPermissionsExecutorService() {
    int parallelism = Math.max(1, configuration.getPermissions().getParallelism());
    log.info("Creating Keycloak permissions executor service with parallelism={}", parallelism);
    var executor = new ThreadPoolExecutor(parallelism, parallelism, 60L, TimeUnit.SECONDS,
      new LinkedBlockingQueue<>());
    executor.allowCoreThreadTimeOut(true);
    return executor;
  }

  /**
   * HTTP-interface client for the Keycloak {@code master} realm token endpoint (no bearer required).
   */
  @Bean
  public KeycloakTokenClient keycloakTokenClient() {
    return buildHttpServiceClient(RestClient.builder(), configuration.getTls(), configuration.getBaseUrl(),
      KeycloakTokenClient.class);
  }

  /**
   * Provider that acquires and caches the Keycloak admin access token, resolving the client secret lazily from
   * the secure store via {@link RealmConfigurationProvider}.
   */
  @Bean
  public KeycloakAdminTokenProvider keycloakAdminTokenProvider(KeycloakTokenClient keycloakTokenClient) {
    return new KeycloakAdminTokenProvider(keycloakTokenClient, configuration,
      () -> realmConfigurationProvider.getRealmConfiguration().getClientSecret());
  }

  /**
   * HTTP-interface client for the Keycloak Admin REST API. Every request carries a bearer token supplied by the
   * {@link KeycloakAdminTokenProvider} via a request interceptor.
   */
  @Bean
  public KeycloakAdminClient keycloakAdminClient(KeycloakAdminTokenProvider tokenProvider, ObjectMapper objectMapper) {
    var builder = RestClient.builder()
      .requestInterceptor((request, body, execution) -> {
        request.getHeaders().setBearerAuth(tokenProvider.getAccessToken());
        return execution.execute(request, body);
      })
      .messageConverters(converters -> {
        converters.removeIf(AbstractJacksonHttpMessageConverter.class::isInstance);
        converters.add(new JacksonJsonHttpMessageConverter(keycloakObjectMapper(objectMapper)));
      });
    return buildHttpServiceClient(builder, configuration.getTls(), configuration.getBaseUrl(),
      KeycloakAdminClient.class);
  }

  /**
   * Copy of the application's JSON mapper with a mix-in that ignores {@code ProtocolMapperRepresentation}'s
   * {@code consentRequired} field. That field is the only getter-only primitive across the Keycloak admin-API
   * representation DTOs, so Jackson must set it via field reflection — which fails under GraalVM native image
   * ({@code Can not set boolean field ... consentRequired}). The app never reads protocol-mapper consent, so
   * ignoring it is safe. All other mapper configuration (unknown-property tolerance, modules) is inherited.
   */
  private static JsonMapper keycloakObjectMapper(ObjectMapper objectMapper) {
    return ((JsonMapper) objectMapper).rebuild()
      .addMixIn(ProtocolMapperRepresentation.class, ConsentRequiredIgnoringMixin.class)
      .build();
  }

  // Public + registered for native reflection in RolesRuntimeHints so Jackson can read the mix-in annotation.
  @JsonIgnoreProperties("consentRequired")
  public abstract static class ConsentRequiredIgnoringMixin {
  }
}
