package org.folio.roles.service.role;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.folio.roles.support.UserRoleTestUtils.userRole;
import static org.folio.roles.support.UserRoleTestUtils.userRoles;
import static org.folio.roles.support.UserRoleTestUtils.userRolesRequest;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.UserRole;
import org.folio.roles.integration.keyclock.KeycloakRolesUserService;
import org.folio.roles.integration.userskc.ModUsersKeycloakClient;
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
class UserRoleServiceTest {

  @Mock private RoleService roleService;
  @Mock private UserRoleEntityService userRoleEntityService;
  @Mock private KeycloakRolesUserService keycloakRolesUserService;
  @Mock private ModUsersKeycloakClient modUsersKeycloakClient;

  @InjectMocks private UserRoleService userRoleService;

  private static Role role() {
    return new Role().id(ROLE_ID).name("test role").description("test role description");
  }

  private static Role role(UUID id) {
    return new Role().id(id).name("test role: " + id).description("test role description");
  }

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(roleService, userRoleEntityService, keycloakRolesUserService);
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    void positive() {
      var request = userRolesRequest();
      var roleIds = List.of(ROLE_ID);
      var roles = List.of(role());

      when(userRoleEntityService.create(USER_ID, roleIds)).thenReturn(List.of(userRole()));
      when(roleService.findByIds(roleIds)).thenReturn(roles);

      var result = userRoleService.create(request);

      assertThat(result).isEqualTo(userRoles(userRole()));
      verify(keycloakRolesUserService).assignRolesToUser(USER_ID, roles);
    }
  }

  @Nested
  @DisplayName("createSafe")
  class CreateSafe {

    @Test
    void positive() {
      var request = new UserRole().userId(USER_ID).roleId(ROLE_ID);

      when(userRoleEntityService.find(request)).thenReturn(Optional.empty());
      when(roleService.getById(ROLE_ID)).thenReturn(role());

      userRoleService.createSafe(request);

      verify(keycloakRolesUserService).assignRolesToUser(USER_ID, List.of(role()));
      verify(userRoleEntityService).createSafe(request);
    }

    @Test
    void positive_userRoleRelationExists() {
      var request = new UserRole().userId(USER_ID).roleId(ROLE_ID);

      when(roleService.getById(ROLE_ID)).thenReturn(role());
      when(userRoleEntityService.find(request)).thenReturn(Optional.of(request));

      userRoleService.createSafe(request);

      verify(keycloakRolesUserService, never()).assignRolesToUser(USER_ID, List.of(role()));
      verify(userRoleEntityService, never()).createSafe(request);
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    void positive() {
      var roleId1 = randomUUID();
      var roleId2 = randomUUID();
      var roleId3 = randomUUID();
      var existingRoleId = randomUUID();

      var foundUserRoles = List.of(userRole(USER_ID, existingRoleId), userRole(USER_ID, roleId1));
      var rolesToAssign = List.of(role(roleId2), role(roleId3));
      var rolesToUnlink = List.of(role(existingRoleId));

      when(userRoleEntityService.findByUserId(USER_ID)).thenReturn(foundUserRoles);
      when(roleService.findByIds(List.of(roleId2, roleId3))).thenReturn(rolesToAssign);
      when(roleService.findByIds(List.of(existingRoleId))).thenReturn(rolesToUnlink);

      var rolesUserRequest = userRolesRequest(USER_ID, roleId1, roleId2, roleId3);
      userRoleService.update(rolesUserRequest);

      verify(userRoleEntityService).create(USER_ID, List.of(roleId2, roleId3));
      verify(userRoleEntityService).delete(USER_ID, List.of(existingRoleId));
      verify(keycloakRolesUserService).assignRolesToUser(USER_ID, rolesToAssign);
      verify(keycloakRolesUserService).unlinkRolesFromUser(USER_ID, rolesToUnlink);
    }

    @Test
    void positive_addNewPermissions() {
      var roleId1 = randomUUID();
      var roleId2 = randomUUID();

      var foundUserRoles = List.of(userRole(USER_ID, roleId1));
      var rolesToAssign = List.of(role(roleId2));

      when(userRoleEntityService.findByUserId(USER_ID)).thenReturn(foundUserRoles);
      when(roleService.findByIds(List.of(roleId2))).thenReturn(rolesToAssign);

      var rolesUserRequest = userRolesRequest(USER_ID, roleId1, roleId2);
      userRoleService.update(rolesUserRequest);

      verify(userRoleEntityService).create(USER_ID, List.of(roleId2));
      verify(keycloakRolesUserService).assignRolesToUser(USER_ID, rolesToAssign);
    }

    @Test
    void positive_removePermission() {
      var roleId1 = randomUUID();
      var roleId2 = randomUUID();

      var foundUserRoles = List.of(userRole(USER_ID, roleId1), userRole(USER_ID, roleId2));
      var rolesToUnlink = List.of(role(roleId1));

      when(userRoleEntityService.findByUserId(USER_ID)).thenReturn(foundUserRoles);
      when(roleService.findByIds(List.of(roleId1))).thenReturn(rolesToUnlink);

      var rolesUserRequest = userRolesRequest(USER_ID, roleId2);
      userRoleService.update(rolesUserRequest);

      verify(userRoleEntityService).delete(USER_ID, List.of(roleId1));
      verify(keycloakRolesUserService).unlinkRolesFromUser(USER_ID, rolesToUnlink);
    }

    @Test
    void negative_nothingToUpdate() {
      var existingUserRole = userRole(ROLE_ID);
      var rolesUserRequest = userRolesRequest(USER_ID, ROLE_ID);

      when(userRoleEntityService.findByUserId(USER_ID)).thenReturn(List.of(existingUserRole));
      userRoleService.update(rolesUserRequest);

      verifyNoInteractions(keycloakRolesUserService);
    }
  }

  @Nested
  @DisplayName("findById")
  class FindById {

    @Test
    void positive() {
      userRoleService.findById(USER_ID);

      verify(userRoleEntityService).findByUserId(USER_ID);
    }
  }

  @Nested
  @DisplayName("findByQuery")
  class FindByQuery {

    @Test
    void positive() {
      var userRoles = userRoles(List.of(userRole()));
      var query = "cql.allRecords = 1";
      var offset = 0;
      var limit = 1;

      when(userRoleEntityService.findByQuery(query, offset, limit)).thenReturn(userRoles);

      var result = userRoleService.findByQuery(query, offset, limit);

      assertThat(result).isEqualTo(userRoles);
    }
  }

  @Nested
  @DisplayName("deleteById")
  class DeleteById {

    @Test
    void positive() {
      var roles = List.of(role());
      when(userRoleEntityService.findByUserId(USER_ID)).thenReturn(List.of(userRole()));
      when(roleService.findByIds(List.of(ROLE_ID))).thenReturn(roles);

      userRoleService.deleteById(USER_ID);

      verify(keycloakRolesUserService).unlinkRolesFromUser(USER_ID, roles);
      verify(userRoleEntityService).deleteByUserId(USER_ID);
    }
  }
}
