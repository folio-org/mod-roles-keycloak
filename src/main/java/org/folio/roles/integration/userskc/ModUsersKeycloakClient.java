package org.folio.roles.integration.userskc;

import org.folio.spring.config.FeignClientConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "moduserskc", url = "${application.moduserskc.url}", configuration = FeignClientConfiguration.class)
public interface ModUsersKeycloakClient {

  /**
   * Create keycloak user if not exists.
   *
   * @param userId folio user ID, UUID.
   */
  @PostMapping("/users-keycloak/auth-users/{userId}")
  void ensureKeycloakUserExists(@PathVariable("userId") String userId);
}
