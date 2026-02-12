package org.folio.roles;

import static java.util.stream.Collectors.toList;
import static javax.net.ssl.SSLContext.getInstance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.spring.integration.XOkapiHeaders.TENANT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

import java.net.Socket;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.spring.FolioModuleMetadata;
import org.folio.spring.scope.FolioExecutionContextSetter;
import org.keycloak.admin.client.Keycloak;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Log4j2
@Component
@RequiredArgsConstructor
public class KeycloakTestClient {

  private static final String KEYCLOAK_LOGIN_CLIENT_ID = "00000000-0000-0000-0000-000000000010";

  private final Keycloak keycloak;
  private final ObjectMapper objectMapper;
  private final FolioModuleMetadata folioModuleMetadata;
  private final KeycloakConfigurationProperties keycloakConfiguration;

  private final HttpClient httpClient = HttpClient.newBuilder()
    .sslContext(dummySslContext())
    .connectTimeout(Duration.ofSeconds(5))
    .version(Version.HTTP_1_1)
    .build();

  /**
   * Implementation is done using {@link HttpClient} client, cause looks like {@link org.keycloak.admin.client.Keycloak}
   * does not provide API for permissions querying.
   *
   * @return list of found permissions
   */
  @SneakyThrows
  public List<String> getPermissionNames() {
    var headers = Map.<String, Collection<String>>of(TENANT, List.of(TENANT_ID));
    try (var ignored = new FolioExecutionContextSetter(folioModuleMetadata, headers)) {
      var keycloakPermissions = findKeycloakPermissions();
      log.info("Found permissions: {}", keycloakPermissions);
      return keycloakPermissions;
    } catch (Exception exception) {
      throw new AssertionError("Failed to find keycloak permissions", exception);
    }
  }

  private List<String> findKeycloakPermissions() throws Exception {
    var request = keycloakPermissionsRequest();
    var response = httpClient.send(request, BodyHandlers.ofString());
    assertThat(response.statusCode()).isEqualTo(200);

    var body = response.body();
    var jsonNode = objectMapper.readTree(body);

    assertThat(jsonNode.isArray()).isTrue();
    return StreamSupport.stream(jsonNode.spliterator(), false)
      .map(permission -> permission.path("name").asText())
      .filter(Objects::nonNull)
      .collect(toList());
  }

  private HttpRequest keycloakPermissionsRequest() {
    var path = "/admin/realms/{realm}/clients/{clientId}/authz/resource-server/permission";
    var uri = fromUriString(keycloakConfiguration.getBaseUrl() + path)
      .queryParam("first", 0)
      .queryParam("last", 100)
      .buildAndExpand(Map.of("realm", TENANT_ID, "clientId", KEYCLOAK_LOGIN_CLIENT_ID))
      .encode().toUri();

    return HttpRequest.newBuilder(uri)
      .GET()
      .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
      .header(AUTHORIZATION, "Bearer " + keycloak.tokenManager().getAccessTokenString())
      .build();
  }

  @SneakyThrows
  private static SSLContext dummySslContext() {
    var dummyTrustManager = new X509ExtendedTrustManager() {

      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
        // used in tests, not implemented
      }

      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        // used in tests, not implemented
      }

      @Override
      public void checkClientTrusted(X509Certificate[] chain, String authType) {
        // used in tests, not implemented
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
        // used in tests, not implemented
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
        // used in tests, not implemented
      }

      @Override
      public void checkServerTrusted(X509Certificate[] chain, String authType) {
        // used in tests, not implemented
      }

      @Override
      public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
      }
    };

    var sslContext = getInstance("TLS");
    sslContext.init(null, new TrustManager[] {dummyTrustManager}, new SecureRandom());
    return sslContext;
  }
}
