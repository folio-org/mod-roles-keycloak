package org.folio.roles.support;

import static org.folio.roles.support.TestConstants.USER_ID;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.folio.roles.integration.keyclock.model.KeycloakUser;

@UtilityClass
public class KeycloakUtils {

  public static final UUID KEYCLOAK_USER_ID = UUID.randomUUID();
  public static final String ACCESS_TOKEN = "a2V5Y2xvYWtfYWNjZXNzX3Rva2Vu";

  public static KeycloakUser keycloakUser() {
    var keycloakUser = new KeycloakUser();
    keycloakUser.setId(KEYCLOAK_USER_ID);
    keycloakUser.setUserName("test-user");
    keycloakUser.setAttributes(Map.of("user_id", List.of(USER_ID.toString())));
    return keycloakUser;
  }
}
