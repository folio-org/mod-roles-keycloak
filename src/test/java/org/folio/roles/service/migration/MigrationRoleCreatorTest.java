package org.folio.roles.service.migration;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.support.RoleUtils.ROLE_ID;
import static org.folio.roles.support.RoleUtils.ROLE_NAME;
import static org.folio.roles.support.RoleUtils.role;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.dto.RoleType;
import org.folio.roles.domain.dto.Roles;
import org.folio.roles.domain.dto.UserRole;
import org.folio.roles.domain.model.UserPermissions;
import org.folio.roles.exception.MigrationException;
import org.folio.roles.service.role.RoleService;
import org.folio.roles.service.role.UserRoleService;
import org.folio.roles.support.TestUtils;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@UnitTest
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class MigrationRoleCreatorTest {

  private static final UUID JOB_ID = UUID.randomUUID();

  @InjectMocks private MigrationRoleCreator migrationRoleCreator;
  @Mock private RoleService roleService;
  @Mock private org.folio.roles.service.role.RoleMigrationService roleMigrationService;
  @Mock private UserRoleService userRoleService;
  @Mock private org.folio.roles.service.MigrationErrorService migrationErrorService;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  private static Role migrationRole() {
    return new Role()
      .name(ROLE_NAME)
      .type(RoleType.REGULAR)
      .description("System generated role during migration");
  }

  private static Roles createdMigrationRoles() {
    return new Roles().addRolesItem(createdMigrationRole()).totalRecords(1L);
  }

  private static org.folio.roles.service.role.RoleCreationResult successfulRoleCreation() {
    var result = new org.folio.roles.service.role.RoleCreationResult();
    result.addSuccess(createdMigrationRole());
    return result;
  }

  private static org.folio.roles.service.role.RoleCreationResult emptyRoleCreation() {
    return new org.folio.roles.service.role.RoleCreationResult();
  }

  private static Role createdMigrationRole() {
    return new Role()
      .id(ROLE_ID)
      .name(ROLE_NAME)
      .type(RoleType.REGULAR)
      .description("System generated role during migration");
  }

  private static UserPermissions userPermissions() {
    return new UserPermissions().userId(USER_ID).roleName(ROLE_NAME).permissions(List.of("foo.item.get"));
  }

  @Nested
  @DisplayName("createRoles")
  class CreateRoles {

    @Test
    void positive_roleCreated() {
      when(roleMigrationService.createRolesSafely(List.of(migrationRole()))).thenReturn(successfulRoleCreation());
      var result = migrationRoleCreator.createRoles(List.of(userPermissions()), JOB_ID);
      assertThat(result).isEqualTo(List.of(createdMigrationRole()));
    }

    @Test
    void positive_roleFound() {
      when(roleMigrationService.createRolesSafely(List.of(migrationRole()))).thenReturn(emptyRoleCreation());
      when(roleService.search("name==" + ROLE_NAME, 0, 1)).thenReturn(createdMigrationRoles());

      var result = migrationRoleCreator.createRoles(List.of(userPermissions()), JOB_ID);

      assertThat(result).isEqualTo(List.of(createdMigrationRole()));
    }
    
    @Test
    void positive_partialFailure_someRolesCreated(CapturedOutput output) {
      var role1 = migrationRole();
      var role2 = new Role()
        .name("role2")
        .type(RoleType.REGULAR)
        .description("System generated role during migration");
      
      var createdRole1 = createdMigrationRole();
      
      var partialResult = new org.folio.roles.service.role.RoleCreationResult();
      partialResult.addSuccess(createdRole1);
      partialResult.addFailure("role2", "Failed to create in Keycloak", new RuntimeException("Test error"));
      
      when(roleMigrationService.createRolesSafely(List.of(role1, role2))).thenReturn(partialResult);
      when(roleService.search("name==role2", 0, 1)).thenReturn(new Roles().totalRecords(0L));
      
      var userPerm1 = userPermissions();
      var userPerm2 = new UserPermissions().userId(USER_ID).roleName("role2").permissions(List.of("bar.item.get"));
      var result = migrationRoleCreator.createRoles(List.of(userPerm1, userPerm2), JOB_ID);
      
      // Only successfully created roles are returned
      assertThat(result).hasSize(1);
      assertThat(result.get(0)).isEqualTo(createdRole1);
      assertThat(output.getAll()).contains("Some roles failed to create or already existed");
      assertThat(output.getAll()).contains("Recording 1 role creation failure(s)");
      
      // Verify error was logged
      verify(migrationErrorService).logError(JOB_ID, "ROLE_CREATION_FAILED",
        "Failed to create in Keycloak [Type: RuntimeException, Root cause: RuntimeException: Test error]",
        "ROLE", "role2");
    }
    
    @Test
    void positive_allRolesFailed_returnsEmpty(CapturedOutput output) {
      var failureResult = new org.folio.roles.service.role.RoleCreationResult();
      failureResult.addFailure(ROLE_NAME, "Failed to create", new RuntimeException("Test error"));
      
      when(roleMigrationService.createRolesSafely(List.of(migrationRole()))).thenReturn(failureResult);
      when(roleService.search("name==" + ROLE_NAME, 0, 1)).thenReturn(new Roles().totalRecords(0L));
      
      var result = migrationRoleCreator.createRoles(List.of(userPermissions()), JOB_ID);
      
      assertThat(result).isEmpty();
      assertThat(output.getAll()).contains("Recording 1 role creation failure(s)");
      
      // Verify error was logged
      verify(migrationErrorService).logError(JOB_ID, "ROLE_CREATION_FAILED",
        "Failed to create [Type: RuntimeException, Root cause: RuntimeException: Test error]", "ROLE", ROLE_NAME);
    }
    
    @Test
    void positive_searchThrowsException_skipsRole(CapturedOutput output) {
      var failureResult = new org.folio.roles.service.role.RoleCreationResult();
      failureResult.addFailure(ROLE_NAME, "Failed to create", new RuntimeException("Creation failed"));
      
      when(roleMigrationService.createRolesSafely(List.of(migrationRole()))).thenReturn(failureResult);
      when(roleService.search("name==" + ROLE_NAME, 0, 1))
        .thenThrow(new RuntimeException("Search failed"));
      
      var result = migrationRoleCreator.createRoles(List.of(userPermissions()), JOB_ID);
      
      assertThat(result).isEmpty();
      assertThat(output.getAll()).contains("Recording 1 role creation failure(s)");
      assertThat(output.getAll()).contains("Failed to search for role: name = " + ROLE_NAME);
      
      // Verify error was logged from creation result
      verify(migrationErrorService).logError(JOB_ID, "ROLE_CREATION_FAILED",
        "Failed to create [Type: RuntimeException, Root cause: RuntimeException: Creation failed]", "ROLE", ROLE_NAME);
    }
  }

  @Nested
  @DisplayName("assignUsers")
  class AssignUsers {

    @Test
    void positive(CapturedOutput output) {
      var userPermissionsList = List.of(userPermissions().role(role()));

      migrationRoleCreator.assignUsers(userPermissionsList);

      verify(userRoleService).createSafe(new UserRole().userId(USER_ID).roleId(ROLE_ID));
      assertThat(output.getAll()).contains("User-role relations creation process finished: totalRecords = 1");
    }

    @Test
    void negative_userRoleNotCreated(CapturedOutput output) {
      var userPermissionsList = List.of(userPermissions().role(role()));
      var exception = new RuntimeException("error");
      doThrow(exception).when(userRoleService).createSafe(new UserRole().userId(USER_ID).roleId(ROLE_ID));

      assertThatThrownBy(() -> migrationRoleCreator.assignUsers(userPermissionsList))
        .isInstanceOf(MigrationException.class)
        .hasMessage("Failed to assign users to roles: [class UserRole {\n"
          + "    userId: " + USER_ID + "\n"
          + "    roleId: " + ROLE_ID + "\n"
          + "    metadata: null\n"
          + "}]");

      assertThat(output.getAll()).contains(format("Failed to assign user %s to role %s", USER_ID, ROLE_ID));
    }
  }
}
