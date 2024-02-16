package org.folio.roles.support;

import static org.folio.roles.support.AuditableUtils.populateAuditable;

import java.util.UUID;
import lombok.experimental.UtilityClass;
import org.folio.roles.domain.entity.LoadablePermissionEntity;
import org.folio.roles.domain.model.LoadablePermission;

@UtilityClass
public class LoadablePermissionUtils {

  public static LoadablePermissionEntity loadablePermissionEntity(UUID roleId, LoadablePermission perm) {
    var entity = new LoadablePermissionEntity();

    entity.setRoleId(roleId);
    entity.setPermissionName(perm.getPermissionName());
    entity.setCapabilityId(perm.getCapabilityId());
    entity.setCapabilitySetId(perm.getCapabilitySetId());
    populateAuditable(entity, perm.getMetadata());

    return entity;
  }
}
