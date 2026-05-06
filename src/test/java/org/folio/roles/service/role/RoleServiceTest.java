package org.folio.roles.service.role;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.domain.dto.HttpMethod.GET;
import static org.folio.roles.domain.dto.RoleType.CONSORTIUM;
import static org.folio.roles.domain.dto.RoleType.DEFAULT;
import static org.folio.roles.domain.dto.RoleType.REGULAR;
import static org.folio.roles.support.EndpointUtils.endpoint;
import static org.folio.roles.support.PolicyUtils.rolePolicy;
import static org.folio.roles.support.RoleUtils.ROLE_DESCRIPTION;
import static org.folio.roles.support.RoleUtils.ROLE_DESCRIPTION_2;
import static org.folio.roles.support.RoleUtils.ROLE_DESCRIPTION_3;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.RoleUtils.ROLE_ID_2;
import static org.folio.roles.support.RoleUtils.ROLE_ID_3;
import static org.folio.roles.support.RoleUtils.ROLE_NAME;
import static org.folio.roles.support.RoleUtils.ROLE_NAME_2;
import static org.folio.roles.support.RoleUtils.ROLE_NAME_3;
import static org.folio.roles.support.RoleUtils.consortiumRole;
import static org.folio.roles.support.RoleUtils.defaultRole;
import static org.folio.roles.support.RoleUtils.role;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.folio.roles.domain.dto.Endpoint;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.UserRole;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.integration.keyclock.KeycloakAuthorizationService;
import org.folio.roles.integration.keyclock.KeycloakPolicyService;
import org.folio.roles.integration.keyclock.KeycloakRoleService;
import org.folio.roles.integration.keyclock.KeycloakRolesUserService;
import org.folio.roles.service.capability.CapabilityEndpointService;
import org.folio.roles.service.policy.PolicyEntityService;
import org.folio.roles.service.policy.PolicyService;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

  @Mock private KeycloakRoleService keycloakService;
  @Mock private KeycloakRolesUserService keycloakRolesUserService;
  @Mock private KeycloakAuthorizationService keycloakAuthService;
  @Mock private KeycloakPolicyService keycloakPolicyService;
  @Mock private RoleEntityService entityService;
  @Mock private UserRoleEntityService userRoleEntityService;
  @Mock private PolicyEntityService policyEntityService;
  @Mock private PolicyService policyService;
  @Mock private CapabilityEndpointService capabilityEndpointService;

  @Captor private ArgumentCaptor<Function<Endpoint, String>> nameGeneratorCaptor;

  @InjectMocks private RoleService facade;

  @AfterEach
  void tearDown() {
    verifyNoMoreInteractions(keycloakRolesUserService, keycloakAuthService, keycloakPolicyService,
      userRoleEntityService, policyEntityService, policyService, capabilityEndpointService);
  }

  @Nested
  @DisplayName("getById")
  class GetById {

    @Test
    void positive() {
      var role = role();
      when(entityService.getById(ROLE_ID)).thenReturn(role);

      var result = facade.getById(ROLE_ID);

      assertEquals(ROLE_ID, result.getId());
      assertEquals(ROLE_NAME, result.getName());
      verifyNoMoreInteractions(entityService);
    }
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    void positive_singleRole() {
      var role = role();
      when(keycloakService.create(role)).thenReturn(role);
      when(entityService.create(role)).thenReturn(role);

      facade.create(role);

      verify(keycloakService).create(role);
      verify(entityService).create(role);
    }

    @Test
    void negative_dbCreateFails_rollbackKeycloak() {
      var role = role();
      when(keycloakService.create(role)).thenReturn(role);
      when(entityService.create(role)).thenThrow(RuntimeException.class);

      assertThrows(ServiceException.class, () -> facade.create(role));
      verify(keycloakService).deleteById(role.getId());
    }

    @Test
    void positive_multipleRoles() {
      var role1 = role();
      var role2 = role().id(ROLE_ID_2).name(ROLE_NAME_2);

      when(keycloakService.create(role1)).thenReturn(role1);
      when(entityService.create(role1)).thenReturn(role1);
      when(keycloakService.create(role2)).thenReturn(role2);
      when(entityService.create(role2)).thenReturn(role2);

      var result = facade.create(List.of(role1, role2));

      assertThat(result.getRoles()).hasSize(2);
      assertThat(result.getTotalRecords()).isEqualTo(2);
      verify(keycloakService, times(2)).create(any());
      verify(entityService, times(2)).create(any());
    }

    @Test
    void negative_cannotCreateDefaultRole() {
      var defaultRole = defaultRole();

      assertThatThrownBy(() -> facade.create(defaultRole))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Default role cannot be created, updated or deleted via roles API.");

      verifyNoInteractions(keycloakService);
      verifyNoInteractions(entityService);
    }

    @Test
    void createRole_withNullDescription_positive() {
      var role = role();
      role.setDescription(null);
      when(keycloakService.create(role)).thenReturn(role);
      when(entityService.create(role)).thenReturn(role);

      facade.create(role);

      verify(keycloakService).create(role);
      verify(entityService).create(role);
    }

    @Test
    void createRole_withEmptyDescription_positive() {
      var role = role();
      role.setDescription("");
      when(keycloakService.create(role)).thenReturn(role);
      when(entityService.create(role)).thenReturn(role);

      facade.create(role);

      verify(keycloakService).create(role);
      verify(entityService).create(role);
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    void positive() {
      var role = role();

      when(entityService.getById(ROLE_ID)).thenReturn(role);

      facade.update(role);

      verify(entityService).update(role);
      verify(keycloakService).update(role);
    }

    @Test
    void negative_roleIdIsNull() {
      var role = role();
      role.setId(null);

      assertThatThrownBy(() -> facade.update(role))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Role should has ID");

      verifyNoInteractions(entityService);
      verifyNoInteractions(keycloakService);
    }

    @Test
    void negative_repositoryThrowsException() {
      var role = role();

      when(entityService.getById(any())).thenReturn(role);
      when(entityService.update(role)).thenThrow(RuntimeException.class);

      assertThrows(RuntimeException.class, () -> facade.update(role));
      verify(keycloakService, times(2)).update(any());
    }

    @Test
    void negative_cannotChangeRoleTypeFromDefaultToRegular() {
      var existingRole = defaultRole();
      var updatedRole = new Role()
        .id(ROLE_ID_3)
        .name(ROLE_NAME_3)
        .description(ROLE_DESCRIPTION_3)
        .type(REGULAR);

      when(entityService.getById(ROLE_ID_3)).thenReturn(existingRole);

      assertThatThrownBy(() -> facade.update(updatedRole))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Default role cannot be created, updated or deleted via roles API.");

      verifyNoInteractions(keycloakService);
      verify(entityService).getById(ROLE_ID_3);
      verify(entityService, times(0)).update(any());
    }

    @Test
    void negative_cannotChangeRoleTypeFromDefaultToConsortium() {
      var existingRole = defaultRole();
      var updatedRole = new Role()
        .id(ROLE_ID_3)
        .name(ROLE_NAME_3)
        .description(ROLE_DESCRIPTION_3)
        .type(CONSORTIUM);

      when(entityService.getById(ROLE_ID_3)).thenReturn(existingRole);

      assertThatThrownBy(() -> facade.update(updatedRole))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Default role cannot be created, updated or deleted via roles API.");

      verifyNoInteractions(keycloakService);
      verify(entityService).getById(ROLE_ID_3);
      verify(entityService, times(0)).update(any());
    }

    @Test
    void negative_cannotChangeRoleTypeFromRegularToDefault() {
      var existingRole = role();
      var updatedRole = new Role()
        .id(ROLE_ID)
        .name(ROLE_NAME)
        .description(ROLE_DESCRIPTION)
        .type(DEFAULT);

      when(entityService.getById(ROLE_ID)).thenReturn(existingRole);

      assertThatThrownBy(() -> facade.update(updatedRole))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Default role cannot be created, updated or deleted via roles API.");

      verifyNoInteractions(keycloakService);
    }

    @Test
    void negative_cannotChangeRoleTypeFromConsortiumToDefault() {
      var existingRole = consortiumRole();
      var updatedRole = new Role()
        .id(ROLE_ID_2)
        .name(ROLE_NAME_2)
        .description(ROLE_DESCRIPTION_2)
        .type(DEFAULT);

      when(entityService.getById(ROLE_ID_2)).thenReturn(existingRole);

      assertThatThrownBy(() -> facade.update(updatedRole))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Default role cannot be created, updated or deleted via roles API.");

      verifyNoInteractions(keycloakService);
    }

    @Test
    void positive_canChangeRoleTypeFromRegularToConsortium() {
      var existingRole = role();
      var updatedRole = new Role()
        .id(ROLE_ID)
        .name(ROLE_NAME)
        .description(ROLE_DESCRIPTION)
        .type(CONSORTIUM);

      when(entityService.getById(ROLE_ID)).thenReturn(existingRole);

      facade.update(updatedRole);

      verify(keycloakService).update(updatedRole);
      verify(entityService).update(updatedRole);
    }

    @Test
    void positive_canChangeRoleTypeFromConsortiumToRegular() {
      var existingRole = consortiumRole();
      var updatedRole = new Role()
        .id(ROLE_ID_2)
        .name(ROLE_NAME_2)
        .description(ROLE_DESCRIPTION_2)
        .type(REGULAR);

      when(entityService.getById(ROLE_ID_2)).thenReturn(existingRole);

      facade.update(updatedRole);

      verify(keycloakService).update(updatedRole);
      verify(entityService).update(updatedRole);
    }

    @Test
    void positive_updateWithoutTypeChange() {
      var existingRole = role();
      var updatedRole = new Role()
        .id(ROLE_ID)
        .name("Updated Name")
        .description("Updated Description")
        .type(REGULAR);

      when(entityService.getById(ROLE_ID)).thenReturn(existingRole);

      facade.update(updatedRole);

      verify(keycloakService).update(updatedRole);
      verify(entityService).update(updatedRole);
    }

    @Test
    void positive_updateConsortiumRoleWithoutTypeChange() {
      // Validates that CONSORTIUM roles can be updated when type doesn't change
      var existingRole = consortiumRole();
      var updatedRole = new Role()
        .id(ROLE_ID_2)
        .name("Updated Consortium Role")
        .description("Updated Description")
        .type(CONSORTIUM);

      when(entityService.getById(ROLE_ID_2)).thenReturn(existingRole);

      facade.update(updatedRole);

      verify(keycloakService).update(updatedRole);
      verify(entityService).update(updatedRole);
    }

    @Test
    void negative_cannotChangeTypeFromRegularToDefaultWithDifferentFields() {
      // Validates that even when changing other fields, type cannot be changed to DEFAULT
      var existingRole = role();
      var updatedRole = new Role()
        .id(ROLE_ID)
        .name("Completely New Name")
        .description("Completely New Description")
        .type(DEFAULT);

      when(entityService.getById(ROLE_ID)).thenReturn(existingRole);

      assertThatThrownBy(() -> facade.update(updatedRole))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Default role cannot be created, updated or deleted via roles API.");

      verifyNoInteractions(keycloakService);
    }

    @Test
    void negative_cannotChangeTypeFromConsortiumToDefaultWithDifferentFields() {
      // Validates that CONSORTIUM roles also cannot be changed to DEFAULT
      var existingRole = consortiumRole();
      var updatedRole = new Role()
        .id(ROLE_ID_2)
        .name("Completely New Name")
        .description("Completely New Description")
        .type(DEFAULT);

      when(entityService.getById(ROLE_ID_2)).thenReturn(existingRole);

      assertThatThrownBy(() -> facade.update(updatedRole))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Default role cannot be created, updated or deleted via roles API.");

      verifyNoInteractions(keycloakService);
    }
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    void positive() {
      var role = role();
      var roles = PageResult.of(1L, List.of(role));
      var cqlQuery = "cql.allRecords = 1";

      when(entityService.findByQuery(cqlQuery, 0, 1)).thenReturn(roles);

      var result = facade.search(cqlQuery, 0, 1);

      assertEquals(result.getTotalRecords(), result.getRoles().size());
      verify(entityService).findByQuery(cqlQuery, 0, 1);
    }
  }

  @Nested
  @DisplayName("deleteById")
  class DeleteById {

    @Test
    void positive() {
      when(entityService.getById(ROLE_ID)).thenReturn(role());

      facade.deleteById(ROLE_ID);

      verify(userRoleEntityService).findByRoleId(ROLE_ID);
      verify(userRoleEntityService).deleteByRoleId(ROLE_ID);
      verify(policyEntityService).findByName("Policy for role: " + ROLE_ID);
      verify(entityService).deleteById(ROLE_ID);
      verify(keycloakService).deleteById(ROLE_ID);
    }

    @Test
    void positive_assignedUsersAndRolePolicy_existInKeycloakAndDbAreCleanedUp() {
      var role = role();
      var userRole = new UserRole().userId(USER_ID).roleId(ROLE_ID);
      var policyName = "Policy for role: " + ROLE_ID;
      var policy = rolePolicy(policyName);
      var endpoint = endpoint("/foo/entities", GET);
      var endpoints = List.of(endpoint);

      when(entityService.getById(ROLE_ID)).thenReturn(role);
      when(userRoleEntityService.findByRoleId(ROLE_ID)).thenReturn(List.of(userRole));
      when(policyEntityService.findByName(policyName)).thenReturn(Optional.of(policy));
      when(capabilityEndpointService.getRoleAssignedEndpoints(ROLE_ID, emptyList(), emptyList())).thenReturn(endpoints);

      facade.deleteById(ROLE_ID);

      var inOrder = inOrder(policyEntityService, capabilityEndpointService, userRoleEntityService,
        keycloakRolesUserService, keycloakAuthService, policyService, entityService, keycloakService);
      inOrder.verify(policyEntityService).findByName(policyName);
      inOrder.verify(capabilityEndpointService).getRoleAssignedEndpoints(ROLE_ID, emptyList(), emptyList());
      inOrder.verify(userRoleEntityService).findByRoleId(ROLE_ID);
      inOrder.verify(keycloakRolesUserService).unlinkRolesFromUser(USER_ID, List.of(role));
      inOrder.verify(userRoleEntityService).deleteByRoleId(ROLE_ID);
      inOrder.verify(keycloakAuthService).deletePermissions(eq(policy), eq(endpoints), nameGeneratorCaptor.capture());
      inOrder.verify(policyService).deleteById(policy.getId());
      inOrder.verify(entityService).deleteById(ROLE_ID);
      inOrder.verify(keycloakService).deleteById(ROLE_ID);

      var policyNameGenerator = nameGeneratorCaptor.getValue();
      assertThat(policyNameGenerator.apply(endpoint)).isEqualTo("GET access for role '%s' to '/foo/entities'", ROLE_ID);
    }

    @Test
    void negative_repositoryThrowsException() {
      var role = role();
      var userRole = new UserRole().userId(USER_ID).roleId(ROLE_ID);
      var policyName = "Policy for role: " + ROLE_ID;
      var policy = rolePolicy(policyName);
      var endpoints = List.of(endpoint("/foo/entities", GET));

      when(entityService.getById(ROLE_ID)).thenReturn(role);
      when(userRoleEntityService.findByRoleId(ROLE_ID)).thenReturn(List.of(userRole));
      when(policyEntityService.findByName(policyName)).thenReturn(Optional.of(policy));
      when(capabilityEndpointService.getRoleAssignedEndpoints(ROLE_ID, emptyList(), emptyList())).thenReturn(endpoints);
      doThrow(RuntimeException.class).when(keycloakService).deleteById(ROLE_ID);

      assertThrows(RuntimeException.class, () -> facade.deleteById(ROLE_ID));
      verify(userRoleEntityService).findByRoleId(ROLE_ID);
      verify(userRoleEntityService).deleteByRoleId(ROLE_ID);
      verify(policyEntityService).findByName(policyName);
      verify(capabilityEndpointService).getRoleAssignedEndpoints(ROLE_ID, emptyList(), emptyList());
      verify(keycloakRolesUserService).unlinkRolesFromUser(USER_ID, List.of(role));
      verify(keycloakAuthService).deletePermissions(eq(policy), eq(endpoints), any());
      verify(policyService).deleteById(policy.getId());
      verify(entityService).deleteById(ROLE_ID);
      verify(entityService, never()).create(role);
      verify(keycloakPolicyService).create(policy);
      verify(keycloakAuthService).createPermissions(eq(policy), eq(endpoints), any());
      verify(keycloakRolesUserService).assignRolesToUser(USER_ID, List.of(role));
    }

    @Test
    void negative_userUnlinkFails_roleDeletionIsNotAttempted() {
      var role = role();
      var userRole = new UserRole().userId(USER_ID).roleId(ROLE_ID);

      when(entityService.getById(ROLE_ID)).thenReturn(role);
      when(userRoleEntityService.findByRoleId(ROLE_ID)).thenReturn(List.of(userRole));
      doThrow(RuntimeException.class).when(keycloakRolesUserService).unlinkRolesFromUser(USER_ID, List.of(role));

      assertThrows(RuntimeException.class, () -> facade.deleteById(ROLE_ID));

      verify(policyEntityService).findByName("Policy for role: " + ROLE_ID);
      verify(userRoleEntityService).findByRoleId(ROLE_ID);
      verify(keycloakRolesUserService).unlinkRolesFromUser(USER_ID, List.of(role));
      verify(keycloakRolesUserService, never()).assignRolesToUser(any(), any());
      verifyNoInteractions(policyService, keycloakService);
    }

    @Test
    void negative_permissionDeletionFails_previousUserUnlinkIsRestoredAndRolePolicyIsNotRecreated() {
      var role = role();
      var userRole = new UserRole().userId(USER_ID).roleId(ROLE_ID);
      var policyName = "Policy for role: " + ROLE_ID;
      var policy = rolePolicy(policyName);
      var endpoints = List.of(endpoint("/foo/entities", GET));

      when(entityService.getById(ROLE_ID)).thenReturn(role);
      when(userRoleEntityService.findByRoleId(ROLE_ID)).thenReturn(List.of(userRole));
      when(policyEntityService.findByName(policyName)).thenReturn(Optional.of(policy));
      when(capabilityEndpointService.getRoleAssignedEndpoints(ROLE_ID, emptyList(), emptyList())).thenReturn(endpoints);
      doThrow(RuntimeException.class).when(keycloakAuthService).deletePermissions(eq(policy), eq(endpoints), any());

      assertThrows(RuntimeException.class, () -> facade.deleteById(ROLE_ID));

      verify(policyEntityService).findByName(policyName);
      verify(capabilityEndpointService).getRoleAssignedEndpoints(ROLE_ID, emptyList(), emptyList());
      verify(userRoleEntityService).findByRoleId(ROLE_ID);
      verify(keycloakRolesUserService).unlinkRolesFromUser(USER_ID, List.of(role));
      verify(userRoleEntityService).deleteByRoleId(ROLE_ID);
      verify(keycloakAuthService).deletePermissions(eq(policy), eq(endpoints), any());
      verify(keycloakAuthService).createPermissions(eq(policy), eq(endpoints), any());
      verify(keycloakRolesUserService).assignRolesToUser(USER_ID, List.of(role));
      verifyNoInteractions(policyService, keycloakService);
    }

    @Test
    void negative_policyDeletionFails_previousUserUnlinkAndPermissionsAreRestoredAndRolePolicyIsNotRecreated() {
      var role = role();
      var userRole = new UserRole().userId(USER_ID).roleId(ROLE_ID);
      var policyName = "Policy for role: " + ROLE_ID;
      var policy = rolePolicy(policyName);
      var endpoints = List.of(endpoint("/foo/entities", GET));

      when(entityService.getById(ROLE_ID)).thenReturn(role);
      when(userRoleEntityService.findByRoleId(ROLE_ID)).thenReturn(List.of(userRole));
      when(policyEntityService.findByName(policyName)).thenReturn(Optional.of(policy));
      when(capabilityEndpointService.getRoleAssignedEndpoints(ROLE_ID, emptyList(), emptyList())).thenReturn(endpoints);
      doThrow(RuntimeException.class).when(policyService).deleteById(policy.getId());

      assertThrows(RuntimeException.class, () -> facade.deleteById(ROLE_ID));

      verify(policyEntityService).findByName(policyName);
      verify(capabilityEndpointService).getRoleAssignedEndpoints(ROLE_ID, emptyList(), emptyList());
      verify(userRoleEntityService).findByRoleId(ROLE_ID);
      verify(keycloakRolesUserService).unlinkRolesFromUser(USER_ID, List.of(role));
      verify(userRoleEntityService).deleteByRoleId(ROLE_ID);
      verify(keycloakAuthService).deletePermissions(eq(policy), eq(endpoints), any());
      verify(policyService).deleteById(policy.getId());
      verify(keycloakAuthService).createPermissions(eq(policy), eq(endpoints), any());
      verify(keycloakRolesUserService).assignRolesToUser(USER_ID, List.of(role));
      verifyNoInteractions(keycloakService);
    }

    @Test
    void negative_defaultRoleCannotBeDeleted() {
      var role = defaultRole();

      when(entityService.getById(ROLE_ID_3)).thenReturn(role);

      assertThatThrownBy(() -> facade.deleteById(ROLE_ID_3))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Default role cannot be created, updated or deleted via roles API.");

      verifyNoInteractions(keycloakService);
    }
  }

  @Nested
  @DisplayName("findByIds")
  class FindByIds {

    @Test
    void positive() {
      var roleIds = List.of(ROLE_ID);
      when(entityService.findByIds(roleIds)).thenReturn(List.of(role()));
      var result = facade.findByIds(roleIds);
      assertThat(result).containsExactly(role());
    }

    @Test
    void positive_multipleRoles() {
      var roleIds = List.of(ROLE_ID, ROLE_ID_2);
      var role1 = role();
      var role2 = consortiumRole();
      when(entityService.findByIds(roleIds)).thenReturn(List.of(role1, role2));
      var result = facade.findByIds(roleIds);
      assertThat(result).containsExactly(role1, role2);
    }

    @Test
    void negative_notAllRolesFound() {
      var roleIds = List.of(ROLE_ID);
      when(entityService.findByIds(roleIds)).thenReturn(emptyList());
      assertThatThrownBy(() -> facade.findByIds(roleIds))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageMatching("Roles are not found for ids: \\[.*]");
    }

    @Test
    void negative_someRolesNotFound() {
      var roleIds = List.of(ROLE_ID, ROLE_ID_2, ROLE_ID_3);
      when(entityService.findByIds(roleIds)).thenReturn(List.of(role()));
      assertThatThrownBy(() -> facade.findByIds(roleIds))
        .isInstanceOf(EntityNotFoundException.class)
        .hasMessageMatching("Roles are not found for ids: \\[.*]");
    }
  }
}
