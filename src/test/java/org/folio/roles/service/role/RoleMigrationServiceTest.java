package org.folio.roles.service.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.folio.roles.support.RoleUtils.role;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import java.util.List;
import java.util.Optional;
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
class RoleMigrationServiceTest {

  @Mock private KeycloakRoleService keycloakRoleService;
  @Mock private RoleEntityService roleEntityService;

  @InjectMocks private RoleMigrationService roleMigrationService;

  @Nested
  @DisplayName("createRolesSafely")
  class CreateRolesSafely {

    @Test
    void positive_rolesCreatedSuccessfully() {
      var role = role();
      when(keycloakRoleService.create(role)).thenReturn(role);
      when(roleEntityService.create(role)).thenReturn(role);

      var result = roleMigrationService.createRolesSafely(List.of(role));

      assertThat(result.getSuccessfulRoles()).hasSize(1);
      assertThat(result.getFailures()).isEmpty();
      verify(keycloakRoleService).create(role);
      verify(roleEntityService).create(role);
    }

    @Test
    void positive_roleExistsInKeycloak() {
      var role = role();
      when(keycloakRoleService.create(role)).thenThrow(
        new KeycloakApiException("Role already exists", new WebApplicationException()));
      when(keycloakRoleService.findByName(role.getName())).thenReturn(Optional.of(role));
      when(roleEntityService.findByName(role.getName())).thenReturn(Optional.empty());
      when(roleEntityService.create(role)).thenReturn(role);

      var result = roleMigrationService.createRolesSafely(List.of(role));

      assertThat(result.getSuccessfulRoles()).hasSize(1);
      assertThat(result.getFailures()).hasSize(1);
      assertThat(result.getFailures().get(0).getRoleName()).isEqualTo(role.getName());
      assertThat(result.getFailures().get(0).getErrorMessage()).contains("Failed to create role in Keycloak");
      verify(keycloakRoleService).create(role);
      verify(keycloakRoleService).findByName(role.getName());
      verify(roleEntityService, times(2)).findByName(role.getName());
      verify(roleEntityService).create(role);
    }

    @Test
    void positive_roleAlreadyExistsInDb() {
      var role = role();
      when(keycloakRoleService.create(role)).thenThrow(
        new KeycloakApiException("Role already exists", new WebApplicationException()));
      when(roleEntityService.findByName(role.getName())).thenReturn(Optional.of(role));

      var result = roleMigrationService.createRolesSafely(List.of(role));

      assertThat(result.getSuccessfulRoles()).isEmpty();
      assertThat(result.getFailures()).hasSize(1);
      verify(keycloakRoleService).create(role);
      verify(roleEntityService).findByName(role.getName());
      verifyNoMoreInteractions(keycloakRoleService);
    }

    @Test
    void positive_dbCreationFailsAfterKeycloakSuccess_rollbackHappens() {
      var role = role();
      when(keycloakRoleService.create(role)).thenReturn(role);
      when(roleEntityService.findByName(role.getName())).thenReturn(Optional.empty());
      when(roleEntityService.create(role)).thenThrow(RuntimeException.class);

      var result = roleMigrationService.createRolesSafely(List.of(role));

      assertThat(result.getSuccessfulRoles()).isEmpty();
      assertThat(result.getFailures()).hasSize(1);
      assertThat(result.getFailures().get(0).getErrorMessage()).contains("Failed to create role in database");
      verify(keycloakRoleService).create(role);
      verify(roleEntityService).findByName(role.getName());
      verify(roleEntityService).create(role);
      // Verify rollback: Keycloak role should be deleted after DB creation fails
      verify(keycloakRoleService).deleteById(role.getId());
    }

    @Test
    void positive_keycloakRoleNotFound() {
      var role = role();
      when(keycloakRoleService.create(role)).thenThrow(
        new KeycloakApiException("Role already exists", new WebApplicationException()));
      when(keycloakRoleService.findByName(role.getName())).thenReturn(Optional.empty());
      when(roleEntityService.findByName(role.getName())).thenReturn(Optional.empty());

      var result = roleMigrationService.createRolesSafely(List.of(role));

      assertThat(result.getSuccessfulRoles()).isEmpty();
      assertThat(result.getFailures()).hasSize(1);
      verify(keycloakRoleService).create(role);
      verify(keycloakRoleService).findByName(role.getName());
      verify(roleEntityService).findByName(role.getName());
      verifyNoMoreInteractions(roleEntityService);
    }

    @Test
    void positive_roleExistsInKeycloakButDbCreateFails() {
      var role = role();
      when(keycloakRoleService.create(role)).thenThrow(
        new KeycloakApiException("Role already exists", new WebApplicationException()));
      when(keycloakRoleService.findByName(role.getName())).thenReturn(Optional.of(role));
      when(roleEntityService.findByName(role.getName())).thenReturn(Optional.empty());
      when(roleEntityService.create(role)).thenThrow(RuntimeException.class);

      var result = roleMigrationService.createRolesSafely(List.of(role));

      assertThat(result.getSuccessfulRoles()).isEmpty();
      assertThat(result.getFailures()).hasSize(2);
      verify(keycloakRoleService).create(role);
      verify(keycloakRoleService).findByName(role.getName());
      verify(roleEntityService, times(2)).findByName(role.getName());
      verify(roleEntityService).create(role);
      // Verify rollback: Keycloak role should be deleted after DB creation fails
      verify(keycloakRoleService).deleteById(role.getId());
    }

    @Test
    void positive_multipleRoles_partialSuccess() {
      var role1 = role();
      var role2 = role().name("role2");
      
      when(keycloakRoleService.create(role1)).thenReturn(role1);
      when(roleEntityService.create(role1)).thenReturn(role1);
      
      when(keycloakRoleService.create(role2)).thenThrow(
        new KeycloakApiException("Failed", new WebApplicationException()));

      var result = roleMigrationService.createRolesSafely(List.of(role1, role2));

      assertThat(result.getSuccessfulRoles()).hasSize(1);
      assertThat(result.getSuccessfulRoles().get(0).getName()).isEqualTo(role1.getName());
      assertThat(result.getFailures()).hasSize(1);
      assertThat(result.getFailures().get(0).getRoleName()).isEqualTo(role2.getName());
    }
  }
}
