package org.folio.roles.support;

import static org.folio.roles.support.TestConstants.USER_ID;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.keycloak.representations.idm.UserRepresentation;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KeycloakUserUtils {

  public static final String KEYCLOAK_USER_ID = UUID.randomUUID().toString();

  public static UserRepresentation keycloakUser() {
    var userRepresentation = new UserRepresentation();
    userRepresentation.setUsername("test_username");
    userRepresentation.setEmail("test_email@example.dev");
    userRepresentation.setAttributes(Map.of("user_id", List.of(USER_ID.toString())));
    return userRepresentation;
  }
}
