package org.folio.roles.integration.keyclock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.support.KeycloakUserUtils.KEYCLOAK_USER_ID;
import static org.folio.roles.support.RoleUtils.keycloakRole;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.folio.test.TestConstants.TENANT_ID;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.NotAuthorizedException;
import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.mapper.KeycloakRoleMapper;
import org.folio.roles.support.TestUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.jboss.resteasy.core.Headers;
import org.jboss.resteasy.core.ServerResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakRolesUserServiceTest {

  private static final UUID ROLE_ID_1 = UUID.randomUUID();
  private static final UUID ROLE_ID_2 = UUID.randomUUID();

  @InjectMocks private KeycloakRolesUserService service;

  @Mock private Keycloak keycloak;
  @Mock private KeycloakRoleMapper keycloakRoleMapper;
  @Mock private KeycloakUserService userService;
  @Mock private FolioExecutionContext context;

  @Mock private RoleScopeResource roleScopeResource;
  @Mock(answer = RETURNS_DEEP_STUBS) private RealmResource realmResource;

  @BeforeEach
  void beforeEach() {
    when(context.getTenantId()).thenReturn(TENANT_ID);
    when(userService.findKeycloakIdByUserId(USER_ID)).thenReturn(KEYCLOAK_USER_ID);
  }

  @AfterEach
  void afterEach() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  private static Role role(UUID id, String name) {
    return new Role().id(id).name(name).description("test role description");
  }

  @Nested
  @DisplayName("assignRolesToUser")
  class AssignRolesToUser {

    @Test
    void positive() {
      var role1 = role(ROLE_ID_1, "test-role-1");
      var role2 = role(ROLE_ID_2, "test-role-2");
      var keycloakRole1 = keycloakRole(role1);
      var keycloakRole2 = keycloakRole(role2);
      var roles = List.of(role1, role2);

      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(keycloakRoleMapper.toKeycloakRole(role1)).thenReturn(keycloakRole1);
      when(keycloakRoleMapper.toKeycloakRole(role2)).thenReturn(keycloakRole2);
      when(realmResource.users().get(KEYCLOAK_USER_ID).roles().realmLevel()).thenReturn(roleScopeResource);

      service.assignRolesToUser(USER_ID, roles);

      verify(realmResource, atLeastOnce()).users();
      verify(roleScopeResource).add(List.of(keycloakRole1, keycloakRole2));
    }

    @Test
    void negative_unauthorizedException() {
      var role1 = role(ROLE_ID_1, "test-role-1");
      var role2 = role(ROLE_ID_2, "test-role-2");
      var keycloakRole1 = keycloakRole(role1);
      var keycloakRole2 = keycloakRole(role2);
      var roles = List.of(role1, role2);

      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(keycloakRoleMapper.toKeycloakRole(role1)).thenReturn(keycloakRole1);
      when(keycloakRoleMapper.toKeycloakRole(role2)).thenReturn(keycloakRole2);
      when(realmResource.users().get(KEYCLOAK_USER_ID).roles().realmLevel()).thenReturn(roleScopeResource);

      var exception = new NotAuthorizedException(new ServerResponse(null, 401, new Headers<>()));
      doThrow(exception).when(roleScopeResource).add(List.of(keycloakRole1, keycloakRole2));

      assertThatThrownBy(() -> service.assignRolesToUser(USER_ID, roles))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to assign roles to user: userId = %s", USER_ID);

      verify(realmResource, atLeastOnce()).users();
    }
  }

  @Nested
  @DisplayName("unlinkRolesFromUser")
  class UnlinkRolesFromUser {

    @Test
    void positive() {
      var role1 = role(ROLE_ID_1, "test-role-1");
      var role2 = role(ROLE_ID_2, "test-role-2");
      var keycloakRole1 = keycloakRole(role1);
      var keycloakRole2 = keycloakRole(role2);
      var roles = List.of(role1, role2);

      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(keycloakRoleMapper.toKeycloakRole(role1)).thenReturn(keycloakRole1);
      when(keycloakRoleMapper.toKeycloakRole(role2)).thenReturn(keycloakRole2);
      when(realmResource.users().get(KEYCLOAK_USER_ID).roles().realmLevel()).thenReturn(roleScopeResource);

      service.unlinkRolesFromUser(USER_ID, roles);

      verify(realmResource, atLeastOnce()).users();
      verify(roleScopeResource).remove(List.of(keycloakRole1, keycloakRole2));
    }

    @Test
    void negative_unauthorizedException() {
      var role1 = role(ROLE_ID_1, "test-role-1");
      var role2 = role(ROLE_ID_2, "test-role-2");
      var keycloakRole1 = keycloakRole(role1);
      var keycloakRole2 = keycloakRole(role2);
      var roles = List.of(role1, role2);

      when(keycloak.realm(TENANT_ID)).thenReturn(realmResource);
      when(keycloakRoleMapper.toKeycloakRole(role1)).thenReturn(keycloakRole1);
      when(keycloakRoleMapper.toKeycloakRole(role2)).thenReturn(keycloakRole2);
      when(realmResource.users().get(KEYCLOAK_USER_ID).roles().realmLevel()).thenReturn(roleScopeResource);

      var exception = new NotAuthorizedException(new ServerResponse(null, 401, new Headers<>()));
      doThrow(exception).when(roleScopeResource).remove(List.of(keycloakRole1, keycloakRole2));

      assertThatThrownBy(() -> service.unlinkRolesFromUser(USER_ID, roles))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to unlink roles from user: userId = %s", USER_ID);

      verify(realmResource, atLeastOnce()).users();
    }
  }
}
