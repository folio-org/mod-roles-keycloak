package org.folio.roles.integration.keyclock;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.folio.roles.integration.keyclock.model.KeycloakUser.USER_ID_ATTR;

import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.folio.roles.integration.keyclock.client.UserClient;
import org.folio.roles.integration.keyclock.model.KeycloakUser;
import org.folio.spring.FolioExecutionContext;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KeycloakUserService {

  private final UserClient client;
  private final KeycloakAccessTokenService tokenService;
  private final FolioExecutionContext context;

  @Cacheable(cacheNames = "keycloak-user-id", key = "#userId")
  public UUID findKeycloakIdByUserId(UUID userId) {
    return getKeycloakUserByUserId(userId).getId();
  }

  @Cacheable(cacheNames = "keycloak-users", key = "#userId")
  public KeycloakUser getKeycloakUserByUserId(UUID userId) {
    var query = USER_ID_ATTR + ":" + userId;

    var found = client.findUsersWithAttrs(tokenService.getToken(), context.getTenantId(), query, false);

    if (isEmpty(found)) {
      throw new EntityNotFoundException("Keycloak user doesn't exist with the given 'user_id' attribute: " + userId);
    }

    if (found.size() != 1) {
      throw new IllegalStateException("Too many keycloak users with 'user_id' attribute: " + userId);
    }

    return found.get(0);
  }
}
