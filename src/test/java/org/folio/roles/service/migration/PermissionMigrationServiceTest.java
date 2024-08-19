package org.folio.roles.service.migration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.folio.roles.support.TestConstants.USER_ID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.folio.roles.domain.dto.Role;
import org.folio.roles.domain.model.UserPermissions;
import org.folio.roles.exception.MigrationException;
import org.folio.roles.support.TestUtils;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@UnitTest
@ExtendWith(MockitoExtension.class)
class PermissionMigrationServiceTest {

  private static final UUID MIGRATION_ID = UUID.randomUUID();

  @InjectMocks private PermissionMigrationService permissionMigrationService;
  @Mock private MigrationRoleCreator migrationRoleCreator;
  @Mock private UserPermissionsLoader userPermissionsLoader;
  @Mock private RolePermissionAssignor rolePermissionAssignor;

  @AfterEach
  void tearDown() {
    TestUtils.verifyNoMoreInteractions(this);
  }

  @Test
  void migratePermissions_positive() {
    var userPermissions = new UserPermissions()
      .roleName("test")
      .userId(USER_ID)
      .permissions(List.of("foo.item.get", "foo.item.post"));

    var userPermissionsList = List.of(userPermissions);
    var roles = List.of(new Role().id(UUID.randomUUID()).name("test"));
    when(userPermissionsLoader.loadUserPermissions()).thenReturn(userPermissionsList);
    when(migrationRoleCreator.createRoles(userPermissionsList)).thenReturn(roles);

    permissionMigrationService.migratePermissions(MIGRATION_ID);

    verify(migrationRoleCreator).assignUsers(userPermissionsList);
    verify(rolePermissionAssignor).assignPermissions(userPermissionsList);
  }

  @Test
  void migratePermissions_negative_roleIsNotCreated() {
    var permissions = List.of("foo.item.get", "foo.item.post");
    var userPermissions = new UserPermissions().roleName("test").userId(USER_ID).permissions(permissions);
    var userPermissionsList = List.of(userPermissions);
    when(userPermissionsLoader.loadUserPermissions()).thenReturn(userPermissionsList);
    when(migrationRoleCreator.createRoles(userPermissionsList)).thenReturn(Collections.emptyList());

    assertThatThrownBy(() -> permissionMigrationService.migratePermissions(MIGRATION_ID))
      .isInstanceOf(MigrationException.class)
      .hasMessage("Roles are not created for user permissions: [UserPermissions("
        + "userId=%s, role=null, roleName=test, permissions=%s)]", USER_ID, permissions);
  }
}
