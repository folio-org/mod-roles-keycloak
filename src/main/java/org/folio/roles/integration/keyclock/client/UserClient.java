package org.folio.roles.integration.keyclock.client;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.List;
import org.folio.roles.integration.keyclock.configuration.FeignConfiguration;
import org.folio.roles.integration.keyclock.model.KeycloakUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "keycloak-user-client",
  url = "#{keycloakConfigurationProperties.getBaseUrl()}",
  configuration = FeignConfiguration.class)
public interface UserClient {

  @GetMapping(value = "/admin/realms/{realm}/users?q={attrQuery}&briefRepresentation={brief}",
    produces = APPLICATION_JSON_VALUE)
  List<KeycloakUser> findUsersWithAttrs(@RequestHeader(AUTHORIZATION) String token,
                                        @PathVariable("realm") String realmName,
                                        @PathVariable("attrQuery") String attrQuery,
                                        @PathVariable("brief") boolean briefRepresentation);
}
