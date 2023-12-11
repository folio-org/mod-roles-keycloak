package org.folio.roles.integration.keyclock;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.integration.keyclock.configuration.KeycloakConfigurationProperties;
import org.folio.spring.FolioExecutionContext;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthorizationResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KeycloakAuthorizationClientProvider {

  private final Keycloak keycloak;
  private final KeycloakConfigurationProperties keycloakConfigurationProperties;
  private final FolioExecutionContext context;
  private final KeycloakClientService clientService;

  public AuthorizationResource getAuthorizationClient() {
    var tenantId = context.getTenantId();
    var client = clientService.getLoginClient();
    return keycloak.proxy(AuthorizationResource.class, authorizationResourceUri(client, tenantId));
  }

  private URI authorizationResourceUri(ClientRepresentation client, String realmName) {
    return URI.create(String.format("%s/admin/realms/%s/clients/%s/authz/resource-server",
      keycloakConfigurationProperties.getBaseUrl(), realmName, client.getId()));
  }
}
