package org.folio.roles.service.role;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.domain.dto.RoleType.CONSORTIUM;
import static org.folio.roles.domain.dto.RoleType.DEFAULT;
import static org.folio.roles.domain.dto.RoleType.REGULAR;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Optional;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.model.PageResult;
import org.folio.roles.exception.ServiceException;
import org.folio.roles.integration.keyclock.KeycloakRoleService;
import org.folio.roles.integration.keyclock.exception.KeycloakApiException;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

  @Mock private KeycloakRoleService keycloakService;
  @Mock private RoleEntityService entityService;

  @InjectMocks private RoleService facade;

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
    void create() {
      var role = role();
      when(keycloakService.create(role)).thenReturn(role);
      when(entityService.create(role)).thenReturn(role);

      facade.create(role);

      verify(keycloakService).create(role);
      verify(entityService).create(role);
    }

    @Test
    void negative_create() {
      var role = role();
      when(keycloakService.create(role)).thenReturn(role);
      when(entityService.create(role)).thenThrow(RuntimeException.class);

      assertThrows(ServiceException.class, () -> facade.create(role));
      verify(keycloakService).deleteById(role.getId());
    }

    @Test
    void positive() {
      var role = role();
      var roles = List.of(role);

      when(keycloakService.create(role)).thenReturn(role);
      when(entityService.create(role)).thenReturn(role).thenThrow(RuntimeException.class);

      var result = facade.create(roles);
      facade.create(roles);

      assertEquals(result.getRoles().size(), result.getTotalRecords());
      assertEquals(result.getRoles().getFirst(), role);
      verify(keycloakService, times(2)).create(role);
      verify(entityService, times(2)).create(role);
    }

    @Test
    void positive_roleExistsInKeycloak() {
      var role = role();
      var roles = List.of(role);

      when(keycloakService.create(role)).thenThrow(new KeycloakApiException("Role already exists",
        new WebApplicationException()));
      when(keycloakService.findByName(role.getName())).thenReturn(Optional.of(role));
      when(entityService.create(role)).thenReturn(role);

      var result = facade.create(roles);
      facade.create(roles);

      assertTrue(result.getRoles().isEmpty());
      verify(keycloakService, times(2)).create(role);
      verify(entityService, times(2)).create(role);
    }

    @Test
    void create_positive_returnsEmpty() {
      var role = role();

      when(keycloakService.create(role)).thenThrow(new KeycloakApiException("Role already exists",
        new WebApplicationException()));

      var result = facade.create(List.of(role));

      assertTrue(result.getRoles().isEmpty());
    }

    @Test
    void createBatch_positive_roleAlreadyExistsInDb() {
      var role = role();
      var roles = List.of(role);

      when(keycloakService.create(role)).thenThrow(new KeycloakApiException("Role already exists",
        new WebApplicationException()));
      when(entityService.findByName(role.getName())).thenReturn(Optional.of(role));

      var result = facade.create(roles);

      assertTrue(result.getRoles().isEmpty());
      verify(keycloakService).create(role);
      verify(entityService).findByName(role.getName());
    }

    @Test
    void createBatch_positive_roleExistsInKeycloakButNotInDb() {
      var role = role();
      var roles = List.of(role);

      when(keycloakService.create(role)).thenThrow(new KeycloakApiException("Role already exists",
        new WebApplicationException()));
      when(keycloakService.findByName(role.getName())).thenReturn(Optional.of(role));
      when(entityService.findByName(role.getName())).thenReturn(Optional.empty()).thenReturn(Optional.empty());
      when(entityService.create(role)).thenReturn(role);

      var result = facade.create(roles);

      assertTrue(result.getRoles().isEmpty());
      verify(keycloakService).create(role);
      verify(keycloakService).findByName(role.getName());
      verify(entityService, times(2)).findByName(role.getName());
      verify(entityService).create(role);
    }

    @Test
    void createBatch_positive_roleExistsInKeycloakButDbCreateFails() {
      var role = role();
      var roles = List.of(role);

      when(keycloakService.create(role)).thenThrow(new KeycloakApiException("Role already exists",
        new WebApplicationException()));
      when(keycloakService.findByName(role.getName())).thenReturn(Optional.of(role));
      when(entityService.findByName(role.getName())).thenReturn(Optional.empty()).thenReturn(Optional.empty());
      when(entityService.create(role)).thenThrow(RuntimeException.class);

      var result = facade.create(roles);

      assertTrue(result.getRoles().isEmpty());
      verify(keycloakService).create(role);
      verify(keycloakService).findByName(role.getName());
      verify(entityService, times(2)).findByName(role.getName());
      verify(entityService).create(role);
      // Verify rollback: Keycloak role should be deleted after DB creation fails
      verify(keycloakService).deleteById(role.getId());
    }
    
    @Test
    void createBatch_positive_dbCreationFailsAfterKeycloakSuccess_rollbackHappens() {
      var role = role();
      var roles = List.of(role);

      when(keycloakService.create(role)).thenReturn(role);
      when(entityService.findByName(role.getName())).thenReturn(Optional.empty());
      when(entityService.create(role)).thenThrow(RuntimeException.class);

      var result = facade.create(roles);

      assertTrue(result.getRoles().isEmpty());
      verify(keycloakService).create(role);
      verify(entityService).findByName(role.getName());
      verify(entityService).create(role);
      // Verify rollback: Keycloak role should be deleted after DB creation fails
      verify(keycloakService).deleteById(role.getId());
    }

    @Test
    void createBatch_positive_keycloakRoleNotFound() {
      var role = role();
      var roles = List.of(role);

      when(keycloakService.create(role)).thenThrow(new KeycloakApiException("Role already exists",
        new WebApplicationException()));
      when(keycloakService.findByName(role.getName())).thenReturn(Optional.empty());
      when(entityService.findByName(role.getName())).thenReturn(Optional.empty());

      var result = facade.create(roles);

      assertTrue(result.getRoles().isEmpty());
      verify(keycloakService).create(role);
      verify(keycloakService).findByName(role.getName());
      verify(entityService).findByName(role.getName());
    }

    @Test
    void createBatch_positive_mixedRoles() {
      var role1 = role();
      var role2 = defaultRole();
      var role3 = consortiumRole();
      var roles = List.of(role1, role2, role3);

      when(keycloakService.create(role1)).thenReturn(role1);
      when(entityService.create(role1)).thenReturn(role1);
      when(keycloakService.create(role3)).thenReturn(role3);
      when(entityService.create(role3)).thenReturn(role3);

      var result = facade.create(roles);

      assertEquals(2, result.getTotalRecords());
      assertEquals(2, result.getRoles().size());
      verify(keycloakService, times(2)).create(any());
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

      verify(entityService).deleteById(ROLE_ID);
      verify(keycloakService).deleteById(ROLE_ID);
    }

    @Test
    void negative_repositoryThrowsException() {
      var role = role();

      when(entityService.getById(ROLE_ID)).thenReturn(role);
      doThrow(RuntimeException.class).when(keycloakService).deleteById(ROLE_ID);

      assertThrows(RuntimeException.class, () -> facade.deleteById(ROLE_ID));
      verify(entityService).deleteById(ROLE_ID);
      verify(entityService).create(role);
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
