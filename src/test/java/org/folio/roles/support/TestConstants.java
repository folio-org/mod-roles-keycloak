package org.folio.roles.support;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestConstants {

  public static final String MODULE_NAME = "mod-roles-keycloak";
  public static final String OKAPI_AUTH_TOKEN = "X-Okapi-Token test value";
  public static final String TENANT_ID = "test";
  public static final String USER_ID_HEADER = "3d05b34c-1111-2222-3333-e90770b06a26";
  public static final String LOGIN_CLIENT_SUFFIX = "-login-application";
  public static final String KC_TOKEN_CACHE = "keycloak-access-token";
  public static final String KC_CLIENT_ID_CACHE = "keycloak-client-id";
  public static final String KC_USER_ID_CACHE = "keycloak-user-id";

  public static final UUID USER_ID = UUID.randomUUID();
}
