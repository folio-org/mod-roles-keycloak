package org.folio.roles.support;

import static org.folio.common.utils.CollectionUtils.mapItems;
import static org.folio.roles.support.AuditableUtils.populateAuditable;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermissionEntity;
import static org.folio.roles.support.LoadablePermissionUtils.loadablePermissions;
import static org.instancio.Select.field;
import static org.instancio.Select.root;

import java.util.HashSet;
import lombok.experimental.UtilityClass;
import org.folio.roles.domain.entity.LoadableRoleEntity;
import org.folio.roles.domain.entity.type.EntityLoadableRoleType;
import org.folio.roles.domain.model.LoadableRole;
import org.instancio.Instancio;
import org.instancio.Model;
import org.instancio.OnCompleteCallback;

@UtilityClass
public class LoadableRoleUtils {

  private static final Model<LoadableRole> LOADABLE_ROLE_MODEL = Instancio.of(LoadableRole.class)
    .supply(field(LoadableRole::getPermissions), () -> new HashSet<>(loadablePermissions(10)))
    .onComplete(root(), completeRoleInit())
    .toModel();

  public static LoadableRole loadableRole() {
    return Instancio.of(LOADABLE_ROLE_MODEL).create();
  }

  public static LoadableRoleEntity loadableRoleEntity(LoadableRole role) {
    var entity = new LoadableRoleEntity();

    entity.setId(role.getId());
    entity.setName(role.getName());
    entity.setDescription(role.getDescription());
    entity.setType(EntityLoadableRoleType.from(role.getType()));

    entity.setPermissions(mapItems(role.getPermissions(), perm -> loadablePermissionEntity(role.getId(), perm)));

    populateAuditable(entity, role.getMetadata());

    return entity;
  }

  private static OnCompleteCallback<LoadableRole> completeRoleInit() {
    return (LoadableRole role) -> {
      var roleId = role.getId();

      role.setName("Role " + roleId);
      role.setDescription("Description of role " + roleId);

      role.getPermissions().forEach(loadablePermission -> loadablePermission.setRoleId(roleId));
    };
  }
}
