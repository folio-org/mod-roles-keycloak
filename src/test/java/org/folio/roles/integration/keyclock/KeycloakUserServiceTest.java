package org.folio.roles.integration.keyclock;

import static java.util.Collections.emptyList;
import static java.util.UUID.fromString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.integration.keyclock.model.KeycloakUser.USER_ID_ATTR;
import static org.folio.roles.support.KeycloakUtils.ACCESS_TOKEN;
import static org.folio.test.TestConstants.TENANT_ID;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.folio.roles.integration.keyclock.client.UserClient;
import org.folio.roles.integration.keyclock.model.KeycloakUser;
import org.folio.roles.support.TestUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakUserServiceTest {

  private static final UUID USER_ID = fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID KEYCLOAK_USER_ID = fromString("00000000-0000-0000-0000-000000000002");

  @Mock private UserClient client;
  @Mock private KeycloakAccessTokenService tokenService;
  @Mock private FolioExecutionContext context;

  @InjectMocks private KeycloakUserService service;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Nested
  @DisplayName("findKeycloakIdByUserId")
  class FindKeycloakIdByUserId {

    @Test
    void positive() {
      var expectedKeycloakUser = new KeycloakUser();
      expectedKeycloakUser.setId(KEYCLOAK_USER_ID);
      var query = USER_ID_ATTR + ":" + USER_ID;

      when(context.getTenantId()).thenReturn(TENANT_ID);
      when(tokenService.getToken()).thenReturn(ACCESS_TOKEN);
      when(client.findUsersWithAttrs(ACCESS_TOKEN, TENANT_ID, query, false)).thenReturn(List.of(expectedKeycloakUser));

      var result = service.findKeycloakIdByUserId(USER_ID);

      assertThat(result).isEqualTo(KEYCLOAK_USER_ID);
    }

    @Test
    void negative_throwExceptions() {
      var expectedKeycloakUser = new KeycloakUser();
      expectedKeycloakUser.setId(USER_ID);
      var query = USER_ID_ATTR + ":" + USER_ID;

      when(context.getTenantId()).thenReturn(TENANT_ID);
      when(tokenService.getToken()).thenReturn(ACCESS_TOKEN);
      when(client.findUsersWithAttrs(ACCESS_TOKEN, TENANT_ID, query, false)).thenReturn(emptyList());

      assertThatThrownBy(() -> service.findKeycloakIdByUserId(USER_ID))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Keycloak user doesn't exist with the given 'user_id' attribute: %s", USER_ID);
    }

    @Test
    void negative_throwExceptionWhenMultipleUsersFound() {
      var expectedKeycloakUser = new KeycloakUser();
      expectedKeycloakUser.setId(USER_ID);
      var query = USER_ID_ATTR + ":" + USER_ID;
      var foundUsers = List.of(expectedKeycloakUser, expectedKeycloakUser);

      when(context.getTenantId()).thenReturn(TENANT_ID);
      when(tokenService.getToken()).thenReturn(ACCESS_TOKEN);
      when(client.findUsersWithAttrs(ACCESS_TOKEN, TENANT_ID, query, false)).thenReturn(foundUsers);

      assertThatThrownBy(() -> service.findKeycloakIdByUserId(USER_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Too many keycloak users with 'user_id' attribute: %s", USER_ID);
    }
  }
}
