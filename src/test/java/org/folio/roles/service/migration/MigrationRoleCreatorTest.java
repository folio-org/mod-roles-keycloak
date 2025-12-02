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
      when(roleMigrationService.createRolesSafely(List.of(migrationRole()))).thenReturn(createdMigrationRoles());
      var result = migrationRoleCreator.createRoles(List.of(userPermissions()), JOB_ID);
      assertThat(result).isEqualTo(List.of(createdMigrationRole()));
    }

    @Test
    void positive_roleFound() {
      when(roleMigrationService.createRolesSafely(List.of(migrationRole()))).thenReturn(new Roles().totalRecords(0L));
      when(roleService.search("name==" + ROLE_NAME, 0, 1)).thenReturn(createdMigrationRoles());

      var result = migrationRoleCreator.createRoles(List.of(userPermissions()), JOB_ID);

      assertThat(result).isEqualTo(List.of(createdMigrationRole()));
    }
    
    @Test
    void positive_partialFailure_someRolesCreated(CapturedOutput output) {
      var userPerm1 = userPermissions();
      var userPerm2 = new UserPermissions().userId(USER_ID).roleName("role2").permissions(List.of("bar.item.get"));
      
      var role1 = migrationRole();
      var role2 = new Role()
        .name("role2")
        .type(RoleType.REGULAR)
        .description("System generated role during migration");
      
      var createdRole1 = createdMigrationRole();
      
      // Only role1 is created successfully, role2 fails
      when(roleMigrationService.createRolesSafely(List.of(role1, role2)))
        .thenReturn(new Roles().addRolesItem(createdRole1).totalRecords(1L));
      
      // role2 search returns empty (not found)
      when(roleService.search("name==role2", 0, 1)).thenReturn(new Roles().totalRecords(0L));
      
      var result = migrationRoleCreator.createRoles(List.of(userPerm1, userPerm2), JOB_ID);
      
      // Only successfully created roles are returned
      assertThat(result).hasSize(1);
      assertThat(result.get(0)).isEqualTo(createdRole1);
      assertThat(output.getAll()).contains("Some roles failed to create or already existed");
      assertThat(output.getAll()).contains("Searching for 1 missing role(s)");
      
      // Verify error was logged for the missing role
      verify(migrationErrorService).logError(JOB_ID, "ROLE_CREATION_FAILED", 
        "Role not found after creation attempt", "ROLE", "role2");
    }
    
    @Test
    void positive_allRolesFailed_returnsEmpty(CapturedOutput output) {
      when(roleMigrationService.createRolesSafely(List.of(migrationRole()))).thenReturn(new Roles().totalRecords(0L));
      when(roleService.search("name==" + ROLE_NAME, 0, 1)).thenReturn(new Roles().totalRecords(0L));
      
      var result = migrationRoleCreator.createRoles(List.of(userPermissions()), JOB_ID);
      
      assertThat(result).isEmpty();
      assertThat(output.getAll()).contains("Some roles failed to create or already existed");
      
      // Verify error was logged for the failed role
      verify(migrationErrorService).logError(JOB_ID, "ROLE_CREATION_FAILED",
        "Role not found after creation attempt", "ROLE", ROLE_NAME);
    }
    
    @Test
    void positive_searchThrowsException_skipsRole(CapturedOutput output) {
      when(roleMigrationService.createRolesSafely(List.of(migrationRole()))).thenReturn(new Roles().totalRecords(0L));
      when(roleService.search("name==" + ROLE_NAME, 0, 1))
        .thenThrow(new RuntimeException("Search failed"));
      
      var result = migrationRoleCreator.createRoles(List.of(userPermissions()), JOB_ID);
      
      assertThat(result).isEmpty();
      assertThat(output.getAll()).contains("Failed to search for role: name = " + ROLE_NAME);
      
      // Verify error was logged with the exception message
      verify(migrationErrorService).logError(JOB_ID, "ROLE_CREATION_FAILED",
        "Search failed: Search failed", "ROLE", ROLE_NAME);
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
