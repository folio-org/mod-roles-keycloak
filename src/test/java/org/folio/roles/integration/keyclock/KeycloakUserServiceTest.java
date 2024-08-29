package org.folio.roles.integration.keyclock;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.support.KeycloakUserUtils.KEYCLOAK_USER_ID;
import static org.folio.test.TestConstants.TENANT_ID;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.folio.roles.support.TestUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakUserServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();

  @Mock private Keycloak keycloak;
  @Mock(answer = RETURNS_DEEP_STUBS) private RealmResource realmResource;
  @Mock private FolioExecutionContext context;

  @InjectMocks private KeycloakUserService service;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  private static UserRepresentation keycloakUser() {
    return keycloakUser(KEYCLOAK_USER_ID);
  }

  private static UserRepresentation keycloakUser(String id) {
    var userRepresentation = new UserRepresentation();
    userRepresentation.setId(id);
    userRepresentation.setUsername("test_username");
    userRepresentation.setEmail("test_email@test.dev");
    userRepresentation.setEnabled(true);
    userRepresentation.setAttributes(Map.of("user_id", List.of(USER_ID.toString())));
    return userRepresentation;
  }

  @Nested
  @DisplayName("findKeycloakIdByUserId")
  class FindKeycloakIdByUserId {

    @Test
    void positive() {
      var query = "user_id:" + USER_ID;
      var foundKeycloakUsers = List.of(keycloakUser());

      when(context.getTenantId()).thenReturn(TENANT_ID);
      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.users().searchByAttributes(null, null, null, false, query)).thenReturn(foundKeycloakUsers);

      var result = service.findKeycloakIdByUserId(USER_ID);

      assertThat(result).isEqualTo(KEYCLOAK_USER_ID);
      verify(realmResource, atLeastOnce()).users();
    }

    @Test
    void negative_userNotFoundByAttributes() {
      var query = "user_id:" + USER_ID;

      when(context.getTenantId()).thenReturn(TENANT_ID);
      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.users().searchByAttributes(null, null, null, false, query)).thenReturn(emptyList());

      assertThatThrownBy(() -> service.findKeycloakIdByUserId(USER_ID))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessage("Keycloak user doesn't exist with the given 'user_id' attribute: %s", USER_ID);

      verify(realmResource, atLeastOnce()).users();
    }

    @Test
    void negative_throwExceptionWhenMultipleUsersFound() {
      var query = "user_id:" + USER_ID;
      var userId1 = UUID.randomUUID().toString();
      var userId2 = UUID.randomUUID().toString();
      var foundUsers = List.of(keycloakUser(userId1), keycloakUser(userId2));

      when(context.getTenantId()).thenReturn(TENANT_ID);
      when(context.getTenantId()).thenReturn(TENANT_ID);
      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(realmResource.users().searchByAttributes(null, null, null, false, query)).thenReturn(foundUsers);

      assertThatThrownBy(() -> service.findKeycloakIdByUserId(USER_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Too many keycloak users with 'user_id' attribute: %s", USER_ID);

      verify(realmResource, atLeastOnce()).users();
    }
  }
}
