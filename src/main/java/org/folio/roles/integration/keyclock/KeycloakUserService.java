package org.folio.roles.integration.keyclock;

import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import jakarta.persistence.EntityNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.folio.spring.FolioExecutionContext;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class KeycloakUserService {

  public static final String USER_ID_ATTR = "user_id";

  private final Keycloak keycloak;
  private final FolioExecutionContext context;

  @Cacheable(cacheNames = "keycloak-user-id", key = "#userId")
  public String findKeycloakIdByUserId(UUID userId) {
    return getKeycloakUserByUserId(userId).getId();
  }

  @Cacheable(cacheNames = "keycloak-users", key = "#userId")
  public UserRepresentation getKeycloakUserByUserId(UUID userId) {
    var query = USER_ID_ATTR + ":" + userId;
    var realmResource = keycloak.realm(context.getTenantId());
    var users = realmResource.users().searchByAttributes(null, null, null, false, query);

    if (isEmpty(users)) {
      throw new EntityNotFoundException("Keycloak user doesn't exist with the given 'user_id' attribute: " + userId);
    }

    if (users.size() != 1) {
      throw new IllegalStateException("Too many keycloak users with 'user_id' attribute: " + userId);
    }

    return users.get(0);
  }

  /**
   * Retrieves folio user identifier from {@link UserRepresentation} object.
   *
   * @param userRepresentation - {@link UserRepresentation} object to process
   * @return found folio user identifier
   * @throws IllegalStateException if user id attribute is not found or value contains more than 1 object
   * @throws IllegalArgumentException if user_id identifier failed to parse to {@link UUID}
   */
  public static UUID getUserId(UserRepresentation userRepresentation) {
    var attributes = userRepresentation.getAttributes();
    var attrs = MapUtils.emptyIfNull(attributes);

    var values = attrs.get(USER_ID_ATTR);
    if (isEmpty(values)) {
      throw new IllegalStateException("User id attribute is not found");
    } else if (values.size() != 1) {
      throw new IllegalStateException("User id attribute contains too many values");
    }

    return UUID.fromString(values.get(0));
  }
}
