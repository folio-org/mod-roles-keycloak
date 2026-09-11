package org.folio.roles.integration.keyclock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.support.KeycloakUserUtils.KEYCLOAK_USER_ID;
import static org.folio.roles.support.RoleUtils.keycloakRole;
import static org.folio.roles.support.TestConstants.TENANT_ID;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.integration.keyclock.client.KeycloakAdminClient;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.mapper.KeycloakRoleMapper;
import org.folio.roles.support.TestUtils;
import org.folio.spring.FolioExecutionContext;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakRolesUserServiceTest {

  private static final UUID ROLE_ID_1 = UUID.randomUUID();
  private static final UUID ROLE_ID_2 = UUID.randomUUID();

  @InjectMocks private KeycloakRolesUserService service;

  @Mock private KeycloakAdminClient keycloakAdminClient;
  @Mock private FolioExecutionContext context;
  @Mock private KeycloakUserService userService;
  @Mock private KeycloakRoleMapper keycloakRoleMapper;

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

  private static HttpClientErrorException httpError(HttpStatus status) {
    return HttpClientErrorException.create(status, status.getReasonPhrase(), HttpHeaders.EMPTY, new byte[0],
      StandardCharsets.UTF_8);
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

      when(keycloakRoleMapper.toKeycloakRole(role1)).thenReturn(keycloakRole1);
      when(keycloakRoleMapper.toKeycloakRole(role2)).thenReturn(keycloakRole2);

      service.assignRolesToUser(USER_ID, roles);

      verify(keycloakAdminClient).addRealmRoleMappings(TENANT_ID, KEYCLOAK_USER_ID,
        List.of(keycloakRole1, keycloakRole2));
    }

    @Test
    void negative_unauthorizedException() {
      var role1 = role(ROLE_ID_1, "test-role-1");
      var role2 = role(ROLE_ID_2, "test-role-2");
      var keycloakRole1 = keycloakRole(role1);
      var keycloakRole2 = keycloakRole(role2);
      var roles = List.of(role1, role2);

      when(keycloakRoleMapper.toKeycloakRole(role1)).thenReturn(keycloakRole1);
      when(keycloakRoleMapper.toKeycloakRole(role2)).thenReturn(keycloakRole2);
      doThrow(httpError(UNAUTHORIZED)).when(keycloakAdminClient)
        .addRealmRoleMappings(TENANT_ID, KEYCLOAK_USER_ID, List.of(keycloakRole1, keycloakRole2));

      assertThatThrownBy(() -> service.assignRolesToUser(USER_ID, roles))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to assign roles to user: userId = %s", USER_ID);
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

      when(keycloakRoleMapper.toKeycloakRole(role1)).thenReturn(keycloakRole1);
      when(keycloakRoleMapper.toKeycloakRole(role2)).thenReturn(keycloakRole2);

      service.unlinkRolesFromUser(USER_ID, roles);

      verify(keycloakAdminClient).removeRealmRoleMappings(TENANT_ID, KEYCLOAK_USER_ID,
        List.of(keycloakRole1, keycloakRole2));
    }

    @Test
    void negative_unauthorizedException() {
      var role1 = role(ROLE_ID_1, "test-role-1");
      var role2 = role(ROLE_ID_2, "test-role-2");
      var keycloakRole1 = keycloakRole(role1);
      var keycloakRole2 = keycloakRole(role2);
      var roles = List.of(role1, role2);

      when(keycloakRoleMapper.toKeycloakRole(role1)).thenReturn(keycloakRole1);
      when(keycloakRoleMapper.toKeycloakRole(role2)).thenReturn(keycloakRole2);
      doThrow(httpError(UNAUTHORIZED)).when(keycloakAdminClient)
        .removeRealmRoleMappings(TENANT_ID, KEYCLOAK_USER_ID, List.of(keycloakRole1, keycloakRole2));

      assertThatThrownBy(() -> service.unlinkRolesFromUser(USER_ID, roles))
        .isInstanceOf(KeycloakApiException.class)
        .hasMessage("Failed to unlink roles from user: userId = %s", USER_ID);
    }
  }
}
