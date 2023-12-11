package org.folio.roles.integration.keyclock;

import static org.folio.roles.support.KeycloakUtils.ACCESS_TOKEN;
import static org.folio.test.TestConstants.TENANT_ID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import feign.FeignException;
import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.integration.keyclock.client.RolesUsersClient;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.roles.mapper.KeycloakRolesUserMapper;
import org.folio.roles.mapper.KeycloakRolesUserMapperImpl;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class KeycloakRolesUserServiceTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID KEYCLOAK_USER_ID = UUID.randomUUID();
  private static final UUID ROLE_ID_1 = UUID.randomUUID();
  private static final UUID ROLE_ID_2 = UUID.randomUUID();

  @Mock private RolesUsersClient rolesUserClient;
  @Mock private FolioExecutionContext context;
  @Mock private KeycloakAccessTokenService tokenService;
  @Mock private KeycloakUserService userService;
  @Spy private KeycloakRolesUserMapper mapper = new KeycloakRolesUserMapperImpl();

  @InjectMocks private KeycloakRolesUserService service;

  @BeforeEach
  void beforeEach() {
    when(context.getTenantId()).thenReturn(TENANT_ID);
    when(tokenService.getToken()).thenReturn(ACCESS_TOKEN);
    when(userService.findKeycloakIdByUserId(USER_ID)).thenReturn(KEYCLOAK_USER_ID);
  }

  @AfterEach
  void afterEach() {
    verifyNoMoreInteractions(context, tokenService, userService);
  }

  private static Role role(UUID id, String name) {
    return new Role().id(id).name(name).description("test role description");
  }

  @Nested
  @DisplayName("assignRolesToUser")
  class AssignRolesToUser {

    @Test
    void positive() {
      var roles = List.of(role(ROLE_ID_1, "test-role-1"), role(ROLE_ID_2, "test-role-2"));

      service.assignRolesToUser(USER_ID, roles);

      var keycloakRoles = mapper.toKeycloakDto(roles);
      verify(rolesUserClient).assignRolesToUser(ACCESS_TOKEN, TENANT_ID, KEYCLOAK_USER_ID, keycloakRoles);
    }

    @Test
    void negative_throwsFeignException() {
      var roles = List.of(role(ROLE_ID_1, "test-role-1"), role(ROLE_ID_2, "test-role-2"));
      var keycloakRoles = mapper.toKeycloakDto(roles);
      doThrow(FeignException.FeignClientException.class).when(rolesUserClient)
        .assignRolesToUser(ACCESS_TOKEN, TENANT_ID, KEYCLOAK_USER_ID, keycloakRoles);
      assertThrows(KeycloakApiException.class, () -> service.assignRolesToUser(USER_ID, roles));
    }
  }

  @Nested
  @DisplayName("unlinkRolesFromUser")
  class UnlinkRolesFromUser {

    @Test
    void positive() {
      var roles = List.of(role(ROLE_ID_1, "test-role-1"), role(ROLE_ID_2, "test-role-2"));

      service.unlinkRolesFromUser(USER_ID, roles);

      var keycloakUsers = mapper.toKeycloakDto(roles);
      verify(rolesUserClient).unlinkRolesFromUser(ACCESS_TOKEN, TENANT_ID, KEYCLOAK_USER_ID, keycloakUsers);
    }

    @Test
    void negative_throwsFeignException() {
      var roles = List.of(role(ROLE_ID_1, "test-role-1"), role(ROLE_ID_2, "test-role-2"));
      var keycloakRoles = mapper.toKeycloakDto(roles);
      doThrow(FeignException.FeignClientException.class).when(rolesUserClient)
        .unlinkRolesFromUser(ACCESS_TOKEN, TENANT_ID, KEYCLOAK_USER_ID, keycloakRoles);

      assertThrows(KeycloakApiException.class, () -> service.unlinkRolesFromUser(USER_ID, roles));
    }
  }
}
