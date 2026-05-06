package org.folio.roles.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.folio.test.types.UnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@UnitTest
@ExtendWith(OutputCaptureExtension.class)
class LoadablePermissionEntityTest {

  @Test
  void logCreated_positive_emitsInsertLog(CapturedOutput output) {
    var entity = entity();

    entity.logCreated();

    assertThat(output).contains("Saved role_loadable_permission record: roleId=" + entity.getRoleId()
      + ", permissionName=" + entity.getPermissionName())
      .contains("role_loadable_permission save stack trace");
  }

  @Test
  void logUpdated_positive_emitsUpdateLog(CapturedOutput output) {
    var entity = entity();

    entity.logUpdated();

    assertThat(output).contains("Updated role_loadable_permission record: roleId=" + entity.getRoleId()
      + ", permissionName=" + entity.getPermissionName())
      .contains("role_loadable_permission update stack trace");
  }

  private static LoadablePermissionEntity entity() {
    var entity = new LoadablePermissionEntity();
    entity.setRoleId(UUID.randomUUID());
    entity.setPermissionName("users.collection.get");
    entity.setCapabilityId(UUID.randomUUID());
    entity.setCapabilitySetId(UUID.randomUUID());
    return entity;
  }
}
